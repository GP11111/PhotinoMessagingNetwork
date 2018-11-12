package com.photino.dnr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
	
	// Constructor
	public KeepAliveHandler(DatagramSocket socketOut) 
	{
	
		this.socketOut = socketOut;

	//@Override
	//public void run() 
	//{
		
		Timer katimer = new Timer();

		int katime = 30;// keep alive send interval in seconds
		int maxfrequency = katime * 10;// maximum threshold of hits from a single ip before blacklisting
		
		
		//katimer.scheduleAtFixedRate(new TimerTask() {
		//	  @Override
		//	  public void run() {
		//	    SendKeepAlive();
		//	  }
		//	}, katime*1000, katime*1000);
		
		katimer.schedule(new TimerTask() {
			  @Override
			  public void run() {
			    SendKeepAlive(maxfrequency);
			  }
			}, katime*1000, katime*1000);
	}
	public void SendKeepAlive(int maxfrequency)
	{
		if ( PhotinoDNR.hasBeenValidated )
		{
			GUI.updateStats(Integer.toString(PhotinoDNR.dnrdomain.size()), Integer.toString(PhotinoDNR.activeip.size()),Integer.toString(PhotinoDNR.topicdomain.size()),Integer.toString(PhotinoDNR.quedomain.size()));
			Map<String,String> tdnrdomain = new HashMap<String, String>();
			tdnrdomain.putAll(PhotinoDNR.dnrdomain);
			Set s = tdnrdomain.entrySet();
			Iterator it = s.iterator();
			String ip = "";
			String seq = "";
			Random rand = new Random(); 
			int dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
			byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
			
			int secs = (int) ((new Date().getTime())/1000);
			PhotinoDNR.dnrdomain.put(PhotinoDNR.myIP,Integer.toString(secs));
			
GUI.outputText("Running KA: "+PhotinoDNR.myIP+" dnrdomain len="+Integer.toString(tdnrdomain.size())+" "+Integer.toString(PhotinoDNR.activeip.size()));
			while(it.hasNext())
			{
				Map.Entry m =(Map.Entry)it.next();
				ip = (String)m.getKey();
				seq = (String)m.getValue();
		    
				// send KA
				try 
				{
					if ( !ip.equals(PhotinoDNR.myIP) )
					{
						sendDataDnr = ("95|Keep Alive|KA").getBytes();
						DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), PhotinoDNR.dnrIn);
						socketOut.send(sendPacketDnr);
						GUI.outputText("Sending KA to DNR "+ip);
					}
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
		 
			}
			tdnrdomain.clear();
			
			//
			// check on receivedip address list for too high frequency and blacklist
			//
			Map<String,String> treceivedip = new HashMap<String, String>();
			treceivedip.putAll(PhotinoDNR.receivedip);
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
					PhotinoDNR.blacklistip.put(ip,Integer.toString((int) ((new Date().getTime())/1000)) );
					GUI.outputText("Blacklisted IP: "+ip);
					//
					// remove from receivedip
					//
					PhotinoDNR.receivedip.remove(ip);
				}
				else
				{
					PhotinoDNR.receivedip.put(ip,"0");
				}
		    }
		    treceivedip.clear();
		   
		    //
		    // now check the blacklist and remove if older than 10 minutes = 600 seconds
		    //
		    int nowsec = (int)(new Date().getTime())/1000;
			Map<String,String> tblacklistip = new HashMap<String, String>();
			tblacklistip.putAll(PhotinoDNR.blacklistip);
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
					PhotinoDNR.blacklistip.remove(ip);
					GUI.outputText("Removed from Blacklist IP: "+ip);
				}
		    }
		    tblacklistip.clear();
		    
		    GUI.outputText("Blacklisted IPs: "+Integer.toString(PhotinoDNR.blacklistip.size()));
		}
	}
}

