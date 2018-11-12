package com.photino.dnr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.crypto.Cipher;


public class PhotinoDNR 
{
	public static String version = "0.9.0.0";
	public static int versionNum = 900;
	
	public static Boolean hasBeenValidated;
	
	public static String myIP;
	public static String validationID;
	public static String apiPath;
	public static String apiIP;
	public static String dnrSeed;
	public static Boolean messageQueued;
	public static int tokenexpiry;
	public static int messagequeexpiry;
	
	public static DatagramSocket[] socketIn;
	public static DatagramSocket[] socketOut;
	public static DatagramSocket socketInDnr;
	public static DatagramSocket socketOutDnr;
	
	public static int dnrIn;
	public static int dnrOut;
	
	public static int udppacketsize = 32768;
	
	public static ConcurrentHashMap<String,String> dnrdomain;// dnr node ip map
	public static ConcurrentHashMap<String,String> quedomain;// que node ip map
	public static ConcurrentHashMap<String,String> topicdomain;// topic to ip map
	public static ConcurrentHashMap<String,String> domaintopic;// ip to multi-topic map
	public static ConcurrentHashMap<String,String> activeip;// active ip list of gateway nodes for keep alives
	public static ConcurrentHashMap<String,String> activeiprouter;// list of active ips that are behind router/firewalls
	public static ConcurrentHashMap<String,String> userregistry;// user@topic to userid DID map
	public static ConcurrentHashMap<String,String> userregistrydid;// userid DID to user@topic(s) map
	public static ConcurrentHashMap<String,String> tokendevice;// user token to device id - for remote notifications
	public static ConcurrentHashMap<String,String> userpublickey;// user tag to public key
	public static ConcurrentHashMap<String,String> messageque;// for queuing messages for recipients where their gateway is offline
	public static ConcurrentHashMap<PrivateKey,String> keypairs;// for storing public private key pairs
	public static ConcurrentHashMap<String,String> receivedip;// for tracking ips that have been received - for frequency blacklist
	public static ConcurrentHashMap<String,String> blacklistip;// black listed ips
	public static ConcurrentHashMap<String,String> whitelistip;// white listed ips

	public static void main(String[] args) throws Exception
	{
		new PhotinoDNR();

	}
	public PhotinoDNR() throws Exception
	{
		//
		// get config properties file
		//
		File pf = new File( "dnr.properties");
		Properties prop = new Properties();
		InputStream is = null;
		try
		{		
			if ( pf.exists() && !pf.isDirectory() )
			{
				is = new FileInputStream(pf); 
				prop.load(is);
				System.out.println(prop.getProperty("API_PATH"));
				System.out.println(prop.getProperty("GATEWAY_START_PORT"));
				System.out.println(prop.getProperty("THREAD_POOL_COUNT"));
				System.out.println(prop.getProperty("ROOT_DIR"));
				System.out.println(prop.getProperty("VALIDATION_ID"));
				System.out.println(prop.getProperty("DNR_SEED"));
				System.out.println(prop.getProperty("TOKEN_EXPIRY"));
				System.out.println(prop.getProperty("MESSAGEQUE_EXPIRY"));
			}
		}
		catch (IOException ex) 
		{
			ex.printStackTrace();
		} 
		finally 
		{
			if ( is != null ) 
			{
				try 
				{
					is.close();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
		
		//
		// intialize
		//
		messageQueued = false;
		//
		// create the gui
		//
		GUI gui = new GUI();
		GUI.updateVersion(version);
		//
		// get my ip
		//
		myIP = InetAddress.getLocalHost().getHostAddress();
		
		//
		// API path
		//
		apiPath = "http://message.photino.io/";
		try 
		{
			if ( !prop.getProperty("API_PATH").equals("") )
			{
				apiPath = prop.getProperty("API_PATH");
			}
		}
		catch (Exception ex) {}
		apiIP = apiPath.toUpperCase();
		if ( apiIP.substring(0,4).equals("HTTP") ) apiIP = apiPath.substring(apiPath.indexOf("//")+2);
		if ( apiIP.indexOf("/") > 0 ) apiIP = apiIP.substring(0,apiIP.indexOf("/"));
		InetAddress address = InetAddress.getByName(apiIP); 
		apiIP = address.getHostAddress();
		//
		// dnr seed
		//
		dnrSeed = "158.69.198.174";
		try 
		{
			if ( !prop.getProperty("DNR_SEED").equals("") )
			{
				dnrSeed = prop.getProperty("DNR_SEED");
			}
		}
		catch (Exception ex) {}
		try 
		{
			URL url = new URL(apiPath+"getSeed.php");
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			InputStream stream = connection.getInputStream();
			String result = new BufferedReader(new InputStreamReader(stream))
					  .lines().collect(Collectors.joining("\n"));
			stream.close();
			if ( !result.equals("") )
			{
				dnrSeed = result;
			}
			System.out.println("DNRSeed from api "+result);
		} 
		catch (IOException e) {e.printStackTrace();}
		//
		// validation ID
		//
		validationID = "";
		try 
		{
			if ( !prop.getProperty("VALIDATION_ID").equals("") )
			{
				validationID = prop.getProperty("VALIDATION_ID");
			}
		}
		catch (Exception ex) {}
		//
		// expiry timeouts in days
		//
		tokenexpiry = 30;
		messagequeexpiry = 30;
		try 
		{
			if ( Integer.parseInt(prop.getProperty("TOKEN_EXPIRY")) > 0 )
			{
				tokenexpiry = Integer.parseInt(prop.getProperty("TOKEY_EXPIRY"));
				if ( tokenexpiry > 100 || tokenexpiry < 1 ) tokenexpiry = 30;
			}
		}
		catch (Exception ex) {}
		try 
		{
			if ( Integer.parseInt(prop.getProperty("MESSAGEQUE_EXPIRY")) > 0 )
			{
				messagequeexpiry = Integer.parseInt(prop.getProperty("MESSAGEQUE_EXPIRY"));
				if ( messagequeexpiry > 100 || messagequeexpiry < 1 ) messagequeexpiry = 30;
			}
		}
		catch (Exception ex) {}
		//
		//udp sockets
		//
		int startIn = 55000;
		int startOut = startIn + 1000;
		int maxnumIn = 25;
		int maxnumOut = maxnumIn;
		
		dnrIn = 59000;
		dnrOut = dnrIn + 1;

		socketIn = new DatagramSocket[maxnumIn];
		socketOut = new DatagramSocket[maxnumOut];
		socketInDnr = new DatagramSocket();
		socketOutDnr = new DatagramSocket();

		DatagramPacket[] receivePacketIn = new DatagramPacket[maxnumIn];
		DatagramPacket[] receivePacketOut = new DatagramPacket[maxnumOut];
		DatagramPacket receivePacketInDnr;
		DatagramPacket receivePacketOutDnr;
		

		byte[][] receiveDataIn = new byte[maxnumIn][PhotinoDNR.udppacketsize];
		byte[][] receiveDataOut = new byte[maxnumOut][PhotinoDNR.udppacketsize];
		byte[] receiveDataInDnr = new byte[PhotinoDNR.udppacketsize];
		byte[] receiveDataOutDnr = new byte[PhotinoDNR.udppacketsize];

		//
		//setup the sockets
		//
		for(int i = 0 ; i < maxnumIn; i++)
		{
			socketIn[i] = new DatagramSocket(startIn+i);
			receivePacketIn[i] = new DatagramPacket(receiveDataIn[i], receiveDataIn[i].length);
			//GUI.outputText("Socket in: " + startIn+i);
		}
		GUI.outputText("Socket in array started..."+Integer.toString(maxnumIn));
		for(int i = 0 ; i < maxnumOut; i++)
		{
			 socketOut[i] = new DatagramSocket(startOut+i);
			 receivePacketOut[i] = new DatagramPacket(receiveDataOut[i], receiveDataOut[i].length);
			 //GUI.outputText("Socket out: " + startOut+i);
		}
		GUI.outputText("Socket out array started..."+Integer.toString(maxnumOut));
		
		GUI.updateIPPort(myIP,Integer.toString(startIn), Integer.toString(startIn + maxnumIn - 1),Integer.toString(startOut), Integer.toString(startOut + maxnumOut - 1));

		//
		//Maps for DNR caching
		//
		dnrdomain = new ConcurrentHashMap<String, String>();// dnr node ip map
		quedomain = new ConcurrentHashMap<String, String>();// que node ip map
		topicdomain = new ConcurrentHashMap<String, String>();// topic to ip map
		domaintopic = new ConcurrentHashMap<String, String>();// ip to multi-topic map
		activeip = new ConcurrentHashMap<String, String>(16,(float) 0.75,30);// active ip list of gateway nodes for keep alives
		activeiprouter = new ConcurrentHashMap<String, String>();// active ip list behind fw/router
		userregistry = new ConcurrentHashMap<String, String>();// user@topic to userid DID map
		userregistrydid = new ConcurrentHashMap<String, String>();// userid DID to user@topic(s) map
		tokendevice = new ConcurrentHashMap<String, String>();// user token to device id - for remote notifications
		userpublickey = new ConcurrentHashMap<String,String>();// user tag to public key
		messageque = new ConcurrentHashMap<String,String>();// for queuing messages for recipients where their gateway is offline
		keypairs = new ConcurrentHashMap<PrivateKey, String>();// storing public private key pairs
		//
		//Maps for receiving messages from ips
		// - primarily used for determining if DOS attacks and blacklisting
		//
		receivedip = new ConcurrentHashMap<String, String>(16,(float) 0.75,30);
		blacklistip = new ConcurrentHashMap<String, String>();
		whitelistip = new ConcurrentHashMap<String, String>();
		
		whitelistip.put(apiIP, "0");
		
		//
		// check to see if there are serial files to load
		//
		try
		{
			File f = new File("userregistry.ser");
			if(f.exists() && !f.isDirectory()) 
			{
				FileInputStream fis = new FileInputStream("userregistry.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				userregistry = (ConcurrentHashMap) ois.readObject();
				ois.close();
			}
			f = new File("userregistrydid.ser");
			if(f.exists() && !f.isDirectory()) 
			{
				FileInputStream fis = new FileInputStream("userregistrydid.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				userregistrydid = (ConcurrentHashMap) ois.readObject();
				ois.close();
			}
			f = new File("tokendevice.ser");
			if(f.exists() && !f.isDirectory()) 
			{
				FileInputStream fis = new FileInputStream("tokendevice.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				tokendevice = (ConcurrentHashMap) ois.readObject();
				ois.close();
			}
			f = new File("userpublickey.ser");
			if(f.exists() && !f.isDirectory()) 
			{
				FileInputStream fis = new FileInputStream("userpublickey.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				userpublickey = (ConcurrentHashMap) ois.readObject();
				ois.close();
			}
			f = new File("messagequednr.ser");
			if(f.exists() && !f.isDirectory()) 
			{
				FileInputStream fis = new FileInputStream("messagequednr.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				messageque = (ConcurrentHashMap) ois.readObject();
				ois.close();
			}
		}
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}	 
		
        GUI.outputText("userregistry is "+userregistry.size());
        GUI.outputText("userregistrydid is "+userregistrydid.size());
        GUI.outputText("tokendevice is "+tokendevice.size());
        GUI.outputText("userpublickey is "+userpublickey.size());
        GUI.outputText("messageque is "+messageque.size());
       
		//Path fuserregistry = Paths.get("userregistry.txt");
		//String DATA_SEPARATOR = "||";
		//Files.write(fuserregistry, () -> userregistry.entrySet().stream()
		//	    .<CharSequence>map(e -> e.getKey() + DATA_SEPARATOR + e.getValue())
		//	    .iterator());
		
		//
		// create new thread objects to listen for messages
		//
		SocketListener[] threads = new SocketListener[maxnumIn];
		for(int i = 0 ; i < maxnumIn; i++)
		{     
		  	threads[i] = new SocketListener(i,socketIn[i],socketOut[i],socketIn[i].getLocalPort());
		  	threads[i].start();
		}   
		GUI.outputText("Listener threads started..."+Integer.toString(maxnumIn));
		//
		//create the sockets and  thread object for inter-dnr communications - ie. dnr to dnr
		//
		//
		//setup the sockets for the DNR communications
		//
		socketInDnr = new DatagramSocket(dnrIn);
		receivePacketInDnr = new DatagramPacket(receiveDataInDnr, receiveDataInDnr.length);
		GUI.outputText("DNR Socket in started: " + dnrIn);
		socketOutDnr = new DatagramSocket(dnrOut);
		receivePacketOutDnr = new DatagramPacket(receiveDataOutDnr, receiveDataOutDnr.length);
		GUI.outputText("DNR Socket out started: " + dnrOut);
		
		DNRSocketListener dnrThread = new DNRSocketListener(socketInDnr,socketOutDnr,socketInDnr.getLocalPort());
		dnrThread.start();  
		GUI.outputText("DNR Listener thread started...");
		//
		//create new thread objects for keep alives
		//
		KeepAliveHandler kaThread = new KeepAliveHandler(socketOutDnr);
		kaThread.start();  
		KeepAliveProcessor kapThread = new KeepAliveProcessor();
		kapThread.start(); 
		

		//
		// create public=private key pairs for use in transmission encryption
		//
		GUI.outputText("Creating Keypairs...");
		for (int j=0; j < 10; ++j )
		{
			GenerateKeyPair.CreateKeyPair();
		}
		GUI.outputText("Keypairs created..."+Integer.toString(keypairs.size()));
		
		//
		// create new thread object for public private key pair generation
		//
		KeyPairHandler kpThread = new KeyPairHandler();
		kpThread.start();
		GUI.outputText("Keypairs Handler started...");
		
		//
		// check to see if this is the seed dnr, if not reach out to the seed dnr for sync etc
		//
		
		hasBeenValidated = true;
		dnrdomain.put(dnrSeed,Integer.toString((int) ((new Date().getTime())/1000)) );
		GUI.updateStatus("Active - Processing");
		if ( !dnrSeed.equals(InetAddress.getLocalHost().getHostAddress()) )
		{
			hasBeenValidated = false;
			GUI.updateStatus("Requesting authorization");
			byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
			// first send over the dnrIn port to open and allow incoming messages from DNR
			sendDataDnr = ("1000||").getBytes();
			DatagramPacket sendPacketDnrI = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrSeed), dnrIn);
			socketInDnr.send(sendPacketDnrI);
			// reset message and send validation request to DNR
			sendDataDnr = new byte[PhotinoDNR.udppacketsize];
			// request registration with dnr seed
			sendDataDnr = ("60|"+validationID+"|").getBytes();
			DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrSeed), dnrIn);
			socketOutDnr.send(sendPacketDnr);
		}
		
	}
}
