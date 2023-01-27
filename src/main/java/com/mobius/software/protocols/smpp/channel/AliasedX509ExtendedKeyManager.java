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
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

public class AliasedX509ExtendedKeyManager extends X509ExtendedKeyManager
{
    private String keyAlias;
    private X509KeyManager keyManager;

    public AliasedX509ExtendedKeyManager(String keyAlias, X509KeyManager keyManager) throws Exception 
    {
        this.keyAlias = keyAlias;
        this.keyManager = keyManager;
    }

    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) 
    {
        return keyAlias == null ? keyManager.chooseClientAlias(keyType, issuers, socket) : keyAlias;
    }

    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) 
    {   
        return keyAlias == null ? keyManager.chooseServerAlias(keyType, issuers, socket) : keyAlias;
    }

    public String[] getClientAliases(String keyType, Principal[] issuers) 
    {
        return keyManager.getClientAliases(keyType, issuers);
    }

    public String[] getServerAliases(String keyType, Principal[] issuers) 
    {
        return keyManager.getServerAliases(keyType, issuers);
    }

    public X509Certificate[] getCertificateChain(String alias) 
    {
        return keyManager.getCertificateChain(alias);
    }

    public PrivateKey getPrivateKey(String alias) 
    {
        return keyManager.getPrivateKey(alias);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) 
    {
        return keyAlias == null ? super.chooseEngineServerAlias(keyType,issuers,engine) : keyAlias;
    }

    @Override
    public String chooseEngineClientAlias(String keyType[], Principal[] issuers, SSLEngine engine)
    {
        return keyAlias == null ? super.chooseEngineClientAlias(keyType,issuers,engine) : keyAlias;
    }
}