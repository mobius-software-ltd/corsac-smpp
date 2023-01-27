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
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class to handle validation of certificates, aliases and keystores
 *
 * Allows specifying Certificate Revocation List (CRL), as well as enabling
 * CRL Distribution Points Protocol (CRLDP) certificate extension support,
 * and also enabling On-Line Certificate Status Protocol (OCSP) support.
 * 
 * IMPORTANT: at least one of the above mechanisms *MUST* be configured and
 * operational, otherwise certificate validation *WILL FAIL* unconditionally.
 * 
 * @author Jetty7
 */
public class CertificateValidator
{
    private static final Logger logger = LoggerFactory.getLogger(CertificateValidator.class);
    private static AtomicLong aliasCount = new AtomicLong();
    
    private KeyStore trustStore;
    private Collection<? extends CRL> crls;

    private int maxCertPathLength = -1;
    private boolean enableCRLDP = false;
    private boolean enableOCSP = false;
    private String ocspResponderURL;
    
    public CertificateValidator(KeyStore trustStore, Collection<? extends CRL> crls) 
    {
        if (trustStore == null) 
            throw new InvalidParameterException("TrustStore must be specified for CertificateValidator.");
        
        this.trustStore = trustStore;
        this.crls = crls;
    }
    
    public void validate(KeyStore keyStore) throws CertificateException 
    {
        try 
        {
            Enumeration<String> aliases = keyStore.aliases();
            for ( ; aliases.hasMoreElements(); ) 
            {
                String alias = aliases.nextElement();
                validate(keyStore,alias);
            }
        } 
        catch (KeyStoreException kse) 
        {
            throw new CertificateException("Unable to retrieve aliases from keystore", kse);
        }
    }
    
    public String validate(KeyStore keyStore, String keyAlias) throws CertificateException 
    {
        String result = null;
        if (keyAlias != null) 
        {
            try 
            {
                validate(keyStore, keyStore.getCertificate(keyAlias));
            } 
            catch (KeyStoreException kse) 
            {
                logger.debug("", kse);
                throw new CertificateException("Unable to validate certificate" + " for alias [" + keyAlias + "]: " + kse.getMessage(), kse);
            }
            result = keyAlias;
        }
	
        return result;
    }
    
    public void validate(KeyStore keyStore, Certificate cert) throws CertificateException 
    {
        Certificate[] certChain = null;
        if (cert != null && cert instanceof X509Certificate) 
        {
            ((X509Certificate)cert).checkValidity();
            
            String certAlias = null;
            try 
            {
                if (keyStore == null) 
                    throw new InvalidParameterException("Keystore cannot be null");
                
                certAlias = keyStore.getCertificateAlias((X509Certificate)cert);
                if (certAlias == null) 
                {
                    certAlias = "CHSMPP" + String.format("%016X", aliasCount.incrementAndGet());
                    keyStore.setCertificateEntry(certAlias, cert);
                }
                
                certChain = keyStore.getCertificateChain(certAlias);
                if (certChain == null || certChain.length == 0) 
                    throw new IllegalStateException("Unable to retrieve certificate chain");                
            }
            catch (KeyStoreException kse) 
            {
                logger.debug("", kse);
                throw new CertificateException("Unable to validate certificate" + (certAlias == null ? "":" for alias [" +certAlias + "]") + ": " + kse.getMessage(), kse);
            }
            
            validate(certChain);
        }
    }
    
    public void validate(Certificate[] certChain) throws CertificateException 
    {
        try 
        {
            ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>();
            for (Certificate item : certChain) 
            {
                if (item == null) continue;
                if (!(item instanceof X509Certificate)) 
                    throw new IllegalStateException("Invalid certificate type in chain");
                
                certList.add((X509Certificate)item);
            }
    
            if (certList.isEmpty()) 
                throw new IllegalStateException("Invalid certificate chain");
            
            X509CertSelector certSelect = new X509CertSelector();
            certSelect.setCertificate(certList.get(0));
            
            PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, certSelect);
            pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList)));
    
            pbParams.setMaxPathLength(maxCertPathLength);
            pbParams.setRevocationEnabled(true);
    
            if (crls != null && !crls.isEmpty()) 
                pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
            
            if (enableOCSP) 
                Security.setProperty("ocsp.enable","true");
            
            if (enableCRLDP) 
                System.setProperty("com.sun.security.enableCRLDP","true");
            
            CertPathBuilderResult buildResult = CertPathBuilder.getInstance("PKIX").build(pbParams);               
            CertPathValidator.getInstance("PKIX").validate(buildResult.getCertPath(),pbParams);
        } 
        catch (GeneralSecurityException gse) 
        {
            logger.debug("", gse);
            throw new CertificateException("Unable to validate certificate: " + gse.getMessage(), gse);
        }
    }

    public KeyStore getTrustStore() 
    {
        return trustStore;
    }

    public Collection<? extends CRL> getCrls() 
    {
        return crls;
    }

    public int getMaxCertPathLength() 
    {
        return maxCertPathLength;
    }

    public void setMaxCertPathLength(int maxCertPathLength) 
    {
    	this.maxCertPathLength = maxCertPathLength;
    }
    
    public boolean isEnableCRLDP() 
    {
        return enableCRLDP;
    }

    public void setEnableCRLDP(boolean enableCRLDP) 
    {
        this.enableCRLDP = enableCRLDP;
    }

    public boolean isEnableOCSP() 
    {
        return enableOCSP;
    }

    public void setEnableOCSP(boolean enableOCSP) 
    {
    	this.enableOCSP = enableOCSP;
    }

    public String getOcspResponderURL() 
    {
        return ocspResponderURL;
    }

    public void setOcspResponderURL(String ocspResponderURL) 
    {
    	this.ocspResponderURL = ocspResponderURL;
    }
}