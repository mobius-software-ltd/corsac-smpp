package com.mobius.software.protocols.smpp;
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
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class CertificateHandler
{
	public static X509Certificate convertToX509Certificate(String pem) throws CertificateException, IOException
	{
		StringReader reader = new StringReader(pem);
		PEMParser pr = new PEMParser(reader);
		X509Certificate cert;
		try
		{
			X509CertificateHolder holder = (X509CertificateHolder) pr.readObject();
			cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(holder);
		}
		finally
		{
			pr.close();
		}

		return cert;
	}

	public static KeyStore getKeyStore(String certificate, String certificateChain,String privateKey) throws Exception
	{
		if(certificate==null || privateKey==null)
			throw new InvalidParameterException("certificate and private key can not be null");
		
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
		Security.insertProviderAt(new BouncyCastleProvider(), 0);

		PrivateKey pk=null;
		X509Certificate certificateValue=null;
		List<X509Certificate> certificateChainValue = new ArrayList<>();

		PEMParser reader = new PEMParser(new StringReader(certificate));
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
		JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();

		Object o;
		int count=0;
		while ((o = reader.readObject()) != null)
		{
			if(count>0)
			{
				reader.close();
				throw new InvalidParameterException("certificate contains multiple entries");
			}
			
			count++;
			if (o instanceof X509CertificateHolder)
				certificateValue=(certConverter.getCertificate((X509CertificateHolder) o));
			else
			{
				reader.close();
				throw new InvalidParameterException("certificate contains invalid entry");			
			}
		}
		
		reader.close();
		if(count==0)
			throw new InvalidParameterException("certificate is missing");
		
		reader = new PEMParser(new StringReader(privateKey));
		
		count=0;
		while ((o = reader.readObject()) != null)
		{
			if(count>0)
			{
				reader.close();
				throw new InvalidParameterException("private key contains multiple entries");
			}
			
			count++;
			if (o instanceof PEMKeyPair)
				pk = converter.getKeyPair((PEMKeyPair) o).getPrivate();			
			else if(o instanceof PrivateKeyInfo)
				pk=converter.getPrivateKey((PrivateKeyInfo)o);			
			else
			{
				reader.close();
				throw new InvalidParameterException("private key contains invalid entry");
			}
		}
		
		reader.close();
		if(count==0)
			throw new InvalidParameterException("private key is missing");
		
		if(certificateChain!=null)
		{
			reader = new PEMParser(new StringReader(certificateChain));
			
			while ((o = reader.readObject()) != null)
			{
				if (o instanceof X509CertificateHolder)
					certificateChainValue.add((certConverter.getCertificate((X509CertificateHolder) o)));
				else
				{
					reader.close();
					throw new InvalidParameterException("certificate chain contains invalid entry");
				}
			}

			reader.close();
		}
		
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, null);

		X509Certificate[] chain = new X509Certificate[certificateChainValue.size()+1];
		chain[0]=certificateValue;
		
		int index = 1;
		for (X509Certificate curr : certificateChainValue)
		{
			ks.setCertificateEntry(curr.getSubjectX500Principal().getName(), curr);
			chain[index++] = curr;
		}

		ks.setKeyEntry("main", pk, "".toCharArray(), chain);
		return ks;
	}
	
	public static String convertFromX509Certificate(X509Certificate cert) throws CertificateException, IOException
	{
		return new String(java.util.Base64.getMimeEncoder().encode(cert.getEncoded()),StandardCharsets.UTF_8);		
	}

	public static String getThumbprint(X509Certificate cert) throws CertificateEncodingException, NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		return SmppHelper.bytesToHex(md.digest(cert.getEncoded()));
	}
	
	private static TrustManager[] buildTrustManagers(final KeyStore trustStore) throws IOException 
	{
        TrustManager[] trustManagers = null;
        try 
        {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        catch (NoSuchAlgorithmException | KeyStoreException exc) 
        {
            throw new IOException("Unable to initialise TrustManager[]", exc);
        }
        
        return trustManagers;
    }

    private static KeyManager[] buildKeyManagers(final KeyStore keyStore, char[] storePassword) throws IOException 
    {
        KeyManager[] keyManagers;
        try 
        {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, storePassword);
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException exc) 
        {
            throw new IOException("Unable to initialise KeyManager[]", exc);
        }
        
        return keyManagers;
    }
    
    public static SSLContext createSslContext(KeyStore ks,KeyStore ts) throws IOException 
    {
        KeyManager[] keyManagers = buildKeyManagers(ks, "".toCharArray());
        TrustManager[] trustManagers = buildTrustManagers(ts);

        SSLContext sslContext;
        try 
        {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);
        }
        catch (NoSuchAlgorithmException | KeyManagementException exc) 
        {
            throw new IOException("Unable to create and initialise the SSLContext", exc);
        }

        return sslContext;
    }
}
