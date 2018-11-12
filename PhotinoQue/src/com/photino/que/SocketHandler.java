package com.photino.que;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
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
		String info1;
		String info2;
		String recipientDomain;
		int recipientPort;
		String received;
		String receivedmessage;
		String toreturn;
		byte[] sendData = new byte[PhotinoQue.udppacketsize];
		DatagramPacket sendPacket;
		Random rand = new Random(); 
		
		Boolean add = false;
		String messagelist = "";
		
		int secs = (int) ((new Date().getTime())/1000);
		
		//
		// content format: mtype | recipient | mid || messagereceived
		//
	
			//try 
			//{
				// receive the content and split
				//
				// format: message type | info1 | info2
				//
				received = content;
				mtype = Integer.parseInt(received.substring(0,received.indexOf("|") )); 
				received = received.substring(received.indexOf("|")+1);
				info1 = received.substring(0,received.indexOf("|") ); 
				received = received.substring(received.indexOf("|")+1);
				info2 = received.substring(0);
				received = content;
   
				// creating Date object
				Date date = new Date();
				
				// add-update receivedip map
				if ( !PhotinoQue.whitelistip.containsKey(IPAdr.getHostAddress()) )
				{
					if ( PhotinoQue.receivedip.containsKey(IPAdr.getHostAddress()) )
					{
						PhotinoQue.receivedip.put(IPAdr.getHostAddress(), Integer.toString(Integer.parseInt(PhotinoQue.receivedip.get(IPAdr.getHostAddress()))+1));
					}
					else
					{
						PhotinoQue.receivedip.put(IPAdr.getHostAddress(), "1");
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
					case 1 :
						break;
					case 2 :
						break;
					case 3 :
						break;
					case 4 :
						break;
					case 5 :
						break;
					case 6 :
						break;
					case 10:
						break;
					case 11:
						break;
					case 12:
						break;
					case 20:
						// receive message to be queued as the gateway serving the topic is offline
						// info1 has recipient, info2 has messageid || message	
						// messageque has format of key = messageid, value = createtime | mtype | recipient |  message
						toreturn = secs + "|" + Integer.toString(mtype) + "|" + info1 + "|" + info2.substring(info2.indexOf("||")+2);
						PhotinoQue.messageque.put(info2.substring(0, info2.indexOf("||")), toreturn);
						PhotinoQue.messageQueued = true;
						SendData("21|Message queued - recipient gateway not currently available.",IPAdr,port);
						break;
					case 21:
						break;
					case 22:
						// receive message from gateway as backup
						if ( ValidateGatewayTopic(IPAdr,info1) )
						{
							// info1 has recipient, info2 has messageid || message
							// message has format of key = recipient, value = createtime |  message
							toreturn = secs + "|" + info2.substring(info2.indexOf("||")+2);
							AddMessage(info1, toreturn);
							PhotinoQue.messageReceived = true;
						}
						break;
					case 24:
						// receive from gateway that a recipient has picked up their messages
						// clean out the messages associated to the usertag from the message store
						if ( ValidateGatewayTopic(IPAdr,info1) )
						{
							ClearMessages(info1);
						}
						break;
					case 50:
						// retrieve messages for a usertag
						messagelist = GetMessages(info1);
						SendData("51|"+messagelist,IPAdr,port);
						break;
					case 56:
						// info1 has recipient, info2 has message
						if ( ValidateGatewayTopic(IPAdr,info1) )
						{
							SendNotificationRequest(info1,info2);
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
					case 86:
						int sport = rand.nextInt((58099 - 58000) + 1) + 58000;// random port for file transfer
						int recipientport = rand.nextInt((45024 - 45000) + 1) + 45000;
						File f = new File(info2);
						if(f.exists() && !f.isDirectory()) 
						{
							SendData("87|"+info2+"|"+Integer.toString(sport),IPAdr,recipientport);
							FileSend.SendFile(info2,sport);
						}
						break;
					case 87:
						FileReceive.ReceiveFile(info1,IPAdr.getHostAddress(),Integer.parseInt(info2));
						break;
						
					case 90:
						
						break;
					case 91:
						
						break;
						
					default:

						break;
				}
				return;
			//} 
			//catch (IOException e) 
			//{
			//	e.printStackTrace();
			//}
	}
	public static Boolean AddMessage(String recipient, String message)
	{
		//message has format of key = recipient, value = createtime |  message ( || createtime|message || createtime|message ... )
		Boolean ret = false;
		if ( PhotinoQue.messages.containsKey(recipient) )
		{
			String existingmessages = (String) PhotinoQue.messages.get(recipient);
			message = existingmessages + "||" + message;
		}
		PhotinoQue.messages.put(recipient, message);
		ret = true;
		return ret;
	}
	public static Boolean ClearMessages(String recipient)
	{
		Boolean ret = false;
		if ( PhotinoQue.messages.containsKey(recipient) )
		{
			PhotinoQue.messages.remove(recipient);
		}
		ret = true;
		return ret;
	}
	public static String GetMessages(String recipient)
	{
		//
		// returns the total number of messages for this recipient that are on this que
		// and the messages separated by commas that can fit in a single packet
		//
		// note: if the messages are longer than a packet, the first set will be returned, and the remainder
		// will be re-stored into the message map. The client should look at the total number vs the number in the list
		// to determine if all the messages have been returned or not. If not, the client should re-issue the call to pickup
		// the remaining messages.
		//
		String messagelist = "";
		String messagestokeep = "";
		int numberinset = 0;
		int numberofmessages = 0;
		String messagestosend ="";
		int maxpacketsize = (PhotinoQue.udppacketsize/1000) * 1000;
		String[] mlist;
		String[] messagecontent;
		if ( PhotinoQue.messages.containsKey(recipient) )
		{
			messagelist = (String) PhotinoQue.messages.get(recipient);
			mlist = messagelist.split("\\|\\|");
			numberofmessages = mlist.length;
			for ( int j=0; j < mlist.length; ++ j )
			{
				messagecontent = mlist[j].split("\\|");
				if ( ( messagestosend.length() + messagecontent[1].length() + 1 ) < maxpacketsize )
				{
					if ( messagestosend.equals("") ) messagestosend = messagecontent[1];
					else messagestosend = messagestosend + "|" + messagecontent[1]; 
				}
				else
				{
					for (int k=j; k < mlist.length; ++k )
					{
						if ( messagestokeep.equals("") ) messagestokeep = mlist[k];
						else messagestokeep = messagestokeep + "||" + mlist[k];
					}
					j = mlist.length + 1;
				}
			}
			if ( messagestokeep.equals("") )
			{
				PhotinoQue.messages.remove(recipient);
			}
			else
			{
				PhotinoQue.messages.put(recipient, messagestokeep);
			}
		}
		
		messagelist = numberofmessages + "|" + messagestosend;
		
		return messagelist;
	}
	public Boolean ValidateGatewayTopic(InetAddress IPAdr, String usertag )
	{
		Boolean ret = false;
		String ip = IPAdr.getHostAddress();
		String topic = usertag.substring(usertag.indexOf("@")+1);
		String topicip = "";
		if ( PhotinoQue.topicdomain.containsKey(topic) )
		{
			topicip = PhotinoQue.topicdomain.get(topic);
			if ( topicip.equals(ip) ) ret = true;
		}
		return ret;
	}
	public Boolean SendNotificationRequest(String recipient, String message)
	{
		Boolean ret = false;

		if ( PhotinoQue.usertoken.containsKey(recipient) )
		{
			String token = PhotinoQue.usertoken.get(recipient);
			token = token.substring(token.indexOf(",")+1);
			Random rand = new Random();
			int dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
			Object[] values = PhotinoQue.dnrdomain.keySet().toArray();// values().toArray();
			String dnrIP = (String) values[rand.nextInt(values.length)];
			SendData("56|"+token+"|"+message,dnrIP,dnrPort);
			
			GUI.outputText("Sending notification to "+recipient);
			
			values = null;
		}
		ret = true;
		return ret;
	}
	public Boolean SendData(String content, InetAddress IPAdr, int port)
	{
		Boolean ret = true;
		byte[] sendData = new byte[PhotinoQue.udppacketsize];
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
		byte[] sendData = new byte[PhotinoQue.udppacketsize];
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

