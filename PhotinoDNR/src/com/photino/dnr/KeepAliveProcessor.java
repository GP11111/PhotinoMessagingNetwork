package com.photino.dnr;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
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
//KeepAliveProcessor class
//
class KeepAliveProcessor extends Thread 
{
	public int seedupdate = 0;
	public int tokenupdate = 0;
	public int messagequeupdate = 0;
	
	// Constructor
	public KeepAliveProcessor() 
	{

	//@Override
	//public void run() 
	//{
		
		Timer timer = new Timer();
		int timeout = 120;// keep alive timeout in seconds
		
		timer.scheduleAtFixedRate(new TimerTask() {
		  @Override
		  public void run() {
		    CheckIPAlive(timeout);
		  }
		}, timeout*1000, timeout*1000);
	}

	public void CheckIPAlive(int timeout)
	{
		Boolean removed = false;
		Random rand = new Random(); 
		int secs = (int) ((new Date().getTime())/1000);
		int minimumka = secs - timeout;
		
		Map<String,String> tdnrdomain = new HashMap<String, String>();
		tdnrdomain.putAll(PhotinoDNR.dnrdomain);
		Map<String,String> tquedomain = new HashMap<String, String>();
		tquedomain.putAll(PhotinoDNR.quedomain);
		Map<String,String> tactiveip = new HashMap<String, String>();
		tactiveip.putAll(PhotinoDNR.activeip);
		
		Set s = tdnrdomain.entrySet();
		Iterator it = s.iterator();
		String ip = "";
		int lastka = 0;
		String tlist = "";
		String[] topiclist;
		int numoftopics = 0;
		String topicstosend = "";
		
		GUI.outputText("Running Timeout");
		//
		// check to see if dnrdomain ip list is stale, if so remove and send update to gateways
		//
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    ip = (String)m.getKey();
		    lastka = Integer.parseInt((String) m.getValue());

			if ( !ip.equals(PhotinoDNR.myIP) )
			{
GUI.outputText("Looking at removing "+ip);
				removed = false;
GUI.outputText(Integer.toString(lastka)+" "+Integer.toString(minimumka));
				if ( minimumka > lastka )
				{
					// remove from dnrdomain
					PhotinoDNR.dnrdomain.remove(ip);
					PhotinoDNR.whitelistip.remove(ip);
					removed = true;
				}

				if ( removed )
				{
					//
					// other dnrs will remove this dnr via the ka processing
					// now send update to gateways and ques
					//
		    		int recipientPort = 53000;
		    		int outSocket = 0;
		    		Set ss = tactiveip.entrySet();
		    		Iterator its = ss.iterator();
		    		if ( tactiveip.size() > 0 )
		    		{
		    			String sendip = "";
		    			byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		    			while(its.hasNext())
		    			{
		    				Map.Entry ms =(Map.Entry)its.next();
		    				sendip = (String)ms.getKey();
		    				sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		    				sendDataDnr = ("76|1|"+ip).getBytes();
		    				outSocket = rand.nextInt(24);
		    				try 
		    				{
		    					DatagramPacket sendPacket = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(sendip), recipientPort);
								PhotinoDNR.socketOut[outSocket].send(sendPacket);
							} 
		    				catch (IOException e) {}
		    			}
		    		}
		    		//
		    		// and send to queues
		    		//
		    		recipientPort = 59100;
		    		ss = tquedomain.entrySet();
		    		its = ss.iterator();
		    		if ( tquedomain.size() > 0 )
		    		{
		    			String sendip = "";
		    			byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		    			while(its.hasNext())
		    			{
		    				Map.Entry ms =(Map.Entry)its.next();
		    				sendip = (String)ms.getKey();
		    				sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		    				sendDataDnr = ("76|1|"+ip).getBytes();
		    				outSocket = rand.nextInt(24);
		    				try 
		    				{
		    					DatagramPacket sendPacket = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(sendip), recipientPort);
								PhotinoDNR.socketOut[outSocket].send(sendPacket);
							} 
		    				catch (IOException e) {}
		    			}
		    		}
				}
			}
		}
		    
		//
		// check to see if quedomain list is stale for any que and remove
		//
		s = tquedomain.entrySet();
		it = s.iterator();
		while(it.hasNext())
		{
		    // key=value separator this by Map.Entry to get key and value
		    Map.Entry m =(Map.Entry)it.next();

		    ip = (String)m.getKey();
		    lastka = Integer.parseInt((String) m.getValue());

			removed = false;
			if ( minimumka > lastka )
			{
				// remove from quedomain
				PhotinoDNR.quedomain.remove(ip);
				removed = true;
			}
			if ( removed )
			{
				//
				// other dnrs will remove this que via the ka processing, now send update to gateways
				//
	    		int recipientPort = 53000;
	    		int outSocket = 0;
	    		Set ss = tactiveip.entrySet();
	    		Iterator its = ss.iterator();
	    		if ( tactiveip.size() > 0 )
	    		{
	    			String sendip = "";
	    			byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
	    			while(its.hasNext())
	    			{
	    				Map.Entry ms =(Map.Entry)its.next();
	    				sendip = (String)ms.getKey();
	    				sendDataDnr = new byte[PhotinoDNR.udppacketsize];
	    				sendDataDnr = ("78|1|"+ip).getBytes();
	    				outSocket = rand.nextInt(24);
	    				try 
	    				{
	    					DatagramPacket sendPacket = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(sendip), recipientPort);
							PhotinoDNR.socketOut[outSocket].send(sendPacket);
						} 
	    				catch (IOException e) {}
	    			}
	    		}
			}
		}
		
		//
		// check to see if activeip list is stale for any gateway and remove topics and update
		//

		s = tactiveip.entrySet();
		it = s.iterator();
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    ip = (String)m.getKey();
		    lastka = Integer.parseInt((String)m.getValue());
GUI.outputText("Gateway "+Integer.toString(lastka)+" "+Integer.toString(minimumka));
		    if ( minimumka > lastka )
		    {
		    	// remove from domaintopic, topicdomain and activeip
		    	numoftopics = 0;
		    	topicstosend = "";
		    	if ( PhotinoDNR.domaintopic.containsKey(ip) )
		    	{
		    		tlist = (String) PhotinoDNR.domaintopic.get(ip);
		    		topiclist = tlist.split(",");
		    		if ( topiclist.length >= 1 )
		    		{
		    			for ( int i=0; i < topiclist.length; i++ )
		    			{
		    				PhotinoDNR.topicdomain.remove(topiclist[i]);
		    			}
		    		}
		    		PhotinoDNR.domaintopic.remove(ip);
		    		
		    		numoftopics = topiclist.length;
		    		
		    		for ( int i=0; i < topiclist.length; i++ )
		    		{
		    			if ( topicstosend.equals("") ) topicstosend = topiclist[i] + ":" + ip;
		    			else topicstosend = topicstosend + "," + topiclist[i] + ":" + ip;
		    		}
		    	}
		    	PhotinoDNR.activeip.remove(ip);
		    	if ( PhotinoDNR.activeiprouter.containsKey(ip) ) PhotinoDNR.activeiprouter.remove(ip);
	    		
		    	GUI.outputText("Removed topics for "+ip+": "+tlist);
		    	
		    	//
		    	// now update the other dnrs, ques and gateways
		    	//
		    	try
		    	{
		    		byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
  			
		    		//
		    		// update dnrs
		    		//
		    		if ( PhotinoDNR.dnrdomain.size() > 1 )
		    		{
		    			
		    			Set sdnr = tdnrdomain.entrySet();
		    			Iterator itdnr = sdnr.iterator();
		    			
		    			while(itdnr.hasNext())
		    			{
		    				m =(Map.Entry)itdnr.next();
		    				ip = (String)m.getKey();
				GUI.outputText("Sending update to dnr at "+ip);
							if ( !ip.equals(PhotinoDNR.myIP) )
							{
								sendDataDnr = new byte[PhotinoDNR.udppacketsize];
								sendDataDnr = ("83|"+numoftopics+"|"+topicstosend).getBytes();
								DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), PhotinoDNR.dnrIn);
								PhotinoDNR.socketOutDnr.send(sendPacketDnr);
							}
		    			}
		    		}

		    		//
		    		// update the ques 
		    		//
		    		int recipientPort = 59100;
		    		int outSocket = 0;
		    		if ( PhotinoDNR.quedomain.size() > 0 )
		    		{
		    			Set sq = tquedomain.entrySet();
		    			Iterator itq = sq.iterator();

		    			while(itq.hasNext())
		    			{
		    				m =(Map.Entry)itq.next();
		    				ip = (String)m.getKey();
		    				sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		    				sendDataDnr = ("83|"+numoftopics+"|"+topicstosend).getBytes();
		    				//recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
		    				outSocket = rand.nextInt(24);
		    				DatagramPacket sendPacket = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), recipientPort);
		    				PhotinoDNR.socketOut[outSocket].send(sendPacket);
		    			}
		    		}
		    		
		    		//
		    		// update the gateways 
		    		//
		    		recipientPort = 53000;
		    		outSocket = 0;
		    		if ( PhotinoDNR.activeip.size() > 0 )
		    		{
		    			Set sgw = tactiveip.entrySet();
		    			Iterator itgw = sgw.iterator();

		    			while(itgw.hasNext())
		    			{
		    				m =(Map.Entry)itgw.next();
		    				ip = (String)m.getKey();
		    				sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		    				sendDataDnr = ("83|"+numoftopics+"|"+topicstosend).getBytes();
		    				//recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
		    				outSocket = rand.nextInt(24);
		    				DatagramPacket sendPacket = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), recipientPort);
		    				PhotinoDNR.socketOut[outSocket].send(sendPacket);
		    			}
		    		}
		    	}
		    	catch (IOException e) {}
		    }
		}
		
		tdnrdomain.clear();
		tquedomain.clear();
		tactiveip.clear();
		
		//
		// check if time to run seed update
		//
		seedupdate = seedupdate + 1;
		if ( seedupdate >= 2 )
		{
			seedupdate = 0;
			SendDNRSeed();
		}
		
		//
		// check if time to run the token expire
		//
		tokenupdate = tokenupdate + 1;
		if ( tokenupdate >= 5 )
		{
			tokenupdate = 0;
			ExpireTokens();
		}
		
		//
		// check to see if time to run messageque maintenance
		//
		messagequeupdate = messagequeupdate + 1;
		if ( messagequeupdate >= 6 )
		{
			messagequeupdate = 0;
			SendMessagesFromQue();
		}
	}
	
	public void SendDNRSeed()
	{
		//
		// before sending the seed update, check to see if the current seed is in the dnrdomain set
		// if not, re-initialize this dnr
		//
		try 
		{
			URL url = new URL(PhotinoDNR.apiPath+"getSeed.php");
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			InputStream stream = connection.getInputStream();
			String result = new BufferedReader(new InputStreamReader(stream))
					  .lines().collect(Collectors.joining("\n"));
			stream.close();
			if ( validateIP(result) )
			{
				if ( !PhotinoDNR.dnrdomain.containsKey(result) && PhotinoDNR.dnrdomain.size() == 1 && !result.equals(PhotinoDNR.myIP) )
				{
					//
					// reinitialize this dnr
					//
					byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
					// reset message and send validation request to DNR
					sendDataDnr = new byte[PhotinoDNR.udppacketsize];
					// request registration with dnr seed
					sendDataDnr = ("60|"+PhotinoDNR.validationID+"|").getBytes();
					DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(result), PhotinoDNR.dnrIn);
					PhotinoDNR.socketOutDnr.send(sendPacketDnr);
				}
				else
				{
					try 
					{
						String urlSeed = PhotinoDNR.apiPath + "saveSeed.php";
						String param = "{\"validationID\":\""+PhotinoDNR.validationID+"\", \"type\":\""+"D"+"\", \"dnrseed\":\""+PhotinoDNR.myIP+"\"}";
						String charset = "UTF-8"; 
						URLConnection connectionSeed = new URL(urlSeed).openConnection();
						connectionSeed.setDoOutput(true);// set to post
						connectionSeed.setRequestProperty("Accept-Charset", charset);
						connectionSeed.setRequestProperty("Content-Type", "application/json;charset=" + charset);
						OutputStream output = connectionSeed.getOutputStream();
						output.write(param.getBytes(charset));
						InputStream response = connectionSeed.getInputStream();
						byte[] respb = new byte[PhotinoDNR.udppacketsize];
						response.read(respb);
						result = new String(respb);
						result = result.replaceAll("\u0000.*", "");
						
					} 
					catch (IOException e) {e.printStackTrace();}
				}
			}
		} 
		catch (IOException e) {e.printStackTrace();}

	}
	
	public static boolean validateIP(final String ip) 
	{
	    String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	    return ip.matches(PATTERN);
	}
	
	public void ExpireTokens()
	{
		int secs = (int) ((new Date().getTime())/1000);
		int minimumka = secs - (PhotinoDNR.tokenexpiry*86400);// tokenexpiry days ago
		
		Map<String,String> ttokendevice = new HashMap<String, String>();
		ttokendevice.putAll(PhotinoDNR.tokendevice);
		
		Set s = ttokendevice.entrySet();
		Iterator it = s.iterator();
		int createtime = 0;
		String token = "";
		String tlist = "";
		String[] tokenparameterlist;
		int numoftokensremoved = 0;
		
		GUI.outputText("Running Token Expiry...");
		//
		// check to see if the token is stale, if so remove
		//
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    token = (String)m.getKey();
		    tlist = (String) m.getValue();
		    tokenparameterlist = tlist.split(",");
		    createtime = Integer.parseInt((String) tokenparameterlist[0]);
		    
			if ( minimumka > createtime )
			{
				PhotinoDNR.tokendevice.remove(token);
				numoftokensremoved = numoftokensremoved + 1;
			}
		}
		if ( numoftokensremoved > 0 )
		{
			try 
			{
				FileOutputStream fos;
				fos = new FileOutputStream("tokendevice.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(PhotinoDNR.tokendevice);
				oos.close();
			} 
			catch (IOException e) {e.printStackTrace();}
		}
	}
	
	public void SendMessagesFromQue()
	{
		Random rand = new Random(); 
		byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		int secs = (int) ((new Date().getTime())/1000);
		int retention = secs - (PhotinoDNR.messagequeexpiry*86400);// messagequeexpiry days ago
		
		Map<String,String> tmessageque = new HashMap<String, String>();
		tmessageque.putAll(PhotinoDNR.messageque);
		
		Set s = tmessageque.entrySet();
		Iterator it = s.iterator();
		int createtime = 0;
		String messageid = "";
		String messagelist = "";
		String message = "";
		String mtype = "";
		String recipient = "";
		String recipientDomain = "";
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
				if ( PhotinoDNR.topicdomain.containsKey(recipientDomain) )
				{
					recipientDomain = (String) PhotinoDNR.topicdomain.get(recipientDomain);
					//
					// recipient domain is online so forward message
					//
		    		sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		    		sendDataDnr = (mtype + "|"  + recipient + "|" + messageid + "||" + message).getBytes();
		    		int recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
		    		int outSocket = rand.nextInt(24);
		    		try
		    		{
		    			DatagramPacket sendPacket = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(recipientDomain), recipientPort);
		    			PhotinoDNR.socketOut[outSocket].send(sendPacket);
		    			PhotinoDNR.messageque.remove(messageid);
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
				PhotinoDNR.messageque.remove(messageid);
				numofmessagesremoved = numofmessagesremoved + 1;
			}
		}
		
		tmessageque.clear();
		
		//
		// save the messageque
		//
		if ( PhotinoDNR.messageQueued || numofmessagesremoved > 0 )
		{
			try 
			{
				FileOutputStream fos;
				fos = new FileOutputStream("messagequednr.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(PhotinoDNR.messageque);
				oos.close();
			} 
			catch (IOException e) {e.printStackTrace();}
			PhotinoDNR.messageQueued = false;
		}
	}
}

