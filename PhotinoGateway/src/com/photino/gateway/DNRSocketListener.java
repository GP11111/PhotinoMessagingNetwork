package com.photino.gateway;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;


//
//Socket listener class for DNR messaging
//
class DNRSocketListener extends Thread
{
	final DatagramSocket socketIn;
	final DatagramSocket socketOut;
	final int port;
	
	public DNRSocketListener(DatagramSocket socketIn, DatagramSocket socketOut, int port) 
	{
		this.socketIn = socketIn;
		this.socketOut = socketOut;
		this.port = port;
	}
		
		@Override
		public void run() 
		{
		DatagramPacket receivePacketIn;
		InetAddress IPAddressIn;
		int portIn = 0;
		byte[] receiveDataIn = new byte[PhotinoGateway.udppacketsize];
		String contentIn = "";
		System.out.println("DNRSocketListener thread started" );
		GUI.outputText("DNRSocketListener thread started");
		
		while (true) 
		{ 

		  try
		  {
			  receiveDataIn = new byte[PhotinoGateway.udppacketsize];
		      receivePacketIn = new DatagramPacket(receiveDataIn, receiveDataIn.length);

		      socketIn.receive(receivePacketIn);

		      contentIn = new String( receivePacketIn.getData());
		      contentIn = contentIn.replaceAll("\u0000.*", "");
		      System.out.println("DNR RECEIVED In1: " + contentIn);
		      IPAddressIn = receivePacketIn.getAddress();
		      portIn = receivePacketIn.getPort();
		      // check if in blacklist
		      if ( !PhotinoGateway.blacklistip.containsKey(IPAddressIn.getHostAddress()) )
		      {
		    	  //
		    	  // create a new thread object to handle the received content
		    	  //
		    	  Thread t = new DNRSocketHandler(contentIn,IPAddressIn,portIn,socketOut);
		    	  t.start();
		    	  GUI.outputText("DNRSocketPoll rec:" + contentIn);
		      }
		  	}
		  	catch (Exception e)
		  	{
		  		//s.close();
		  		GUI.outputText("catch");
		  		e.printStackTrace();
		  	}
			}
		}
			
}