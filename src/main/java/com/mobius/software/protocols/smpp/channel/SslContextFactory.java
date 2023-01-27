package com.mobius.software.protocols.smpp.channel;
/*
 * Mobius Software LTD
 * Copyright 2019 - 2023, Mobius Software LTD and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslContextFactory 
{
    private static final Logger logger = LoggerFactory.getLogger(SslContextFactory.class);
    
    private SSLContext sslContext;
    private InputStream keyStoreInputStream;
    private InputStream trustStoreInputStream;

    private final SslConfiguration sslConfig;

    public SslContextFactory() throws Exception 
    {
        this(new SslConfiguration());
    }

    public SslContextFactory(SslConfiguration sslConfig) throws Exception 
    {
		this.sslConfig = sslConfig;
		init();
    }

    private void init() throws Exception 
    {
        if (sslContext == null) 
        {
            if (keyStoreInputStream == null && sslConfig.getKeyStorePath() == null && trustStoreInputStream == null && sslConfig.getTrustStorePath() == null) 
            {
                TrustManager[] trust_managers = null;
                if (sslConfig.isTrustAll()) 
                {
                    logger.debug("No keystore or trust store configured.  ACCEPTING UNTRUSTED CERTIFICATES!!!!!");
                    TrustManager trustAllCerts = new X509TrustManager() 
                    {
                    	public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    		return null;
                    	}
			    
                    	public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    	}

                    	public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    	}
                    };
                    
                    trust_managers = new TrustManager[] { trustAllCerts };
                }
                
                SecureRandom secureRandom = (sslConfig.getSecureRandomAlgorithm() == null)?null:
                SecureRandom.getInstance(sslConfig.getSecureRandomAlgorithm());
                sslContext = SSLContext.getInstance(sslConfig.getProtocol());
                sslContext.init(null, trust_managers, secureRandom);
            } 
            else 
            {
                // verify that keystore and truststore
                // parameters are set up correctly               
                checkKeyStore();

                KeyStore keyStore = loadKeyStore();
                KeyStore trustStore = loadTrustStore();

                Collection<? extends CRL> crls = loadCRL(sslConfig.getCrlPath());

                if (sslConfig.isValidateCerts() && keyStore != null) 
                {
                    if (sslConfig.getCertAlias() == null) 
                    {
                        List<String> aliases = Collections.list(keyStore.aliases());
                        sslConfig.setCertAlias(aliases.size() == 1 ? aliases.get(0) : null);
                    }

                    Certificate cert = sslConfig.getCertAlias() == null?null:
                    keyStore.getCertificate(sslConfig.getCertAlias());
                    if (cert == null) 
                        throw new Exception("No certificate found in the keystore" + (sslConfig.getCertAlias() == null ? "":" for alias " + sslConfig.getCertAlias()));
                    
                    CertificateValidator validator = new CertificateValidator(trustStore, crls);
                    validator.setMaxCertPathLength(sslConfig.getMaxCertPathLength());
                    validator.setEnableCRLDP(sslConfig.isEnableCRLDP());
                    validator.setEnableOCSP(sslConfig.isEnableOCSP());
                    validator.setOcspResponderURL(sslConfig.getOcspResponderURL());
                    validator.validate(keyStore, cert);
                }

                KeyManager[] keyManagers = getKeyManagers(keyStore);
                TrustManager[] trustManagers = getTrustManagers(trustStore, crls);

                SecureRandom secureRandom = (sslConfig.getSecureRandomAlgorithm() == null)?null:
                SecureRandom.getInstance(sslConfig.getSecureRandomAlgorithm());
                sslContext = (sslConfig.getProvider() == null)?
                SSLContext.getInstance(sslConfig.getProtocol()):
                SSLContext.getInstance(sslConfig.getProtocol(), sslConfig.getProvider());
                sslContext.init(keyManagers, trustManagers, secureRandom);

                SSLEngine engine = newSslEngine();
                
                logger.info("Enabled Protocols {} of {}",
			    Arrays.asList(engine.getEnabledProtocols()),
			    Arrays.asList(engine.getSupportedProtocols()));
                logger.debug("Enabled Ciphers {} of {}",
			    Arrays.asList(engine.getEnabledCipherSuites()),
			    Arrays.asList(engine.getSupportedCipherSuites()));
            }
        }
    }

    public SSLContext getSslContext() 
    {
    	return sslContext;
    }
    
    protected KeyStore loadKeyStore() throws Exception 
    {
        return getKeyStore(keyStoreInputStream, sslConfig.getKeyStorePath(), sslConfig.getKeyStoreType(), sslConfig.getKeyStoreProvider(), sslConfig.getKeyStorePassword());
    }

    protected KeyStore loadTrustStore() throws Exception 
    {
        return getKeyStore(trustStoreInputStream, sslConfig.getTrustStorePath(), sslConfig.getTrustStoreType(), sslConfig.getTrustStoreProvider(), sslConfig.getTrustStorePassword());
    }

    protected Collection<? extends CRL> loadCRL(String crlPath) throws Exception 
    {
        Collection<? extends CRL> crlList = null;
        if (crlPath != null) 
        {
            InputStream in = null;
            try 
            {
            	in = new FileInputStream(crlPath); //assume it's a file
                crlList = CertificateFactory.getInstance("X.509").generateCRLs(in);
            } 
            finally 
            {
                if (in != null) 
                    in.close();                
            }
        }
        
        return crlList;
    }

    protected KeyStore getKeyStore(InputStream storeStream, String storePath, String storeType, String storeProvider, String storePassword) throws Exception 
    {
        KeyStore keystore = null;
        if (storeStream != null || storePath != null) 
        {
            InputStream inStream = storeStream;
            try 
            {
                if (inStream == null) 
                    inStream = new FileInputStream(storePath); //assume it's a file
                
                if (storeProvider != null) 
                    keystore = KeyStore.getInstance(storeType, storeProvider);
                else 
                    keystore = KeyStore.getInstance(storeType);
                
                keystore.load(inStream, storePassword == null ? null : storePassword.toCharArray());
            } 
            finally 
            {
                if (inStream != null) 
                    inStream.close();                
            }
        }
        
        return keystore;
    }

    protected KeyManager[] getKeyManagers(KeyStore keyStore) throws Exception 
    {
        KeyManager[] managers = null;
        if (keyStore != null) 
        {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(sslConfig.getKeyManagerFactoryAlgorithm());
            keyManagerFactory.init(keyStore, sslConfig.getKeyManagerPassword() == null? (sslConfig.getKeyStorePassword() == null?null: sslConfig.getKeyStorePassword().toCharArray()): sslConfig.getKeyManagerPassword().toCharArray());
            managers = keyManagerFactory.getKeyManagers();

            if (sslConfig.getCertAlias() != null) 
            {
                for (int idx = 0; idx < managers.length; idx++) 
                {
                    if (managers[idx] instanceof X509KeyManager) 
                        managers[idx] = new AliasedX509ExtendedKeyManager(sslConfig.getCertAlias(),(X509KeyManager)managers[idx]);                    
                }
            }
        }
        
        return managers;
    }

    protected TrustManager[] getTrustManagers(KeyStore trustStore, Collection<? extends CRL> crls) throws Exception 
    {   
        TrustManager[] managers = null;
        if (trustStore != null) 
        {
            if (sslConfig.isValidatePeerCerts() && sslConfig.getTrustManagerFactoryAlgorithm().equalsIgnoreCase("PKIX")) 
            {
                PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
                pbParams.setMaxPathLength(sslConfig.getMaxCertPathLength());
                pbParams.setRevocationEnabled(true);

                if (crls != null && !crls.isEmpty()) 
                    pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
                
                if (sslConfig.isEnableCRLDP()) 
                    System.setProperty("com.sun.security.enableCRLDP","true");
                
                if (sslConfig.isEnableOCSP()) 
                {
                    Security.setProperty("ocsp.enable","true");

                    if (sslConfig.getOcspResponderURL() != null) 
                        Security.setProperty("ocsp.responderURL", sslConfig.getOcspResponderURL());                    
                }

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(sslConfig.getTrustManagerFactoryAlgorithm());
                trustManagerFactory.init(new CertPathTrustManagerParameters(pbParams));
                managers = trustManagerFactory.getTrustManagers();
            } 
            else 
            {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(sslConfig.getTrustManagerFactoryAlgorithm());
                trustManagerFactory.init(trustStore);
                managers = trustManagerFactory.getTrustManagers();
            }
        }
        
        return managers;
    }

    public void checkKeyStore() 
    {
        if (sslContext != null)
            return;
        
        if (keyStoreInputStream == null && sslConfig.getKeyStorePath() == null) 
            throw new IllegalStateException("SSL doesn't have a valid keystore");
        
        if (trustStoreInputStream == null && sslConfig.getTrustStorePath() == null) 
        {
            trustStoreInputStream = keyStoreInputStream;
            sslConfig.setTrustStorePath(sslConfig.getKeyStorePath());
            sslConfig.setTrustStoreType(sslConfig.getKeyStoreType());
            sslConfig.setTrustStoreProvider(sslConfig.getKeyStoreProvider());
            sslConfig.setTrustStorePassword(sslConfig.getKeyStorePassword());
            sslConfig.setTrustManagerFactoryAlgorithm(sslConfig.getKeyManagerFactoryAlgorithm());
        }

        if (keyStoreInputStream != null && keyStoreInputStream == trustStoreInputStream) 
        {
            try 
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                streamCopy(keyStoreInputStream, baos, null, false);
                keyStoreInputStream.close();
                keyStoreInputStream = new ByteArrayInputStream(baos.toByteArray());
                trustStoreInputStream = new ByteArrayInputStream(baos.toByteArray());
            } 
            catch (Exception ex) 
            {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static void streamCopy(InputStream is, OutputStream os, byte[] buf, boolean close) throws IOException 
    {
        int len;
        if (buf == null) 
            buf = new byte[4096];
        
        while ((len = is.read(buf)) > 0) 
            os.write(buf, 0, len);
        
        os.flush();
        if (close) 
            is.close();        
    }

    private static boolean contains(Object[] arr, Object obj) 
    {
    	for (Object o : arr) 
    		if (o.equals(obj)) return true;
	
    	return false;
    }

    public String[] selectProtocols(String[] enabledProtocols, String[] supportedProtocols) 
    {
        Set<String> selected_protocols = new HashSet<String>();
        
        if (sslConfig.getIncludeProtocols() != null) 
        {
            for (String protocol : supportedProtocols)
                if (contains(sslConfig.getIncludeProtocols(), protocol))
                    selected_protocols.add(protocol);
        } 
        else 
            selected_protocols.addAll(Arrays.asList(enabledProtocols));
        
        if (sslConfig.getExcludeProtocols() != null) 
            selected_protocols.removeAll(Arrays.asList(sslConfig.getExcludeProtocols()));        

        return selected_protocols.toArray(new String[selected_protocols.size()]);
    }
    
    public String[] selectCipherSuites(String[] enabledCipherSuites, String[] supportedCipherSuites) 
    {
        Set<String> selected_ciphers = new HashSet<String>();
        
        if (sslConfig.getIncludeCipherSuites() != null) 
        {
            for (String cipherSuite : supportedCipherSuites)
                if (contains(sslConfig.getIncludeCipherSuites(), cipherSuite))
                    selected_ciphers.add(cipherSuite);
        } 
        else 
            selected_ciphers.addAll(Arrays.asList(enabledCipherSuites));
        
        if (sslConfig.getExcludeCipherSuites() != null) 
            selected_ciphers.removeAll(Arrays.asList(sslConfig.getExcludeCipherSuites()));
        
        return selected_ciphers.toArray(new String[selected_ciphers.size()]);
    }

    public SSLServerSocket newSslServerSocket(String host,int port,int backlog) throws IOException 
    {
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        SSLServerSocket socket = (SSLServerSocket) (host==null ? factory.createServerSocket(port, backlog): factory.createServerSocket(port, backlog, InetAddress.getByName(host)));

        if (sslConfig.getWantClientAuth())
            socket.setWantClientAuth(sslConfig.getWantClientAuth());
        
        if (sslConfig.getNeedClientAuth())
            socket.setNeedClientAuth(sslConfig.getNeedClientAuth());

        socket.setEnabledCipherSuites(selectCipherSuites(socket.getEnabledCipherSuites(),socket.getSupportedCipherSuites()));
        socket.setEnabledProtocols(selectProtocols(socket.getEnabledProtocols(),socket.getSupportedProtocols()));
	    return socket;
    }
    
    public SSLSocket newSslSocket() throws IOException 
    {
        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket)factory.createSocket();
        
        if (sslConfig.getWantClientAuth())
            socket.setWantClientAuth(sslConfig.getWantClientAuth());
    
        if (sslConfig.getNeedClientAuth())
            socket.setNeedClientAuth(sslConfig.getNeedClientAuth());

        socket.setEnabledCipherSuites(selectCipherSuites(socket.getEnabledCipherSuites(),socket.getSupportedCipherSuites()));   
        socket.setEnabledProtocols(selectProtocols(socket.getEnabledProtocols(),socket.getSupportedProtocols()));
        return socket;
    }
    
    public SSLEngine newSslEngine(String host,int port) 
    {
        SSLEngine sslEngine = sslConfig.isSessionCachingEnabled() ? sslContext.createSSLEngine(host, port) : sslContext.createSSLEngine();
	    customize(sslEngine);
        return sslEngine;
    }
    
    public SSLEngine newSslEngine() 
    {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        customize(sslEngine);
        return sslEngine;
    }

    private void customize(SSLEngine sslEngine) 
    {
        if (sslConfig.getWantClientAuth())
            sslEngine.setWantClientAuth(sslConfig.getWantClientAuth());
    
        if (sslConfig.getNeedClientAuth())
            sslEngine.setNeedClientAuth(sslConfig.getNeedClientAuth());

        sslEngine.setEnabledCipherSuites(selectCipherSuites(sslEngine.getEnabledCipherSuites(),sslEngine.getSupportedCipherSuites()));
	    sslEngine.setEnabledProtocols(selectProtocols(sslEngine.getEnabledProtocols(),sslEngine.getSupportedProtocols()));
    }
}