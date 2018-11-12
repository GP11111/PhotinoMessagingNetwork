package com.photino.que;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


//
//KeepAliveHandler class
//
class KeepAliveHandler extends Thread 
{
	final DatagramSocket socketOut;
	public int kainitialization = 0;
	public int writelocalfile = 0;
	public int sendmessageque = 0;
	public int cleanmessages = 0;
	public int cleantokens = 0;

	public KeepAliveHandler(DatagramSocket sOut) 
	{
		this.socketOut = sOut;
		
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
			GUI.updateStats(Integer.toString(PhotinoQue.dnrdomain.size()), Integer.toString(PhotinoQue.messageque.size()),Integer.toString(PhotinoQue.messages.size()),Integer.toString(PhotinoQue.topicdomain.size()));
			Map<String,String> tdnrdomain = new HashMap<String, String>();
			tdnrdomain.putAll(PhotinoQue.dnrdomain);
			Set s = tdnrdomain.entrySet();
			Iterator it = s.iterator();
			String ip = "";
			String seq = "";
			Random rand = new Random(); 
			int dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
			byte[] sendDataDnr = new byte[PhotinoQue.udppacketsize];

			while(it.hasNext())
			{
				Map.Entry m =(Map.Entry)it.next();
				ip = (String)m.getKey();
				seq = (String)m.getValue();
		    
				// send KA
				try 
				{
					sendDataDnr = ("97|Keep Alive|KA").getBytes();
					DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), dnrPort);
					socketOut.send(sendPacketDnr);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
		    
				System.out.println("Sending KA to DNR "+ip);
			}
			//
			// now check to see if time to send a ka for initialization check
			//
			if ( PhotinoQue.hasBeenValidated )
			{
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
						sendDataDnr = ("98|Keep Alive Initialization|KAINIT").getBytes();
						DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(randomDNR), dnrPort);
						PhotinoQue.socketOutDnr.send(sendPacketDnr);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
					keys = null;
				}
			}
			
			tdnrdomain.clear();
			
			//
			// check on receivedip address list for too high frequency and blacklist
			//
			Map<String,String> treceivedip = new HashMap<String, String>();
			treceivedip.putAll(PhotinoQue.receivedip);
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
					PhotinoQue.blacklistip.put(ip,Integer.toString((int) ((new Date().getTime())/1000)) );
					//
					// remove from receivedip
					//
					PhotinoQue.receivedip.remove(ip);
				}
				else
				{
					PhotinoQue.receivedip.put(ip,"0");
				}
		    }
		    treceivedip.clear();
		    
		    //
		    // now check the blacklist and remove if older than 10 minutes = 600 seconds
		    //
		    int nowsec = (int)(new Date().getTime())/1000;
			Map<String,String> tblacklistip = new HashMap<String, String>();
			tblacklistip.putAll(PhotinoQue.blacklistip);
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
					PhotinoQue.blacklistip.remove(ip);
				}
		    }
		    tblacklistip.clear(); 
		
		    
			if ( PhotinoQue.hasBeenValidated )
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
				SendMessagesFromQue();
			}
			
			//
			// clean messages 
			//
			cleanmessages = cleanmessages + 1;
			if ( cleanmessages >= 5 )
			{
				cleanmessages = 0;
				CleanMessages();
			}
			
			//
			// clean tokens
			//
			cleantokens = cleantokens + 1;
			if ( cleantokens >= 8 )
			{
				cleantokens = 0;
				CleanUserTokens();
			}
	}

	public void WriteLocalFiles()
	{
		// local messages and messages in que
			try 
			{
				if ( PhotinoQue.messageReceived )
				{
					FileOutputStream fos;
					fos = new FileOutputStream("messagequemessage.ser");
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(PhotinoQue.messages);
					oos.close();
					PhotinoQue.messageReceived = false;
				}
				//fos = new FileOutputStream("messagequeque.ser");
				//oos = new ObjectOutputStream(fos);
				//oos.writeObject(PhotinoQue.messageque);
				//oos.close();
			} 
			catch (IOException e) {e.printStackTrace();}
	}
	
	public void SendMessagesFromQue()
	{
		Random rand = new Random(); 
		byte[] sendDataDnr = new byte[PhotinoQue.udppacketsize];
		int secs = (int) ((new Date().getTime())/1000);
		int retention = secs - (PhotinoQue.messagequeexpiry*86400);// messagequeexpiry days ago
		
		Map<String,String> tmessageque = new HashMap<String, String>();
		tmessageque.putAll(PhotinoQue.messageque);
		
		Set s = tmessageque.entrySet();
		Iterator it = s.iterator();
		int createtime = 0;
		String messageid = "";
		String messagelist = "";
		String message = "";
		String mtype = "";
		String recipient = "";
		String recipientDomain = "";
		String[] kalist = null;
		int kacounter = -1;
		String[] messageparameterlist;
		int numofmessagesremoved = 0;
		
		GUI.outputText("Running Message Que Management...");
		//
		// check to see if messages in que can be sent or if expired
		//
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    messageid = (String)m.getKey();
		    messagelist = (String) m.getValue();
		    messageparameterlist = messagelist.split("\\|");

		    //
		    // messagelist string is in the format of createtime | mtype | recipient |  message
		    //
		    createtime = Integer.parseInt((String) messageparameterlist[0]);
		    mtype = (String) messageparameterlist[1];
		    recipient = (String) messageparameterlist[2];
		    message = (String) messageparameterlist[3];
		    //
		    // determine if recipient gateway is online, if so send
		    //
			if ( recipient.indexOf('@') >= 0 )
			{
				recipientDomain = recipient.substring(recipient.indexOf("@")+1);
				recipientDomain = recipientDomain.toUpperCase();
				if ( PhotinoQue.topicdomain.containsKey(recipientDomain) )
				{
					recipientDomain = (String) PhotinoQue.topicdomain.get(recipientDomain);
					if ( !Arrays.stream(kalist).anyMatch(recipientDomain::equals) )
					{
						kacounter = kacounter + 1;
						kalist[kacounter] = recipientDomain;
						//
						// send ka request to gateway from myIP
						//
						String url = PhotinoQue.apiPath + "sendKARequest.php";
						String param = "{\"recipientip\":\""+recipientDomain+"\",\"iptoka\":\""+PhotinoQue.myIP+"\", \"pause\":\"T\"}";
						String charset = "UTF-8"; 
						try
						{
							URLConnection connection = new URL(url).openConnection();
							connection.setDoOutput(true);// set to post
							connection.setRequestProperty("Accept-Charset", charset);
							connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
							OutputStream output = connection.getOutputStream();
							output.write(param.getBytes(charset));
							InputStream response = connection.getInputStream();
							byte[] respb = new byte[PhotinoQue.udppacketsize];
							response.read(respb);
							String resp = new String(respb);
							resp = resp.replaceAll("\u0000.*", "");
						}
						catch (Exception e ) {}
					}
					//
					// recipient domain is online so forward message
					//
		    		sendDataDnr = new byte[PhotinoQue.udppacketsize];
		    		sendDataDnr = (mtype + "|"  + recipient + "|" + messageid + "||" + message).getBytes();
		    		int recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
		    		int outSocket = rand.nextInt(24);
		    		try
		    		{
		    			DatagramPacket sendPacket = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(recipientDomain), recipientPort);
		    			PhotinoQue.socketOut[outSocket].send(sendPacket);
		    			PhotinoQue.messageque.remove(messageid);
						numofmessagesremoved = numofmessagesremoved + 1;
						retention = createtime;
		    		}
		    		catch (IOException e) {e.printStackTrace();}
				}
			} 
			
			//
			// now check if retention time has expired == unable to send the message in the required time to send and que
			//
			if ( retention > createtime )
			{
				PhotinoQue.messageque.remove(messageid);
				numofmessagesremoved = numofmessagesremoved + 1;
			}
		}
		kalist = null;
		tmessageque.clear();
		
		//
		// save the messageque
		//
		if ( PhotinoQue.messageQueued || numofmessagesremoved > 0 )
		{
			try 
			{
				FileOutputStream fos;
				fos = new FileOutputStream("messagequeque.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(PhotinoQue.messageque);
				oos.close();
			} 
			catch (IOException e) {e.printStackTrace();}
			PhotinoQue.messageQueued = false;
		}
	}
	
	public void CleanMessages()
	{
		int secs = (int) ((new Date().getTime())/1000);
		int retention = secs - (PhotinoQue.messageexpiry*86400);// messageexpiry days ago
		Map<String,String> tmessages = new HashMap<String, String>();
		tmessages.putAll(PhotinoQue.messages);
		
		Set s = tmessages.entrySet();
		Iterator it = s.iterator();
		int createtime = 0;
		String messageid = "";
		String[] messagelist;
		String messages = "";
		String recipient = "";
		String message = "";
		int numofmessagesremoved = 0;
		Boolean removed = false;
		String[] messageparameterlist;
		
		GUI.outputText("Running Message Management...");
		
		//
		// message has format of key = recipient, value = createtime |  message ( || createtime|message || createtime|message ... )
		//
		// check to see if messages in que can be sent or if expired
		//
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    recipient = (String)m.getKey();
		    messages = (String) m.getValue();
		    messagelist = messages.split("\\|\\|");
		    numofmessagesremoved = 0;
		    try
		    {
		    	for (int i = 0; i < messagelist.length; ++ i )
		    	{
		    		messageparameterlist = messagelist[i].split("\\|");
		    		createtime = Integer.parseInt(messageparameterlist[0]);
		    		message = (String) messageparameterlist[1];
		    		//
		    		// now check if retention time has expired and remove
		    		//
		    		if ( retention > createtime )
		    		{
		    			PhotinoQue.messages.remove(recipient);
		    			numofmessagesremoved = numofmessagesremoved + 1;
		    			removed = true;
		    			messages = messages.substring(messages.indexOf("|")+1);
		    		}
		    		else
		    		{
		    			i = messagelist.length;
		    		}
		    	}
		    }
		    catch ( Exception e )
		    {
    			messages = "";
    			numofmessagesremoved = numofmessagesremoved + 1;
    			removed = true;
		    }
		    if ( numofmessagesremoved > 0 )
		    {
		    	if ( !messages.equals("") )
		    	{
		    		PhotinoQue.messages.put(recipient,messages);
		    	}
		    	else
		    	{
		    		PhotinoQue.messages.remove(recipient);
		    	}
		    }
		}
		tmessages.clear();
		//
		// save the message store
		//
		if ( removed )
		{
			try 
			{
				FileOutputStream fos;
				fos = new FileOutputStream("messagequemessage.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(PhotinoQue.messages);
				oos.close();
			} 
			catch (IOException e) {e.printStackTrace();}
		}
	}
	
	public void CleanUserTokens()
	{
		int secs = (int) ((new Date().getTime())/1000);
		int retention = secs - (PhotinoQue.tokenexpiry*86400);// tokenexpiry days ago
		Map<String,String> tusertoken = new HashMap<String, String>();
		tusertoken.putAll(PhotinoQue.usertoken);
		
		Set s = tusertoken.entrySet();
		Iterator it = s.iterator();
		int createtime = 0;
		String timetoken = "";
		String usertag = "";
		String token = "";
		int numoftokensremoved = 0;
		Boolean removed = false;
		String[] tokenparameterlist;
		
		GUI.outputText("Running User Token Management...");
		
		//
		// usertoken has format key = usertag, value = createtime,token
		//
		// check to see if tokens have expired
		//
		while(it.hasNext())
		{
		    // key=value separator this by Map.Entry to get key and value
		    Map.Entry m =(Map.Entry)it.next();

		    usertag = (String)m.getKey();
		    timetoken = (String) m.getValue();
		    tokenparameterlist = timetoken.split(",");
		    numoftokensremoved = 0;
		    createtime = Integer.parseInt(tokenparameterlist[0]);
		    token = (String) tokenparameterlist[1];
		    //
		    // now check if retention time has expired and remove
		    //
		    if ( retention > createtime )
		    {
		    		PhotinoQue.usertoken.remove(usertag);
		    		numoftokensremoved = numoftokensremoved + 1;
		    		removed = true;
		    }
		}
		tusertoken.clear();
		//
		// save the message store
		//
		if ( removed )
		{
			try 
			{
				FileOutputStream fos;
				fos = new FileOutputStream("usertoken.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(PhotinoQue.usertoken);
				oos.close();
			} 
			catch (IOException e) {e.printStackTrace();}
		}
	}
}



