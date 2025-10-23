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
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.mobius.software.common.dal.timers.WorkerPool;
import com.mobius.software.protocols.smpp.channel.SmppServerSession;
import com.mobius.software.protocols.smpp.channel.SmppSessionHandler;
import com.mobius.software.protocols.smpp.client.SmppClient;
import com.mobius.software.protocols.smpp.exceptions.SmppChannelException;
import com.mobius.software.protocols.smpp.server.HealthCheckTimer;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class TestsBase 
{
	public static Logger logger=LogManager.getLogger(TestsBase.class);
	
	//for linux set this to true
	private static Boolean isEpoll=false;
	
	protected static Integer smppPort=2775;
	protected static Integer tlsPort=2776;
	
	protected static TestSmppClient client=new TestSmppClient();
	protected static TestSmppServer server=new TestSmppServer();
	protected static TestSmppServer tlsServer=new TestSmppServer();
	
	protected static Long bindTimeout=30000L;
	protected static Long connectTimeout=3000L;
	protected static Long enquiryLinkTimeout=3000L;
	protected static Long requestTimeout=10000L;
	
	private static ListenerWrapper listenerWrapper=new ListenerWrapper();
	
	private static final Integer maxWorkers=4;
	private static final Integer maxAcceptors=4;
	private static final Integer maxClients=4;
	
	private static EventLoopGroup acceptorGroup;
	private static EventLoopGroup clientGroup;
	
	private static WorkerPool workerPool=new WorkerPool("SMPP"); 
		
	protected static String certificate="-----BEGIN CERTIFICATE-----"
			+ "\nMIID5jCCAs4CCQDGO3NYWXh98DANBgkqhkiG9w0BAQUFADCBvjELMAkGA1UEBhMC"
			+ "\nVUExFTATBgNVBAgTDFphcG9yaXpoemhpYTEVMBMGA1UEBxMMWmFwb3Jpemh6aGlh"
			+ "\nMR0wGwYDVQQKExRNb2JpdXMgU29mdHdhcmUgUm9vdDEUMBIGA1UECxMLRGV2ZWxv"
			+ "\ncG1lbnQxHTAbBgNVBAMTFE1vYml1cyBTb2Z0d2FyZSBSb290MS0wKwYJKoZIhvcN"
			+ "\nAQkBFh53ZWJzdXBwb3J0QG1vYml1cy1zb2Z0d2FyZS5jb20wHhcNMTgwODEyMDkz"
			+ "\nODEzWhcNMjgwODA5MDkzODEzWjCBqjEYMBYGA1UEAwwPTW9iaXVzIFNvZnR3YXJl"
			+ "\nMRQwEgYDVQQLDAtEZXZlbG9wbWVudDEYMBYGA1UECgwPTW9iaXVzIFNvZnR3YXJl"
			+ "\nMQswCQYDVQQHDAJVQTEVMBMGA1UECAwMWmFwb3Jpemh6aGlhMQswCQYDVQQGEwJV"
			+ "\nQTEtMCsGCSqGSIb3DQEJARYed2Vic3VwcG9ydEBtb2JpdXMtc29mdHdhcmUuY29t"
			+ "\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgd7mAZD0k+hdu2mmOpDF"
			+ "\nJ7a2zwCgwlDJNGTkpyaIbPNLWMTQuaMq3TL4MkLqggOBHGCS3I++F27NYEcsqSaX"
			+ "\nfOhRWQurrPzw5ck7MSR/+Qbdb2Ntrp19C+2XAr6bu/lozRKuxoBPC3TB3BT1Pzfp"
			+ "\nhW3rKhesfPySAeCf4uJ1ngg1cv4756P6FKyQ0qw8dDndBVbBTyjEu6wl08mV+OWI"
			+ "\nvBszko6MK09kXTH7IXBPFjzyprQlX7bCxy6gUqZQrwjH7dCjavHCdNKVX5cg/v3f"
			+ "\nZS0z/CgOml870i+X2+Dc5BPiLVp+gYYg1OG9YmUq2G55gQMMZM4d7wnOstKSIu8t"
			+ "\nhQIDAQABMA0GCSqGSIb3DQEBBQUAA4IBAQA85djOUevpUiahKZoCS+TAJtMRb3jw"
			+ "\nGSCojniw7AnGiEUtw6LovpY5sBk/I9RP3M7v0zFTtvonfV6rUjsMxz7zHh1EZ8uG"
			+ "\nKY+1rVfTBuUOs15HH0Qyw7VlJmqvkptQu85bTZ58LXfKR2UxfruaWn486yAh4m1a"
			+ "\na2zIYrqwLDQfZIjKmdiv0+5RSgVkig44oBo++e+uF++e7sqcrnW2kEk91DE2ihr4"
			+ "\nxo0AS2I1RDo3fh+Fx9a4fiEjxpBMn0ApEMhJQ4rVp6Ev39aQB0j5EOSl2UfzN4gB"
			+ "\nVgMS1RlS/30xp+G9R7kH2pGmJ2eO9jKlQL8I2Ji0+8F+pYLp67kXY/Af"
			+ "\n-----END CERTIFICATE-----";
	
	protected static String certificateChain="-----BEGIN CERTIFICATE-----"
			+ "\nMIIFKjCCBBKgAwIBAgIJANso1RIy54acMA0GCSqGSIb3DQEBBQUAMIG+MQswCQYD"
			+ "\nVQQGEwJVQTEVMBMGA1UECBMMWmFwb3Jpemh6aGlhMRUwEwYDVQQHEwxaYXBvcml6"
			+ "\naHpoaWExHTAbBgNVBAoTFE1vYml1cyBTb2Z0d2FyZSBSb290MRQwEgYDVQQLEwtE"
			+ "\nZXZlbG9wbWVudDEdMBsGA1UEAxMUTW9iaXVzIFNvZnR3YXJlIFJvb3QxLTArBgkq"
			+ "\nhkiG9w0BCQEWHndlYnN1cHBvcnRAbW9iaXVzLXNvZnR3YXJlLmNvbTAeFw0xODA4"
			+ "\nMTIwOTM3NThaFw0yODA4MDkwOTM3NThaMIG+MQswCQYDVQQGEwJVQTEVMBMGA1UE"
			+ "\nCBMMWmFwb3Jpemh6aGlhMRUwEwYDVQQHEwxaYXBvcml6aHpoaWExHTAbBgNVBAoT"
			+ "\nFE1vYml1cyBTb2Z0d2FyZSBSb290MRQwEgYDVQQLEwtEZXZlbG9wbWVudDEdMBsG"
			+ "\nA1UEAxMUTW9iaXVzIFNvZnR3YXJlIFJvb3QxLTArBgkqhkiG9w0BCQEWHndlYnN1"
			+ "\ncHBvcnRAbW9iaXVzLXNvZnR3YXJlLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEP"
			+ "\nADCCAQoCggEBALySjcc8e7RJzRuUGVVBqTZNXUVyGGtb0HmueX5Vx9pubcWS9DFh"
			+ "\nT9PWIihGQAOt+jL4nSXeySQA1LfMSjUXxqBYjClkGqwEXm2vuaRqhejM056XobMN"
			+ "\npuuPHVDYzPDxIC0k6vak3+QwlPZNHlIq8xzgIYBPma8FlIwbpqlCFECeS7GiqLEH"
			+ "\n3TJEs67b9HrdL0UqKJT/ngSzB7EWM7sw7XQsgJJ77KCKcaeG5Stz2El7kZhAu2c6"
			+ "\n/9bDAfaUnGvMqB27LtYJiKPOnHbVfOS402DPAYMkivNPy1BgsNynrPoAdyeiqnVA"
			+ "\nlOgilFH0hiiVSM3Z9YxbYnJ/acWNg16w6IsCAwEAAaOCAScwggEjMB0GA1UdDgQW"
			+ "\nBBSrWurdT1qJilL9vGgepH5+aR5DDjCB8wYDVR0jBIHrMIHogBSrWurdT1qJilL9"
			+ "\nvGgepH5+aR5DDqGBxKSBwTCBvjELMAkGA1UEBhMCVUExFTATBgNVBAgTDFphcG9y"
			+ "\naXpoemhpYTEVMBMGA1UEBxMMWmFwb3Jpemh6aGlhMR0wGwYDVQQKExRNb2JpdXMg"
			+ "\nU29mdHdhcmUgUm9vdDEUMBIGA1UECxMLRGV2ZWxvcG1lbnQxHTAbBgNVBAMTFE1v"
			+ "\nYml1cyBTb2Z0d2FyZSBSb290MS0wKwYJKoZIhvcNAQkBFh53ZWJzdXBwb3J0QG1v"
			+ "\nYml1cy1zb2Z0d2FyZS5jb22CCQDbKNUSMueGnDAMBgNVHRMEBTADAQH/MA0GCSqG"
			+ "\nSIb3DQEBBQUAA4IBAQADW75JLL8rT8Xewh9CvwrtNc01wKeQmzEKdKU/zMRui70t"
			+ "\nFRSM70w9nBNgGIjJJQ+vIkjsFHauf5wv9h7am6a8qSwWcqq+IT7AT4LFTBvwFsUv"
			+ "\nlvfeBUG7lqO0ubGcAqXmtRg8gfKLh/1CtwrhTdJ/AYrtVSfo5zZ8tu04EWQ2U6mw"
			+ "\nZ9g5CqzlP0obQd0EtfSFVlGzk+71KCa64otBH81bDNI8wyHE3QT6K0efcrd7iD/G"
			+ "\n/4sKUrGOUq4CdrovH1kCn9yZ0qg+EszTr50sSuh7cUmATHsI8Ds4ESSXXuula/A2"
			+ "\nxIwdBMd7qfwoXDWZSUiYUFO9L7yKUob+EWHQXkIq"
			+ "\n-----END CERTIFICATE-----";
	
	protected static String privateKey="-----BEGIN RSA PRIVATE KEY-----"
			+ "\nMIIEpAIBAAKCAQEAgd7mAZD0k+hdu2mmOpDFJ7a2zwCgwlDJNGTkpyaIbPNLWMTQ"
			+ "\nuaMq3TL4MkLqggOBHGCS3I++F27NYEcsqSaXfOhRWQurrPzw5ck7MSR/+Qbdb2Nt"
			+ "\nrp19C+2XAr6bu/lozRKuxoBPC3TB3BT1PzfphW3rKhesfPySAeCf4uJ1ngg1cv47"
			+ "\n56P6FKyQ0qw8dDndBVbBTyjEu6wl08mV+OWIvBszko6MK09kXTH7IXBPFjzyprQl"
			+ "\nX7bCxy6gUqZQrwjH7dCjavHCdNKVX5cg/v3fZS0z/CgOml870i+X2+Dc5BPiLVp+"
			+ "\ngYYg1OG9YmUq2G55gQMMZM4d7wnOstKSIu8thQIDAQABAoIBABzwGNoifYzziuc2"
			+ "\nrFaCaZvmx6cqYafKrnqhPJ3OJTn5oEFgYY3rwKJXOByi8nQT6dHz5uWElfvMsbCR"
			+ "\nS29JbRnk/jNUOWWrWtYo16qkkmtfzzmBsy/kYbelsi9nX3YsJeEEF7OdZX+M/aFX"
			+ "\nfiofAEa97leLvOCAutv2PEom4cRbhuzi23vofY/AxwrPbaxZtzbpZeQdTNTeNV2l"
			+ "\nEf4ALfPizvNiLERilX0pYuq839R6Wfx12Bd+pNE9LU0VjuNjyLDWpFZf3vHF2COQ"
			+ "\nNLoSp3l0Zucf9CWIJqWnhiASb5u5ooPjAJ6o3XxKRnQRoSpyPfkYgCyhFPuIYv6C"
			+ "\nNivng2UCgYEA388wRmNwscBRQ8ipEri4cWYtod8JQMae3m1VXbztu0yA2Kc5QOQU"
			+ "\njG8la4RY+oy1qHznwrCJYJZRkmelXXekXa/Af21pHqV7Mqy3XlfhRj8L6RI1JwYk"
			+ "\nxf1SL2GJhCVfxOe23MPjw16U/X6FB0ALKuVtHr3osfCZDevB/40rUhcCgYEAlIzT"
			+ "\nXpllM5qhjMCw4hsp2Ez72DgybpWdRYJs3PfttKV/EBPcowJjvqM6/4/paFK5eMfS"
			+ "\nlOdVIzkM50+Y1gddMOOHan+SigLVImgeCqntbmPJmRvccW52Tof9cMJq2KnJBjFL"
			+ "\naKHAg+dyX/TYAC6Z0eyELr2ueFecw2Q5Ld6TSsMCgYEAiZxzKlRqLmD5lpwCmShL"
			+ "\nAC67UBQ7NEDr3geLvZ807T0U3CG16lhS6iZM89bkfumVqItkVSkGzwSeE073Nokh"
			+ "\n3xj5W2CCif1lyrq35KJUOUT6pcw0MlJsufAQYGGwlDgGsqNmpEct/CpjoZnxYYvX"
			+ "\nUgDPH1/Ve9NbyFt1ZRP/1vECgYB7ULxWl3hPcloRghRUXsBJ8v5N67jR3BmGjlLY"
			+ "\nzGfjwk7MhfBu0ZkDtHVRmaHlHGcjQJ4rRi6C4uU3T/hMFCjkYL0VR3naX6eWvF/T"
			+ "\n8mRLc0LzexFwiIZlgrZ9WKdh3PAn19wFq+EonoVv6s00uXqvrWu9cXDYLcLQ4O4m"
			+ "\nidI0CQKBgQC+TlkJBxw71/IMtFQlI4uDRg20PhUqmo7/ptMFUA7BNW0/N49ydHsC"
			+ "\n9VU33PJZ0cYMzBuO+R2GnI228IcXEnTfeL/TM9GmsdzB4KGle9fWapLArnV17ACj"
			+ "\nJh1yqiHKC8uXK4uIuWvcYwhvaDAis5ZscZRlAKP1hcAR2EY0dwsikw=="
			+ "\n-----END RSA PRIVATE KEY-----";
		
	private static KeyStore serverKeyStore;//, serverTrustore;
	
	private static ConcurrentHashMap<String, SmppChannelConfig> allowedClients=new ConcurrentHashMap<String, SmppChannelConfig>();
	private static ConcurrentHashMap<String, AtomicInteger> usedClients=new ConcurrentHashMap<String, AtomicInteger>();
	private static ConcurrentHashMap<String, AtomicInteger> heartbeatClients=new ConcurrentHashMap<String, AtomicInteger>();
	private static ConcurrentHashMap<String, ConcurrentLinkedQueue<MessageRequestWrapper>> requests=new ConcurrentHashMap<String, ConcurrentLinkedQueue<MessageRequestWrapper>>();
	private static ConcurrentHashMap<String, ConcurrentLinkedQueue<StatusRequestWrapper>> statusRequests=new ConcurrentHashMap<String, ConcurrentLinkedQueue<StatusRequestWrapper>>();
	private static ConcurrentHashMap<String, ConcurrentLinkedQueue<ResponseWrapper>> responses=new ConcurrentHashMap<String, ConcurrentLinkedQueue<ResponseWrapper>>();
	private static ConcurrentHashMap<String, ConcurrentLinkedQueue<TimeoutWrapper>> timeouts=new ConcurrentHashMap<String, ConcurrentLinkedQueue<TimeoutWrapper>>();
		
	private static ConnectionListener connectionListener;	
	
	@BeforeClass
	public static void setUp() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		//Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
		//Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
		//Security.insertProviderAt(new BouncyCastleProvider(), 0);
		//Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);
		
		try
		{
			X509Certificate certificateValue=CertificateHandler.convertToX509Certificate(certificate);
			if(certificateValue==null)
			{
				logger.error("invalid certificate found");
				assertEquals(1,2);
			}
			
			if(certificateValue.getNotAfter()!=null && certificateValue.getNotAfter().getTime()<System.currentTimeMillis())
			{
				logger.error("invalid already invalid");
				assertEquals(1,2);
			}
			
			if(certificateValue.getNotBefore()!=null && certificateValue.getNotBefore().getTime()>System.currentTimeMillis())
			{
				logger.error("invalid is not valid yet");
				assertEquals(1,2);
			}
			
			serverKeyStore=CertificateHandler.getKeyStore(certificate, certificateChain, privateKey);
			if(serverKeyStore==null)
			{
				logger.error("could not validate security certificates and/or key");
				assertEquals(1,2);
			}
			
			//serverTrustore = KeyStore.getInstance("JKS");
			//serverTrustore.load(null, null);
		}
		catch(Exception ex)
		{
			logger.error(ex.getMessage());
			assertEquals(1,2);
		}
		
		if(isEpoll)
		{
			acceptorGroup=new EpollEventLoopGroup(maxAcceptors);
			clientGroup=new EpollEventLoopGroup(maxClients);
		}
		else
		{
			acceptorGroup=new NioEventLoopGroup(maxAcceptors);
			clientGroup=new NioEventLoopGroup(maxClients);
		}
		
		workerPool.start(maxWorkers);
		
		SmppChannelConfig config=new SmppChannelConfig(null, null, connectTimeout, bindTimeout, requestTimeout, enquiryLinkTimeout, null);
		server.start("default", false, isEpoll, "0.0.0.0", smppPort, null, null, config, listenerWrapper, listenerWrapper, workerPool, acceptorGroup, clientGroup, maxWorkers);
		tlsServer.start("tls", true, isEpoll, "0.0.0.0", tlsPort, serverKeyStore, null, config, listenerWrapper, listenerWrapper, workerPool, acceptorGroup, clientGroup, maxWorkers);
	}
		
	@AfterClass
	public static void shutdown()
	{
		listenerWrapper.stop();
	}
	
	public static void stopServers()
	{
		server.stop();
		tlsServer.stop();
		
		Iterator<AtomicInteger> iterator=usedClients.values().iterator();
		while(iterator.hasNext())
			iterator.next().set(0);	
		
		iterator=heartbeatClients.values().iterator();
		while(iterator.hasNext())
			iterator.next().set(0);	
	}
	
	public static void startServer() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		SmppChannelConfig config=new SmppChannelConfig(null, null, connectTimeout, bindTimeout, requestTimeout, enquiryLinkTimeout, null);
		server.start("default", false, isEpoll, "0.0.0.0", smppPort, null, null, config, listenerWrapper, listenerWrapper, workerPool, acceptorGroup, clientGroup, maxWorkers);
		tlsServer.start("tls", true, isEpoll, "0.0.0.0", tlsPort, serverKeyStore, null, config, listenerWrapper, listenerWrapper, workerPool, acceptorGroup, clientGroup, maxWorkers);
	}
	
	protected static void setConnectionListener(ConnectionListener newConnectionListener)
	{
		connectionListener=newConnectionListener;
	}
	
	protected static void resetConnectionListener()
	{
		connectionListener=null;
	}
	
	protected static void startClient(String connectionID,Boolean isTLS,Boolean useServerKeystore,Boolean enableEnquiry,Integer maxConnections,Integer maxRequestedConnections,String username,String password) throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		Integer port=smppPort;
		KeyStore ks=null,ts=null;
		if(isTLS)
		{
			port=tlsPort;
			if(useServerKeystore)
			{
				ks=serverKeyStore;
				//ts=serverTrustore;
			}
		}
	
		if(allowedClients.containsKey(connectionID))
			throw new SmppChannelException("connection already in list");
		
		SmppChannelConfig config;
		if(enableEnquiry)
			config=new SmppChannelConfig(username, password, connectTimeout, bindTimeout, requestTimeout, enquiryLinkTimeout, maxRequestedConnections);
		else
			config=new SmppChannelConfig(username, password, connectTimeout, bindTimeout, requestTimeout, 0L, maxRequestedConnections);
		
		SmppChannelConfig storedConfig=new SmppChannelConfig(username, password, connectTimeout, bindTimeout, requestTimeout, enquiryLinkTimeout, maxConnections);
		AtomicInteger usedCount=new AtomicInteger(0);
		AtomicInteger heartbeatCount=new AtomicInteger(0);
		
		allowedClients.put(connectionID, storedConfig);
		usedClients.put(connectionID, usedCount);
		heartbeatClients.put(connectionID, heartbeatCount);
		requests.put(connectionID, new ConcurrentLinkedQueue<MessageRequestWrapper>());
		statusRequests.put(connectionID, new ConcurrentLinkedQueue<StatusRequestWrapper>());
		responses.put(connectionID, new ConcurrentLinkedQueue<ResponseWrapper>());
		timeouts.put(connectionID, new ConcurrentLinkedQueue<TimeoutWrapper>());
		
		client.start(connectionID, isTLS, isEpoll, "127.0.0.1", port, ks, ts, config, listenerWrapper, listenerWrapper, workerPool, acceptorGroup, clientGroup, maxWorkers);
	}
	
	protected static void sendMessage(String sourceID,String messageID, SubmitSm submitSm)
	{
		client.sendMessage(sourceID, messageID, submitSm);
	}
	
	protected static void sendDelivery(String sourceID,String messageID, DeliverSm deliverSm)
	{
		client.sendDelivery(sourceID, messageID, deliverSm);
	}
	
	protected static void stopClient(String connectionID)
	{
		client.removeClientSession(connectionID);
		allowedClients.remove(connectionID);
	}
	
	protected static Integer getUsedClients(String connectionID)
	{
		AtomicInteger count=usedClients.get(connectionID);
		if(count!=null)
			return count.get();
		
		return 0;
	}
	
	protected static Integer getHeartbeats(String connectionID)
	{
		AtomicInteger count=heartbeatClients.get(connectionID);
		if(count!=null)
			return count.get();
		
		return 0;
	}
	
	protected static ConcurrentLinkedQueue<MessageRequestWrapper> getRequests(String connectionID)
	{
		return requests.get(connectionID);
	}
	
	
	protected static ConcurrentLinkedQueue<StatusRequestWrapper> getStatuses(String connectionID)
	{
		return statusRequests.get(connectionID);
	}
	
	
	protected static ConcurrentLinkedQueue<ResponseWrapper> getResponses(String connectionID)
	{
		return responses.get(connectionID);
	}
	
	
	protected static ConcurrentLinkedQueue<TimeoutWrapper> getTimeouts(String connectionID)
	{
		return timeouts.get(connectionID);
	}
	
	
	protected static class ListenerWrapper implements SmppSessionListener, ConnectionListener
	{
		public void stop() 
		{
			client.stop();
			
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			server.stop();
			tlsServer.stop();			
						
			acceptorGroup.shutdownGracefully();
			clientGroup.shutdownGracefully();
			
			workerPool.stop();
			
			allowedClients.clear();
			usedClients.clear();	
			heartbeatClients.clear();
			requests.clear();
			statusRequests.clear();
			responses.clear();
			timeouts.clear();
		}

		public SmppSessionHandler createClientHandler(SmppClient client,String uniqueID)
		{
			return new TestSmppClientSessionHandler(client, listenerWrapper, uniqueID, workerPool.getPeriodicQueue());
		}
		
		public SmppSessionHandler createServerHandler(SmppServerSession session, HealthCheckTimer timer)
		{
			return new TestSmppServerSessionHandler(listenerWrapper, timer, session, workerPool.getPeriodicQueue());
		}
		
		@Override
		public String bindRequested(String remoteHost,Integer remotePort, String systemID, String password, Boolean isTls) throws IllegalStateException 
		{
			Iterator<Entry<String,SmppChannelConfig>> iterator=allowedClients.entrySet().iterator();
			while(iterator.hasNext())
			{
				Entry<String,SmppChannelConfig> currEntry=iterator.next();
				if(currEntry.getValue().getUsername().equals(systemID))
				{
					if(currEntry.getValue().getPassword().equals(password))
					{
						AtomicInteger used=usedClients.get(currEntry.getKey());
						if(used.incrementAndGet()>currEntry.getValue().getMaxChannels())
						{
							used.decrementAndGet();
							throw new IllegalStateException("All allowed connections are already used");
						}
						
						if(connectionListener!=null)
							connectionListener.connected(currEntry.getKey());
						
						return currEntry.getKey();
					}
					else
						return null;
				}
			}
			
			return null;
		}

		@Override
		public void unbindRequested(String remoteHost, Integer remotePort, String uniqueID) 
		{
			AtomicInteger used=usedClients.get(uniqueID);
			if(used!=null && used.get()>0)
				used.decrementAndGet();
			
			if(connectionListener!=null)
				connectionListener.disconnected(uniqueID);
		}

		@Override
		public void connected(String uniqueID)
		{
			//not used here
		}
		
		@Override
		public void disconnected(String uniqueID)
		{
			//not used here
		}
		
		@Override
		public void connectionEstablished(String remoteHost, Integer remotePort, String uniqueID)
		{
			//not used here
		}
		
		@Override
		public void sessionBound(String remoteHost, Integer remotePort, String uniqueID)
		{
			
		}
		
		@Override
		public void sessionUnbound(String remoteHost, Integer remotePort, String uniqueID)
		{
			
		}
		
		@Override
		public void messageReceived(String originator, List<String> to, byte[] data, byte[] udh, ReportData reportData, AsyncCallback<RequestProcessingResult> callback) 
		{
			ConcurrentLinkedQueue<MessageRequestWrapper> requestsQueue=requests.get(originator);
			if(requestsQueue!=null)
				requestsQueue.offer(new MessageRequestWrapper(to, data, udh, reportData, callback));
			
			if(connectionListener!=null)
				connectionListener.messageReceived(originator, to, data, udh, reportData, callback);
		}

		@Override
		public void statusReceived(String originator,String messageID,AsyncCallback<DeliveryProcessingResult> response) 
		{
			ConcurrentLinkedQueue<StatusRequestWrapper> statusesQueue=statusRequests.get(originator);
			if(statusesQueue!=null)
				statusesQueue.offer(new StatusRequestWrapper(messageID, response));
			
			if(connectionListener!=null)
				connectionListener.statusReceived(originator,messageID,response);
		}

		@Override
		public void responseReceived(String originator,String originalMessageID, String remoteMessageID, MessageStatus status) 
		{	
			ConcurrentLinkedQueue<ResponseWrapper> responsesQueue=responses.get(originator);
			if(responsesQueue!=null)
				responsesQueue.offer(new ResponseWrapper(originalMessageID, remoteMessageID, status));
			
			if(connectionListener!=null)
				connectionListener.responseReceived(originator, originalMessageID, remoteMessageID, status);
		}

		@Override
		public void timeoutReceived(String originator,String originalMessageID) 
		{		
			ConcurrentLinkedQueue<TimeoutWrapper> timeoutQueue=timeouts.get(originator);
			if(timeoutQueue!=null)
				timeoutQueue.offer(new TimeoutWrapper(originalMessageID));
			
			if(connectionListener!=null)
				connectionListener.timeoutReceived(originator, originalMessageID);
		}

		@Override
		public void heartbeatReceived(String originator) 
		{
			AtomicInteger heartbeats=heartbeatClients.get(originator);
			if(heartbeats!=null)
				heartbeats.incrementAndGet();
			
			if(connectionListener!=null)
				connectionListener.heartbeatReceived(originator);
		}
	}
}