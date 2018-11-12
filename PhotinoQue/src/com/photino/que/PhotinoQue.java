package com.photino.que;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.security.PrivateKey;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public class PhotinoQue 
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
	public static Boolean messageReceived;
	public static int tokenexpiry;
	public static int messageexpiry;
	public static int messagequeexpiry;
	
	public static int maxnumIn;
	public static int maxnumOut;
	
	public static DatagramSocket[] socketIn;
	public static DatagramSocket[] socketOut;
	public static DatagramSocket socketInDnr;
	public static DatagramSocket socketOutDnr;
	
	public static int dnrIn;
	public static int dnrOut;
	
	public static int udppacketsize = 32768;
	
	public static ConcurrentHashMap<String,String> dnrdomain;// for list of active dnr
	public static ConcurrentHashMap<String,String> topicdomain;// for list of topics to ip
	public static ConcurrentHashMap<String,String> messageque;// for queuing messages for recipients where their gateway is offline
	public static ConcurrentHashMap<String,String> messages;// for storing undelivered messages
	public static ConcurrentHashMap<String,String> usertoken;// for storing usertag to token for remote notifications
	public static ConcurrentHashMap<String,String> receivedip;// for tracking ips that have been received - for frequency blacklist
	public static ConcurrentHashMap<String,String> whitelistip;// white listed ips
	public static ConcurrentHashMap<String,String> blacklistip;// black listed ips

	public static void main(String[] args) throws Exception
	{
		new PhotinoQue();

	}
	public PhotinoQue() throws Exception
	{
		//
		// get config properties file
		//
		File pf = new File( "que.properties");
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
				System.out.println(prop.getProperty("MESSAGE_EXPIRY"));
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
		messageReceived = false;
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
			if ( Integer.parseInt(prop.getProperty("API_PATH")) > 0 )
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
		System.out.println("API="+apiPath+" API IP "+apiIP);
		//
		// dnr seed
		//
		dnrSeed = "158.69.198.174";
		try 
		{
			if ( Integer.parseInt(prop.getProperty("DNR_SEED")) > 0 )
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
		messageexpiry = 30;
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
			if ( Integer.parseInt(prop.getProperty("MESSAGE_EXPIRY")) > 0 )
			{
				messageexpiry = Integer.parseInt(prop.getProperty("MESSAGE_EXPIRY"));
				if ( messageexpiry > 100 || messageexpiry < 1 ) messageexpiry = 30;
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
		int startIn = 45000;
		int startOut = startIn + 1000;
		maxnumIn = 25;
		maxnumOut = maxnumIn;
		
		dnrIn = 59100;
		dnrOut = dnrIn + 1;

		socketIn = new DatagramSocket[maxnumIn];
		socketOut = new DatagramSocket[maxnumOut];
		socketInDnr = new DatagramSocket();
		socketOutDnr = new DatagramSocket();

		DatagramPacket[] receivePacketIn = new DatagramPacket[maxnumIn];
		DatagramPacket[] receivePacketOut = new DatagramPacket[maxnumOut];
		DatagramPacket receivePacketInDnr;
		DatagramPacket receivePacketOutDnr;
		
		byte[][] receiveDataIn = new byte[maxnumIn][PhotinoQue.udppacketsize];
		byte[][] receiveDataOut = new byte[maxnumOut][PhotinoQue.udppacketsize];
		byte[] receiveDataInDnr = new byte[PhotinoQue.udppacketsize];
		byte[] receiveDataOutDnr = new byte[PhotinoQue.udppacketsize];
		
		//
		//setup the sockets
		//
		for(int i = 0 ; i < maxnumIn; i++)
		{
			socketIn[i] = new DatagramSocket(startIn+i);
			receivePacketIn[i] = new DatagramPacket(receiveDataIn[i], receiveDataIn[i].length);
		}
		GUI.outputText("Socket in array started..."+Integer.toString(maxnumIn));
		for(int i = 0 ; i < maxnumOut; i++)
		{
			 socketOut[i] = new DatagramSocket(startOut+i);
			 receivePacketOut[i] = new DatagramPacket(receiveDataOut[i], receiveDataOut[i].length);
		}
		GUI.outputText("Socket out array started..."+Integer.toString(maxnumOut));
		
		GUI.updateIPPort(myIP,Integer.toString(startIn), Integer.toString(startIn + maxnumIn - 1),Integer.toString(startOut), Integer.toString(startOut + maxnumOut - 1));
		
		//
		// Initialize the hash maps
		//
		dnrdomain = new ConcurrentHashMap<String,String>();// for list of active dnr
		topicdomain = new ConcurrentHashMap<String,String>();// for list of topics to ip
		messageque = new ConcurrentHashMap<String,String>();// for queuing messages for recipients where their gateway is offline
		messages = new ConcurrentHashMap<String, String>();// storing public private key pairs
		usertoken = new ConcurrentHashMap<String, String>();// for usertag to token for remote notifications
		//
		//Maps for receiving messages from ips
		// - primarily used for determining if DOS attacks and blacklisting
		//
		receivedip = new ConcurrentHashMap<String, String>(16,(float) 0.75,30);
		whitelistip = new ConcurrentHashMap<String, String>();
		blacklistip = new ConcurrentHashMap<String, String>();
		
		whitelistip.put(apiIP, "0");
		
		//
		// check to see if there are serial files to load
		//
		try
		{
			File f = new File("messagequeque.ser");
			if(f.exists() && !f.isDirectory()) 
			{
				FileInputStream fis = new FileInputStream("messagequeque.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				messageque = (ConcurrentHashMap) ois.readObject();
				ois.close();
			}
			f = new File("messagequemessage.ser");
			if(f.exists() && !f.isDirectory()) 
			{
				FileInputStream fis = new FileInputStream("messagequemessage.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				messages = (ConcurrentHashMap) ois.readObject();
				ois.close();
			}
			f = new File("usertoken.ser");
			if(f.exists() && !f.isDirectory()) 
			{
				FileInputStream fis = new FileInputStream("usertoken.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				usertoken = (ConcurrentHashMap) ois.readObject();
				ois.close();
			}
		}
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}	 
		
		GUI.outputText("Messages in que ..."+messageque.size());
		GUI.outputText("Messages for usertags in backup ..."+messages.size());
		GUI.outputText("User Tokens ..."+usertoken.size());
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
		
		dnrdomain.put(dnrSeed, "1");
		DNRSocketListener dnrThread = new DNRSocketListener(socketInDnr,socketOutDnr,socketInDnr.getLocalPort());
		dnrThread.start();  
		GUI.outputText("DNR Listener thread started...");
		//
		//create new thread objects for keep alives
		//
		KeepAliveHandler kaThread = new KeepAliveHandler(socketOutDnr);
		kaThread.start();  
		
		
		//
		//send validation request to the DNR
		//
		hasBeenValidated = false;
		Random rand = new Random(); 
		byte[] sendDataDnr = new byte[PhotinoQue.udppacketsize];
		int dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
		// first send over the dnrIn port to open and allow incoming messages from DNR
		sendDataDnr = ("1000||").getBytes();
		DatagramPacket sendPacketDnrI = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrSeed), dnrPort);
		socketInDnr.send(sendPacketDnrI);
		// reset message and send validation request to DNR
		sendDataDnr = new byte[PhotinoQue.udppacketsize];
		sendDataDnr = ("92|"+validationID+"|").getBytes();
		DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrSeed), dnrPort);
		socketOutDnr.send(sendPacketDnr);

	}
}
