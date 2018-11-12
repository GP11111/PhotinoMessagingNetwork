package com.photino.gateway;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

//
//KeepAliveHandler class
//
class KeepAliveHandler extends Thread 
{
	final DatagramSocket socketOut;
	public int kainitialization = 0;
	public int writelocalfile = 0;
	public int sendmessageque = 0;

	public KeepAliveHandler(DatagramSocket sOut) 
	{
		this.socketOut = sOut;
	//}

	//@Override
	//public void run() 
	//{
		
		Timer timer = new Timer();
		int katime = 30;// keep alive interval in seconds
		int maxfrequency = katime * 10;// maximum threshold of hits from a single ip before blacklisting
	
		timer.scheduleAtFixedRate(new TimerTask() {
		  @Override
		  public void run() {
		    SendKeepAlive(maxfrequency);
		  }
		}, katime*1000, katime*1000);
	}
	
	public void SendKeepAlive(int maxfrequency)
	{

			Map<String,String> tdnrdomain = new HashMap<String, String>();
			tdnrdomain.putAll(PhotinoGateway.dnrdomain);
			Set s = tdnrdomain.entrySet();
			Iterator it = s.iterator();
			String ip = "";
			String seq = "";
			Random rand = new Random(); 
			int dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
			byte[] sendDataDnr = new byte[PhotinoGateway.udppacketsize];

			while(it.hasNext())
			{
				Map.Entry m =(Map.Entry)it.next();
				ip = (String)m.getKey();
				seq = (String)m.getValue();
		    
				// send KA
				try 
				{
					sendDataDnr = ("95|Keep Alive|KA").getBytes();
					DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), dnrPort);
					socketOut.send(sendPacketDnr);
					//PhotinoGateway.socketInDnr.send(sendPacketDnr);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
		    
				System.out.println("Sending KA to DNR "+ip);
				//System.out.println("TopicDomain size "+PhotinoGateway.topicdomain.size());
			}
			//
			// now check to see if time to send a ka for initialization check
			//
			//if ( PhotinoGateway.hasBeenValidated )
			//{
				kainitialization = kainitialization + 1;
				if ( kainitialization >= 20 )
				{
					kainitialization = 0;
					Object[] keys = tdnrdomain.keySet().toArray();// values().toArray();
					String randomDNR = (String) keys[rand.nextInt(keys.length)];
					dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
					// send KA initialization check
					try 
					{
						sendDataDnr = ("96|Keep Alive Initialization|KAINIT").getBytes();
						DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(randomDNR), dnrPort);
						PhotinoGateway.socketOutDnr.send(sendPacketDnr);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
					keys = null;
				}
			//}
				
			//
			// if internalIP not equal to externaiIP send ka's over the inbound sockets
			//
			if ( !PhotinoGateway.internalIP.equals(PhotinoGateway.externalIP) )
			{
				sendDataDnr = new byte[PhotinoGateway.udppacketsize];
				DatagramPacket sendPacketDnrI;
				Object[] keys = tdnrdomain.keySet().toArray();// values().toArray();
				String randomDNR = "";
				sendDataDnr = ("950||").getBytes();
				try
				{
					for ( int j=0; j < PhotinoGateway.maxnumIn; ++j )
					{
						dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
						dnrPort = 44051;
						randomDNR = (String) keys[rand.nextInt(keys.length)];
						randomDNR = PhotinoGateway.apiIP;
						sendPacketDnrI = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(randomDNR), dnrPort);
						PhotinoGateway.socketIn[j].send(sendPacketDnrI);
						System.out.println("port "+Integer.toString(j)+" Sending KA to API "+randomDNR+" on port "+Integer.toString(dnrPort));
					}
					sendPacketDnrI = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(PhotinoGateway.apiIP), 44051);
					PhotinoGateway.socketInDnr.send(sendPacketDnrI);
				}
				catch (Exception e ) {}
			}
			
			tdnrdomain.clear();
			
			//
			// check on receivedip address list for too high frequency and blacklist
			//
			Map<String,String> treceivedip = new HashMap<String, String>();
			treceivedip.putAll(PhotinoGateway.receivedip);
			Set sr = treceivedip.entrySet();
			Iterator itr = sr.iterator();
			int freq = 0;
		    while (itr.hasNext()) 
		    {   
				Map.Entry m =(Map.Entry)itr.next();
				ip = (String)m.getKey();
				freq = Integer.parseInt((String)m.getValue());
				if ( freq > maxfrequency )
				{
					//
					// blacklist ip
					//
					PhotinoGateway.blacklistip.put(ip,Integer.toString((int) ((new Date().getTime())/1000)) );
					//
					// remove from receivedip
					//
					PhotinoGateway.receivedip.remove(ip);
				}
				else
				{
					PhotinoGateway.receivedip.put(ip,"0");
				}
		    }
		    treceivedip.clear();
		    
		    //
		    // now check the blacklist and remove if older than 10 minutes = 600 seconds
		    //
		    int nowsec = (int)(new Date().getTime())/1000;
			Map<String,String> tblacklistip = new HashMap<String, String>();
			tblacklistip.putAll(PhotinoGateway.blacklistip);
			sr = tblacklistip.entrySet();
			itr = sr.iterator();
			int blsec = 0;
		    while (itr.hasNext()) 
		    {   
				Map.Entry m =(Map.Entry)itr.next();
				ip = (String)m.getKey();
				blsec = Integer.parseInt((String)m.getValue());
				if ( ( nowsec - blsec ) > 600 )
				{
					//
					// remove blacklist ip
					//
					PhotinoGateway.blacklistip.remove(ip);
				}
		    }
		    tblacklistip.clear(); 
		
		    
			if ( PhotinoGateway.hasBeenValidated )
			{
				writelocalfile = writelocalfile + 1;
				if ( writelocalfile >= 2 )
				{
					writelocalfile = 0;
					WriteLocalFiles();
				}
			}
			
			//
			// send from message que
			//
			sendmessageque = sendmessageque + 1;
			if ( sendmessageque >= 3 )
			{
				sendmessageque = 0;
				SendMessageFromQue();
			}
	}

	public void WriteLocalFiles()
	{
		// local messages and messages in que
			try 
			{
				FileOutputStream fos;
				ObjectOutputStream oos;
				if ( PhotinoGateway.messageReceived )
				{
					fos = new FileOutputStream("messages.ser");
					oos = new ObjectOutputStream(fos);
					oos.writeObject(PhotinoGateway.messages);
					oos.close();
					PhotinoGateway.messageReceived = false;
				}
				if ( PhotinoGateway.messageQueued )
				{
					fos = new FileOutputStream("messageque.ser");
					oos = new ObjectOutputStream(fos);
					oos.writeObject(PhotinoGateway.messageque);
					oos.close();
					PhotinoGateway.messageQueued = false;
				}
			} 
			catch (IOException e) {e.printStackTrace();}
	}
	
	public void SendMessageFromQue()
	{
		//
		// check to see if any messages are still in the que waiting to be sent to the destination gateway
		//
		Map<String,String> tmessageque = new HashMap<String, String>();
		tmessageque.putAll(PhotinoGateway.messageque);
		Set sr = tmessageque.entrySet();
		Iterator itr = sr.iterator();
		Random generator = new Random();
		int dnrPort = 55000;
		String msgid = "";
		String msg = "";
		String recipient = "";
	    while (itr.hasNext()) 
	    {   
			Map.Entry m =(Map.Entry)itr.next();
			msgid = (String)m.getKey();
			msg = (String)m.getValue();
			
			dnrPort = generator.nextInt((55024 - 55000) + 1) + 55000;
			//
			// get dnr ip from dnrdomain map
			//
			Object[] values = PhotinoGateway.dnrdomain.keySet().toArray();// values().toArray();
			String dnrIP = (String) values[generator.nextInt(values.length)];
			values = null;
			
			recipient = msg.substring(0,msg.indexOf("|") ); 
			
			try
			{
				byte[] sendDataDnr = new byte[PhotinoGateway.udppacketsize];
				sendDataDnr = ("84|"+recipient+"|"+msgid).getBytes();
				DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrIP), dnrPort);
				PhotinoGateway.socketOutDnr.send(sendPacketDnr);
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
	    }
	    tmessageque.clear();
	}
}


