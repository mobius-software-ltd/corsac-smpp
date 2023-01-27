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
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SslConfiguration
{
    
    public static final String DEFAULT_KEYMANAGERFACTORY_ALGORITHM = (Security.getProperty("ssl.KeyManagerFactory.algorithm") == null ? "SunX509" : Security.getProperty("ssl.KeyManagerFactory.algorithm"));
    public static final String DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM = (Security.getProperty("ssl.TrustManagerFactory.algorithm") == null ? "SunX509" : Security.getProperty("ssl.TrustManagerFactory.algorithm"));
    
    private final Set<String> excludeProtocols = new HashSet<String>();
    private Set<String> includeProtocols = null;
    
    private final Set<String> excludeCipherSuites = new HashSet<String>();
    private Set<String> includeCipherSuites = null;

    private String keyStorePath;
    private String keyStoreProvider;
    private String keyStoreType = "JKS";
    private String trustStorePath;
    private String trustStoreProvider;
    private String trustStoreType = "JKS";
    private transient String keyStorePassword;
    private transient String trustStorePassword;
    private transient String keyManagerPassword;

    private String certAlias;

    private boolean needClientAuth = false;
    private boolean wantClientAuth = false;
    private boolean allowRenegotiate = true;

    private String sslProvider;
    private String sslProtocol = "TLS";

    private String secureRandomAlgorithm;
    private String keyManagerFactoryAlgorithm = DEFAULT_KEYMANAGERFACTORY_ALGORITHM;
    private String trustManagerFactoryAlgorithm = DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM;

    private boolean validateCerts;
    private boolean validatePeerCerts;
    private int maxCertPathLength = -1;
    private String crlPath;
    private boolean enableCRLDP = false;
    private boolean enableOCSP = false;
    private String ocspResponderURL;

    private boolean sessionCachingEnabled = true;
    private int sslSessionCacheSize;
    private int sslSessionTimeout;

    private boolean trustAll = true;

    public String[] getExcludeProtocols() 
    {
    	return this.excludeProtocols == null ? null : this.excludeProtocols.toArray(new String[this.excludeProtocols.size()]);
    }

    public void setExcludeProtocols(String... protocols) 
    {
        this.excludeProtocols.clear();
        this.excludeProtocols.addAll(Arrays.asList(protocols));
    }

    public void addExcludeProtocols(String... protocol) 
    {
        this.excludeProtocols.addAll(Arrays.asList(protocol));
    }
    
    public String[] getIncludeProtocols() 
    {
    	return this.includeProtocols == null ? null : this.includeProtocols.toArray(new String[this.includeProtocols.size()]);
    }

    public void setIncludeProtocols(String... protocols) 
    {
        this.includeProtocols = new HashSet<String>(Arrays.asList(protocols));
    }

    public String[] getExcludeCipherSuites() 
    {
    	return this.excludeCipherSuites == null ? null : this.excludeCipherSuites.toArray(new String[this.excludeCipherSuites.size()]);
    }

    public void setExcludeCipherSuites(String... cipherSuites) 
    {
        this.excludeCipherSuites.clear();
        this.excludeCipherSuites.addAll(Arrays.asList(cipherSuites));
    }
    
    public void addExcludeCipherSuites(String... cipher) 
    {
        this.excludeCipherSuites.addAll(Arrays.asList(cipher));
    }

    public String[] getIncludeCipherSuites() 
    {
    	return this.includeCipherSuites == null ? null: this.includeCipherSuites.toArray(new String[this.includeCipherSuites.size()]);
    }

    public void setIncludeCipherSuites(String... cipherSuites) 
    {
        this.includeCipherSuites = new HashSet<String>(Arrays.asList(cipherSuites));
    }

    public String getKeyStorePath() 
    {
        return this.keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) 
    {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStoreProvider() 
    {
        return this.keyStoreProvider;
    }

    public void setKeyStoreProvider(String keyStoreProvider) 
    {
        this.keyStoreProvider = keyStoreProvider;
    }

    public String getKeyStoreType() 
    {
        return this.keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) 
    {
        this.keyStoreType = keyStoreType;
    }

    public String getCertAlias() 
    {
        return this.certAlias;
    }

    public void setCertAlias(String certAlias) 
    {
        this.certAlias = certAlias;
    }

    public String getTrustStorePath() 
    {
        return this.trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) 
    {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStoreProvider() 
    {
        return this.trustStoreProvider;
    }

    public void setTrustStoreProvider(String trustStoreProvider) 
    {
        this.trustStoreProvider = trustStoreProvider;
    }

    public String getTrustStoreType() 
    {
        return this.trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) 
    {
        this.trustStoreType = trustStoreType;
    }

    public boolean getNeedClientAuth() 
    {
        return this.needClientAuth;
    }

    public void setNeedClientAuth(boolean needClientAuth) 
    {
        this.needClientAuth = needClientAuth;
    }

    public boolean getWantClientAuth() 
    {
        return this.wantClientAuth;
    }

    public void setWantClientAuth(boolean wantClientAuth) 
    {
        this.wantClientAuth = wantClientAuth;
    }

    public boolean isValidateCerts() 
    {
        return this.validateCerts;
    }

    public void setValidateCerts(boolean validateCerts) 
    {
        this.validateCerts = validateCerts;
    }

    public boolean isValidatePeerCerts() 
    {
        return this.validatePeerCerts;
    }

    public void setValidatePeerCerts(boolean validatePeerCerts) 
    {
        this.validatePeerCerts = validatePeerCerts;
    }

    public boolean isAllowRenegotiate() 
    {
        return this.allowRenegotiate;
    }

    public void setAllowRenegotiate(boolean allowRenegotiate) 
    {
        this.allowRenegotiate = allowRenegotiate;
    }

    public void setKeyStorePassword(String password) 
    {
        this.keyStorePassword = password;
    }

    public String getKeyStorePassword() 
    {
    	return this.keyStorePassword;
    }

    public void setKeyManagerPassword(String password) 
    {
        this.keyManagerPassword = password;
    }

    public String getKeyManagerPassword() 
    {
    	return this.keyManagerPassword;
    }

    public void setTrustStorePassword(String password) 
    {
        this.trustStorePassword = password;
    }

    public String getTrustStorePassword() 
    {
    	return this.trustStorePassword;
    }

    public String getProvider() 
    {
        return this.sslProvider;
    }

    public void setProvider(String provider) 
    {
        this.sslProvider = provider;
    }

    public String getProtocol() 
    {
        return this.sslProtocol;
    }

    public void setProtocol(String protocol) 
    {
        this.sslProtocol = protocol;
    }

    public String getSecureRandomAlgorithm() 
    {
        return this.secureRandomAlgorithm;
    }

    public void setSecureRandomAlgorithm(String algorithm) 
    {
        this.secureRandomAlgorithm = algorithm;
    }

    public String getKeyManagerFactoryAlgorithm() 
    {
        return this.keyManagerFactoryAlgorithm;
    }
    
    public void setKeyManagerFactoryAlgorithm(String algorithm) 
    {
        this.keyManagerFactoryAlgorithm = algorithm;
    }

    public String getTrustManagerFactoryAlgorithm() 
    {
        return this.trustManagerFactoryAlgorithm;
    }

    public boolean isTrustAll() 
    {
        return this.trustAll;
    }

    public void setTrustAll(boolean trustAll) 
    {
        this.trustAll = trustAll;
    }

    public void setTrustManagerFactoryAlgorithm(String algorithm) 
    {
        this.trustManagerFactoryAlgorithm = algorithm;
    }

    public String getCrlPath() 
    {
        return this.crlPath;
    }

    public void setCrlPath(String crlPath) 
    {
        this.crlPath = crlPath;
    }

    public int getMaxCertPathLength() 
    {
        return this.maxCertPathLength;
    }

    public void setMaxCertPathLength(int maxCertPathLength) 
    {
        this.maxCertPathLength = maxCertPathLength;
    }

    public boolean isEnableCRLDP() 
    {
        return this.enableCRLDP;
    }

    public void setEnableCRLDP(boolean enableCRLDP) 
    {
        this.enableCRLDP = enableCRLDP;
    }

    public boolean isEnableOCSP() 
    {
        return this.enableOCSP;
    }

    public void setEnableOCSP(boolean enableOCSP) 
    {
        this.enableOCSP = enableOCSP;
    }

    public String getOcspResponderURL() 
    {
        return this.ocspResponderURL;
    }

    public void setOcspResponderURL(String ocspResponderURL) 
    {
        this.ocspResponderURL = ocspResponderURL;
    }
    
    public boolean isSessionCachingEnabled() 
    {
        return this.sessionCachingEnabled;
    }
    
    public void setSessionCachingEnabled(boolean enableSessionCaching) 
    {
        this.sessionCachingEnabled = enableSessionCaching;
    }

    public int getSslSessionCacheSize() 
    {
        return this.sslSessionCacheSize;
    }

    public void setSslSessionCacheSize(int sslSessionCacheSize) 
    {
        this.sslSessionCacheSize = sslSessionCacheSize;
    }

    public int getSslSessionTimeout() 
    {
        return this.sslSessionTimeout;
    }

    public void setSslSessionTimeout(int sslSessionTimeout) 
    {
        this.sslSessionTimeout = sslSessionTimeout;
    }
    
}
