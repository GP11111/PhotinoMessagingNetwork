package com.photino.gateway;

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


//
//SocketHandler class
//
class SocketHandler extends Thread 
{
	final int socketnum;
	final String content;
	final InetAddress IPAdr;
	final int port;
	final DatagramSocket sOut;

	public SocketHandler(int socketnum, String content, InetAddress IPAdr, int port, DatagramSocket sOut) 
	{
		this.socketnum = socketnum;
		this.content = content;
		this.IPAdr = IPAdr;
		this.port = port;
		this.sOut = sOut;
	}

	@Override
	public void run() 
	{
		int mtype;
		String mid;
		String recipient;
		String recipientDomain;
		int recipientPort;
		String received;
		String receivedmessage;
		String toreturn;
		byte[] sendData = new byte[PhotinoGateway.udppacketsize];
		DatagramPacket sendPacket;
		Random rand = new Random(); 
		
		Boolean add = false;
		String messagelist = "";
		
		//
		// content format: mtype | recipient | mid || messagereceived
		//
	
			try 
			{

				// receive the content and split
				received = content;
				mtype = Integer.parseInt(received.substring(0,received.indexOf("|") )); 
				received = received.substring(received.indexOf("|")+1);
				receivedmessage = received;
				recipient = received.substring(0,received.indexOf("|") ); 
				received = received.substring(received.indexOf("|")+1);
				mid = received.substring(0,received.indexOf("|") );
				//received = received.substring(received.indexOf("|")+1);
				received = received.substring(received.indexOf("||")+2);

				// resolve recipient to ip in the DNR
				recipient = recipient.toUpperCase();
				if ( recipient.indexOf('@') >= 0 )
				{
					recipientDomain = recipient.substring(recipient.indexOf("@")+1);
					recipientDomain = "158.69.208.56";
				}
				else
				{
					recipientDomain = recipient;
				}
				//recipientDomain = ResolveTopic(recipient,receivedmessage);
     
				// creating Date object
				Date date = new Date();
				
				// add-update receivedip map
				if ( !PhotinoGateway.whitelistip.containsKey(IPAdr.getHostAddress()) )
				{
					if ( PhotinoGateway.receivedip.containsKey(IPAdr.getHostAddress()) )
					{
						PhotinoGateway.receivedip.put(IPAdr.getHostAddress(), Integer.toString(Integer.parseInt(PhotinoGateway.receivedip.get(IPAdr.getHostAddress()))+1));
					}
					else
					{
						PhotinoGateway.receivedip.put(IPAdr.getHostAddress(), "1");
					}
				}
				
				//
				// determine action based on mtype
				//
				switch (mtype) 
				{
					case 0:
						// this is to exit thread without doing anything
						break;
					//
					// transmission encrypted message
					//
					case 1 :
						SendData("2|Message Sent", IPAdr, port);
						// store transmission encrypted message
						received = MessageHandler.AddTranmissionFragment("M",mid,received);
						if ( !received.equals("") )
						{
							recipient = received.substring(0,received.indexOf("|"));
							recipientDomain = ResolveTopic(recipient,received);
							recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
							if ( !recipientDomain.equals("") ) SendData("20|"+received, InetAddress.getByName(recipientDomain), recipientPort);
							GUI.outputText("Message Decrypted as:"+received+" \n");
						}
						break;
					case 2 :
					    GUI.outputText("Relay encrypted message has been received at assembly");
						break;
					case 3 :

						break;
					case 4 :

						break;
					case 5 :

						break;
					case 6 :

						break;
					// 10 - key fragments
					case 10:
						SendData("2|Fragment sent", IPAdr, port);
						// now relay message
						recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
						recipientDomain = ResolveTopic(recipient,receivedmessage);
						SendData("11|"+recipient+"|"+mid+"||"+received, InetAddress.getByName(recipientDomain), recipientPort);
					    GUI.outputText("Relay message has been relayed to "+recipient+" "+recipientDomain);
						break;
					case 11:
						recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
						SendData("12|a|"+mid+"||Relay Received at Destination", IPAdr, recipientPort);
					    GUI.outputText("Fragment message for "+recipient+" has been received at assembly ");
					    received = MessageHandler.AddTranmissionFragment("K",mid,received);
						if ( !received.equals("") )
						{
							recipient = received.substring(0,received.indexOf("|"));
							recipientDomain = ResolveTopic(recipient,received);
							recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
							GUI.outputText("Message Decrypted as:"+received);
							if ( !recipientDomain.equals("") ) SendData("20|"+received, InetAddress.getByName(recipientDomain), recipientPort);
						}
						break;
					case 12:
					    GUI.outputText("Relay fragment has been received at destination ");
						break;
					// transmission decrypted messages
					case 20:
						SendData("21|Message Received at destination",IPAdr,port);
						// store recipient encrypted message
						add = MessageHandler.AddMessage(recipient,received);
						SendToQue(recipient,mid+"||"+received,"BACKUP");
						GUI.outputText("Message for: "+recipient+" has been received - "+received);
						SendNotificationRequest(recipient);
						break;
					case 21:
					    GUI.outputText("Relay message has been received at destination ");
						break;
					// retrieve messages for a user
					case 50:
						messagelist = MessageHandler.GetMessages(recipient);
						SendData("51|"+messagelist,IPAdr,port);
						if ( Integer.parseInt(messagelist.substring(0, messagelist.indexOf("|"))) > 0 )
						{
							SendToQue(recipient,"","READ");
						}
						break;
					// retrieve messages for a user from the DNR, return to caller direct
					case 52:
						// received has the ip:port for return of the request
						if ( ValidateDNRDomain(IPAdr) )
						{
							messagelist = MessageHandler.GetMessages(recipient);	
							SendData("51|"+messagelist,received.substring(0,received.indexOf(":")),Integer.parseInt(received.substring(received.indexOf(":")+1)));
							if ( Integer.parseInt(messagelist.substring(0, messagelist.indexOf("|"))) > 0 )
							{
								SendToQue(recipient,"","READ");
							}
						}
						break;
					//
					// DNR
					//
					// for topic domains
					// 80 get current topic domains from DNR
					// 81 receive current topic domains from DNR
					// 82 receive from DNR topic domain refresh required
					//
					// 90 send validation request to DNR with validID token and topic list
					// 91 receive validation OK from DNR with list of valid topics
					// 94 send keep alive
					// 95 receive keep alive confirmation
					//
					case 80:
						
						break;
					case 82:
						
						break;
					case 90:
						
						break;
					case 91:
						
						break;
					case 95:
						
						break;
					default:

						break;
				}
				return;
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
	}
	public String ResolveTopic(String recipient, String receivedmessage)
	{
		String ret = "";
		//
		// resolve the ip from the domain via the DNR Map
		//
		if ( recipient.indexOf('@') >= 0 )
		{
			recipient = recipient.substring(recipient.indexOf("@")+1);
			if ( PhotinoGateway.topicdomain.containsKey(recipient) )
			{
				ret = (String) PhotinoGateway.topicdomain.get(recipient);
				System.out.println("Domain "+recipient+"  IP :"+ret);
			}
			else
			{
				//
				// add to message queue and send request to DNR for topic resolution
				// must go out over the DNR socket
				//
				Random generator = new Random();
				int dnrPort = generator.nextInt((55024 - 55000) + 1) + 55000;
				//
				// get dnr ip from dnrdomain map
				//
				Object[] values = PhotinoGateway.dnrdomain.keySet().toArray();// values().toArray();
				String dnrIP = (String) values[generator.nextInt(values.length)];
				values = null;
				
				int messageid = generator.nextInt(999999999);
				String messageidStr = Integer.toString(messageid);
				PhotinoGateway.messageque.put(messageidStr,receivedmessage);
				PhotinoGateway.messageQueued = true;
				try
				{
					byte[] sendDataDnr = new byte[PhotinoGateway.udppacketsize];
					sendDataDnr = ("84|@"+recipient+"|"+messageidStr).getBytes();
					DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrIP), dnrPort);
					PhotinoGateway.socketOutDnr.send(sendPacketDnr);
				}
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			ret = recipient;
		}

		return ret;
	}
	public Boolean ValidateDNRDomain(InetAddress IPAdr)
	{
		Boolean ret = false;
		String ip = IPAdr.getHostAddress();
		if ( PhotinoGateway.dnrdomain.containsKey(ip))
		{
			ret = true;
		}
		
		return ret;
	}
	public Boolean SendToQue(String recipient, String message, String stype)
	{
		Boolean ret = false;
		if ( PhotinoGateway.quedomain.size() > 0 )
		{
			Random rand = new Random();
			String qip = "";
			int outSocket = 0;
			int qPort = rand.nextInt((45024 - 45000) + 1) + 45000;
			byte[] sendDataQue = new byte[PhotinoGateway.udppacketsize];
			Map<String,String> tquedomain = new HashMap<String, String>();
			tquedomain.putAll(PhotinoGateway.quedomain);
			Set s = tquedomain.entrySet();
			Iterator it = s.iterator();
			if ( stype.equals("BACKUP") || stype.equals("READ") )
			{
				if ( stype.equals("BACKUP") )
				{
					// for BACKUP info1 has recipient, info2 has messageid || message
					sendDataQue = ("22|"+recipient+"|"+message).getBytes();
				}
				if ( stype.equals("READ") )
				{
					sendDataQue = ("24|"+recipient+"|"+message).getBytes();
				}
				while(it.hasNext())
				{
					Map.Entry m =(Map.Entry)it.next();
					qip = (String) m.getKey();
					outSocket = rand.nextInt(9);
					try 
					{
						DatagramPacket sendPacket = new DatagramPacket(sendDataQue, sendDataQue.length, InetAddress.getByName(qip), qPort);
						PhotinoGateway.socketOut[outSocket].send(sendPacket);
					} 
					catch (IOException e) {}
				}
			}
			tquedomain.clear();
		}
		ret = true;
		return ret;
	}
	public Boolean SendNotificationRequest(String recipient)
	{
		Boolean ret = false;
		if ( PhotinoGateway.quedomain.size() > 0 )
		{
			Random rand = new Random();
			int outSocket = 0;
			int qPort = rand.nextInt((45024 - 45000) + 1) + 45000;
			byte[] sendDataQue = new byte[PhotinoGateway.udppacketsize];
			Object[] values = PhotinoGateway.quedomain.keySet().toArray();// values().toArray();
			String queIP = (String) values[rand.nextInt(values.length)];
			outSocket = rand.nextInt(9);
			try 
			{
				sendDataQue = ("56|"+recipient+"|Unread Messages|You have unread messages.").getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendDataQue, sendDataQue.length, InetAddress.getByName(queIP), qPort);
				PhotinoGateway.socketOut[outSocket].send(sendPacket);
			} 
			catch (IOException e) {}
			values = null;
		}
		ret = true;
		return ret;
	}
	public Boolean SendData(String content, InetAddress IPAdr, int port)
	{
		Boolean ret = true;
		byte[] sendData = new byte[PhotinoGateway.udppacketsize];
		DatagramPacket sendPacket;
		sendData = (content).getBytes();
		sendPacket = new DatagramPacket(sendData, sendData.length, IPAdr, port);
		try 
		{
			sOut.send(sendPacket);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return ret;
	}
	
	public Boolean SendData(String content, String ip, int port)
	{
		Boolean ret = true;
		byte[] sendData = new byte[PhotinoGateway.udppacketsize];
		DatagramPacket sendPacket;
		sendData = (content).getBytes();
		try 
		{
			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), port);
			sOut.send(sendPacket);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return ret;
	}
}
