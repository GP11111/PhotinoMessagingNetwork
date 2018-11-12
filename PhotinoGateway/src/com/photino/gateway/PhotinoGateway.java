package com.photino.gateway;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.photino.gateway.Shamir.SecretShare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.awt.event.ActionEvent;

public class PhotinoGateway 
{
	public static String version = "0.9.0.0";
	public static int versionNum = 900;
	
	public static Boolean hasBeenValidated = false;
	
	public static String validationID = "";
	public static String myTopicList = "";
	public static String myIP = "";
	public static String externalIP = "";
	public static String internalIP = "";
	public static Boolean messageQueued;
	public static Boolean messageReceived;
	public static int messagequeexpiry;
	public static int fragmentexpiry;
	public static int maxnumIn;
	public static int maxnumOut;
	public static int udppacketsize = 32768;
	public static DatagramSocket[] socketIn;
	public static DatagramSocket[] socketOut;
	public static DatagramSocket socketInDnr;
	public static DatagramSocket socketOutDnr;
	
	public static ConcurrentHashMap<String,String> dnrdomain;
	public static ConcurrentHashMap<String,String> quedomain;
	public static ConcurrentHashMap<String,String> topicdomain;
	public static ConcurrentHashMap<String,String> mytopics;
	public static ConcurrentHashMap<String,String> emessages;
	public static ConcurrentHashMap<String,String> messages;
	public static ConcurrentHashMap<String,String> keyfragments;
	public static ConcurrentHashMap<String,String> messageque;
	public static ConcurrentHashMap<String,String> receivedip;
	public static ConcurrentHashMap<String,String> blacklistip;
	public static ConcurrentHashMap<String,String> whitelistip;
	
	public static String apiPath;
	public static String apiIP;
	public static String dnrSeed;


	public static void main(String[] args) throws Exception 
	{
		new PhotinoGateway();
	}
	
public PhotinoGateway() throws Exception
{
	//
	// get config properties file
	//
	File f = new File( "gateway.properties");
	Properties prop = new Properties();
	InputStream is = null;
	try
	{		
		if ( f.exists() && !f.isDirectory() )
		{
			is = new FileInputStream(f); 
			prop.load(is);
			System.out.println(prop.getProperty("GATEWAY_START_PORT"));
			System.out.println(prop.getProperty("THREAD_POOL_COUNT"));
			System.out.println(prop.getProperty("ROOT_DIR"));
			System.out.println(prop.getProperty("VALIDATION_ID"));
			System.out.println(prop.getProperty("TOPICS"));
			System.out.println(prop.getProperty("DNR_SEED"));
			System.out.println(prop.getProperty("API_PATH"));
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
	// initialize
	//
	messageQueued = false;
	messageReceived = false;
	
	//
	// get my ip
	//
	myIP = InetAddress.getLocalHost().getHostAddress();
	internalIP = myIP;
	externalIP = "";
	URL whatismyip = new URL("http://checkip.amazonaws.com");
    BufferedReader in = null;
    try 
    {
        in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
        externalIP = in.readLine();
    } 
    finally 
    {
        if (in != null) 
        {
            try 
            {
                in.close();
            } 
            catch (IOException e) {e.printStackTrace();}
        }
    }
    if ( !externalIP.equals("") && !myIP.equals(externalIP) ) myIP = externalIP;
    System.out.println("Internal IP:"+internalIP+" externalIP"+externalIP+" myIP:"+myIP);
	//
	// create the gui
	//
	GUI gui = new GUI();
	GUI.updateVersion(version);
//
//udp sockets
//
Random rand = new Random(); 
int startIn = 33000;
try 
{
	if ( Integer.parseInt(prop.getProperty("GATEWAY_START_PORT")) > 0 )
	{
		startIn = Integer.parseInt(prop.getProperty("GATEWAY_START_PORT"));
	}
}
catch (Exception ex) {}

int startOut = startIn + 1000;

maxnumIn = 10;

try
{
	if ( Integer.parseInt(prop.getProperty("THREAD_POOL_COUNT")) > 0 )
	{
		maxnumIn = Integer.parseInt(prop.getProperty("THREAD_POOL_COUNT"));
	}
}
catch (Exception ex) {}

validationID = "eB43kje343k3k4kekdkd";
myTopicList = "WINE.COM,AMAZON1";
dnrSeed = "158.69.198.174";
int dnrPort = 55000;

//
// this gateway validation id
//
try
{
	if ( !prop.getProperty("VALIDATION_ID").equals("") )
	{
		validationID = prop.getProperty("VALIDATION_ID");
	}
}
catch (Exception ex) {}
if ( !externalIP.equals(internalIP) ) validationID = validationID + "--R";// this node is behind a router/firewall

//
// topics to listen for
//
try
{
	if ( !prop.getProperty("TOPICS").equals("") )
	{
		myTopicList = prop.getProperty("TOPICS");
	}
}
catch (Exception ex) {}
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
// DNR seed
//
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
// expiry timeouts in days
//
messagequeexpiry = 30;
try 
{
	if ( Integer.parseInt(prop.getProperty("MESSAGEQUE_EXPIRY")) > 0 )
	{
		messagequeexpiry = Integer.parseInt(prop.getProperty("MESSAGEQUE_EXPIRY"));
		if ( messagequeexpiry > 100 || messagequeexpiry < 1 ) messagequeexpiry = 30;
	}
}
catch (Exception ex) {}

maxnumOut = maxnumIn;

int dnrIn = 53000;
int dnrOut = dnrIn + 1;

//DatagramSocket[] socketIn = new DatagramSocket[maxnumIn];
//DatagramSocket[] socketOut = new DatagramSocket[maxnumOut];
//DatagramSocket socketInDnr = new DatagramSocket();
//DatagramSocket socketOutDnr = new DatagramSocket();

socketIn = new DatagramSocket[maxnumIn];
socketOut = new DatagramSocket[maxnumOut];

DatagramPacket[] receivePacketIn = new DatagramPacket[maxnumIn];
DatagramPacket[] receivePacketOut = new DatagramPacket[maxnumOut];
DatagramPacket receivePacketInDnr;
DatagramPacket receivePacketOutDnr;

//byte[] receiveData = new byte[1024];
//byte[] sendData = new byte[1024];

byte[][] receiveDataIn = new byte[maxnumIn][PhotinoGateway.udppacketsize];
byte[][] receiveDataOut = new byte[maxnumOut][PhotinoGateway.udppacketsize];
byte[] receiveDataInDnr = new byte[PhotinoGateway.udppacketsize];
byte[] receiveDataOutDnr = new byte[PhotinoGateway.udppacketsize];

String[] sIn = new String[maxnumIn];
String[] sOut = new String[maxnumOut];

InetAddress[] IPAddressIn = new InetAddress[maxnumIn];
InetAddress[] IPAddressOut = new InetAddress[maxnumOut];
int[] portIn = new int[maxnumIn];
int[] portOut = new int[maxnumOut];

//
//setup the sockets
//
GUI.outputText("Setting up the sockets...");
for(int i = 0 ; i < maxnumIn; i++)
{
	socketIn[i] = new DatagramSocket(startIn+i);
	//receiveDataIn[i] = new byte[1024];
	receivePacketIn[i] = new DatagramPacket(receiveDataIn[i], receiveDataIn[i].length);
	//System.out.println("Socket in: " + startIn+i);
	//GUI.outputText("Socket in: " + startIn+i);
	//System.out.println("Socket " + socketIn[i].getLocalAddress());
}
for(int i = 0 ; i < maxnumOut; i++)
{
	 socketOut[i] = new DatagramSocket(startOut+i);
	 //receiveDataOut[i] = new byte[1024];
	 receivePacketOut[i] = new DatagramPacket(receiveDataOut[i], receiveDataOut[i].length);
	 //System.out.println("Socket out: " + startOut+i);
	 //GUI.outputText("Socket out: " + startOut+i);
}
GUI.outputText("Sockets created...");

GUI.updateIPPort(InetAddress.getLocalHost().getHostAddress(),Integer.toString(startIn), Integer.toString(startIn + maxnumIn - 1));

//
//setup the sockets for the DNR communications
//
socketInDnr = new DatagramSocket(dnrIn);
receivePacketInDnr = new DatagramPacket(receiveDataInDnr, receiveDataInDnr.length);
GUI.outputText("DNR Socket in: " + dnrIn);
socketOutDnr = new DatagramSocket(dnrOut);
receivePacketOutDnr = new DatagramPacket(receiveDataOutDnr, receiveDataOutDnr.length);
GUI.outputText("DNR Socket out: " + dnrOut);

//
//Maps and arrays to locally store message fragments and messages
//
emessages=new ConcurrentHashMap<String, String>();
messages=new ConcurrentHashMap<String, String>();
keyfragments=new ConcurrentHashMap<String, String>(16,(float) 0.75,30);
messageque=new ConcurrentHashMap<String, String>();
//
//Maps for DNR caching
//
dnrdomain=new ConcurrentHashMap<String, String>();
quedomain=new ConcurrentHashMap<String, String>();
topicdomain=new ConcurrentHashMap<String, String>();
mytopics=new ConcurrentHashMap<String, String>();
//
//Maps for receiving messages from ips
// - primarily used for determining if DOS attacks and blacklisting
//
receivedip = new ConcurrentHashMap<String, String>(16,(float) 0.75,30);
blacklistip = new ConcurrentHashMap<String, String>();
whitelistip = new ConcurrentHashMap<String, String>();

whitelistip.put(apiIP, "0");

//
// create a new thread object to listen for messages
//
SocketListener[] threads = new SocketListener[maxnumIn];
for(int i = 0 ; i < maxnumIn; i++)
{     
  	threads[i] = new SocketListener(i,socketIn[i],socketOut[i],socketIn[i].getLocalPort());
  	threads[i].start();
}  

//
//create new thread object to listen for DNR messages
//
dnrdomain.put(dnrSeed, "1");
DNRSocketListener dnrThread = new DNRSocketListener(socketInDnr,socketOutDnr,socketInDnr.getLocalPort());
dnrThread.start();  
//
//send validation request to the DNR
//
byte[] sendDataDnr = new byte[PhotinoGateway.udppacketsize];
dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
// first send over the dnrIn port to open and allow incoming messages from DNR
sendDataDnr = ("1000||").getBytes();
DatagramPacket sendPacketDnrI = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrSeed), dnrPort);
socketInDnr.send(sendPacketDnrI);
// reset message and send validation request to DNR
sendDataDnr = new byte[PhotinoGateway.udppacketsize];
sendDataDnr = ("90|"+validationID+"|"+myTopicList).getBytes();
DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrSeed), dnrPort);
socketOutDnr.send(sendPacketDnr);

//
//create new kaThread for keep alives to DNR
//
KeepAliveHandler kaThread = new KeepAliveHandler(socketOutDnr);
kaThread.start(); 

//
// if internalIP not equal to externaiIP send ka's over the inbound sockets
//
if ( !PhotinoGateway.internalIP.equals(PhotinoGateway.externalIP) )
{
	sendDataDnr = new byte[PhotinoGateway.udppacketsize];
	sendDataDnr = ("950||").getBytes();
	try
	{
		for ( int j=0; j < PhotinoGateway.maxnumIn; ++j )
		{
			sendPacketDnrI = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(PhotinoGateway.apiIP), 44051);
			PhotinoGateway.socketIn[j].send(sendPacketDnrI);
			System.out.println("Initializing ports: "+Integer.toString(j)+" Sending KA to API on port "+Integer.toString(44051));
		}
		sendPacketDnrI = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(PhotinoGateway.apiIP), 44051);
	}
	catch (Exception e ) {}
}

//
// check to see if there are serial files to load
//
try
{
	f = new File("messages.ser");
	if(f.exists() && !f.isDirectory()) 
	{
		FileInputStream fis = new FileInputStream("messages.ser");
		ObjectInputStream ois = new ObjectInputStream(fis);
		messages = (ConcurrentHashMap) ois.readObject();
		ois.close();
	}
	f = new File("messasgeque.ser");
	if(f.exists() && !f.isDirectory()) 
	{
		FileInputStream fis = new FileInputStream("messageque.ser");
		ObjectInputStream ois = new ObjectInputStream(fis);
		messageque = (ConcurrentHashMap) ois.readObject();
		ois.close();
	}
}
catch (IOException ex) 
{
	ex.printStackTrace();
}	 

GUI.outputText("messages is "+messages.size());
GUI.outputText("messageque is "+messageque.size());

}

}
