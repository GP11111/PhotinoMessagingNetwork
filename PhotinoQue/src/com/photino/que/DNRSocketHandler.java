package com.photino.que;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


//
//DNR SocketHandler class
//
class DNRSocketHandler extends Thread 
{
	final String content;
	final InetAddress IPAdr;
	final int port;
	final DatagramSocket sOut;

	public DNRSocketHandler(String content, InetAddress IPAdr, int port, DatagramSocket sOut) 
	{
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
		String recipient = "";
		String recipientDomain;
		String info1;
		String info2;
		int recipientPort;
		String received;
		String toreturn;
		byte[] sendData = new byte[PhotinoQue.udppacketsize];
		DatagramPacket sendPacket;
		Random rand = new Random(); 
		
		Boolean add = false;
		String messagelist = "";
		
	
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
						break;
					case 21:
						break;
					case 50:

						break;
					//
					// DNR
					//
					// for dnr domains
					// 74 get list of valid dnrs
					// 75 receive list of valid dnrs
					//
					// for topic domains
					// 80 get current topic domains from DNR
					// 81 receive current (added) topic domains in from DNR
					// 83 receive removed topic domains from DNR
					// 84 receive from DNR topic domain refresh required
					// 85 receive domain resolution from DNR and process message from que
					//
					// 90 send validation request to DNR with validID token and topic list
					// 91 receive validation OK from DNR with list of valid topics
					// 94 send keep alive
					// 95 receive keep alive confirmation
					//
					case 74:
						break;
					case 75:
						if ( PhotinoQue.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateDNRDomains(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 76:
						if ( PhotinoQue.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateDNRDomains(Integer.parseInt(info1), info2, "REMOVE");
							}
						}
						break;

					case 79:
						// 
						// receive usertag and token
						//
						if ( PhotinoQue.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							RegisterUserToken(info1,info2);
						}
						break;
					case 80:
						break;
					case 81:
						//
						// info1 has number of topicdomain to ip mappings in this message
						// info2 has the mappings
						//
						if ( PhotinoQue.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
						
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateTopicDomains(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 83:
						if ( PhotinoQue.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
						
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateTopicDomains(Integer.parseInt(info1), info2, "REMOVE"); 
							}
						}
						break;
					case 84:
						break;
					case 85:
						// info1 has domain ip, info2 has messageid from the messageque map
						SendMessageFromQue(info1,info2);
						break;
					case 90:
						
						break;
					case 93:
						//
						// info1 has validation code or not
						// info2 has list of validated topics to listen for
						//
						//GUI.outputText(info1+" "+info2+" "+IPAdr.getHostAddress());
						if ( info1.equals("TRUE") && ValidateDNRDomain(IPAdr) )
						{
							PhotinoQue.hasBeenValidated = true;
							GUI.updateStatus("Status: Validated and Processing");
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("80|Refresh Topic|QUE",IPAdr,recipientPort);
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("74|Refresh DNR|DNRs",IPAdr,recipientPort);
							// now get the update usertoken file from a live que node at info2
							if ( !info2.equals("") && !info2.equals(PhotinoQue.myIP) )
							{
								recipientPort = rand.nextInt((45024 - 45000) + 1) + 45000;
								SendData("86|Request Usertoken|usertoken.ser",info2,recipientPort);
							}
						}
						else if ( !info1.equals("TRUE") && ValidateDNRDomain(IPAdr) )
						{
							PhotinoQue.hasBeenValidated = false;
							//GUI.updateTitle("Photino Relay-Only Node");
							//GUI.updateStatus("Status: Relay Node Processing");
						}
						else
						{
							//
							// do nothing
							//
							PhotinoQue.hasBeenValidated = true;
							GUI.updateStatus("Status: Validated and Processing default");
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("80|Refresh Topic|DNR",IPAdr,recipientPort);
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("74|Refresh DNR|QUEs",IPAdr,recipientPort);
							// now get the update usertoken file from a live que node at info2
							if ( !info2.equals("") && !info2.equals(PhotinoQue.myIP) )
							{
								recipientPort = rand.nextInt((45024 - 45000) + 1) + 45000;
								SendData("86|Request Usertoken|usertoken.ser",info2,recipientPort);
							}
						}
						break;
					case 98:
						if ( info1.equals("FALSE") && ValidateDNRDomain(IPAdr) )
						{
							ReinitializeQue();
						}
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
	
	public Boolean UpdateDNRDomains(int numofdnrs, String dnrlist, String updatetype)
	{
		// update type needs to be ADD or REMOVE
		Boolean ret = false;
		String[] dlist;
		String dnrIP;
		dlist = dnrlist.split(",");
		//if ( updatetype.equals("ADD")) PhotinoGateway.dnrdomain.clear();
		if ( dlist.length >= 1 )
		{
			for ( int i=0; i < dlist.length; i++ )
			{
				dnrIP = dlist[i];
				if ( updatetype.equals("ADD") )
				{
					PhotinoQue.dnrdomain.put(dnrIP,Integer.toString(PhotinoQue.dnrdomain.size()));
				}
				else if ( updatetype.equals("REMOVE") )
				{
					if ( PhotinoQue.dnrdomain.containsKey(dnrIP) )
					{
						PhotinoQue.dnrdomain.remove(dnrIP);
					}
				}
			}
			ret = true;
		}
		GUI.outputText(updatetype+" dnrs: "+Integer.toString(PhotinoQue.dnrdomain.size()));
		return ret;
		
	}
	public Boolean UpdateTopicDomains(int numoftopics, String topiclist, String updatetype)
	{
		// update type needs to be ADD or REMOVE
		Boolean ret = false;
		String[] tlist;
		String[] topicIP;
		tlist = topiclist.split(",");
		if ( tlist.length >= 1 )
		{
			for ( int i=0; i < tlist.length; i++ )
			{
				topicIP = tlist[i].split(":");
				if ( updatetype.equals("ADD") )
				{
					PhotinoQue.topicdomain.put(topicIP[0],topicIP[1]);
				}
				else if ( updatetype.equals("REMOVE") )
				{
					if ( PhotinoQue.topicdomain.containsKey(topicIP[0]) )
					{
						PhotinoQue.topicdomain.remove(topicIP[0]);
					}
				}
			}
			ret = true;
		}
		GUI.outputText(updatetype+" topics: "+Integer.toString(PhotinoQue.topicdomain.size()));
		return ret;
		
	}
	public Boolean RegisterUserToken(String usertag, String token )
	{
		Boolean ret = false;
		int secs = (int) ((new Date().getTime())/1000); 
		usertag = usertag.toUpperCase();
		PhotinoQue.usertoken.put(usertag,secs+","+token);
		ret = true;
		//
		// write file local
		//
		try
		{	
			FileOutputStream fos;
			fos = new FileOutputStream("usertoken.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(PhotinoQue.usertoken);
			oos.close();
		}
		catch (Exception e ) {}
		return ret;
	}
	public String ResolveTopic(String recipient)
	{
		String ret = "";
		//
		// resolve the ip from the domain via the DNR Map
		//
		if ( recipient.indexOf('@') >= 0 )
		{
			recipient = recipient.substring(recipient.indexOf("@")+1);
			if ( PhotinoQue.topicdomain.containsKey(recipient) )
			{
				ret = (String) PhotinoQue.topicdomain.get(recipient);
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
		if ( PhotinoQue.dnrdomain.containsKey(ip))
		{
			ret = true;
		}
		
		return ret;
	}
	public Boolean ReinitializeQue()
	{
		Boolean ret = true;
		Random rand = new Random(); 
		byte[] sendDataDnr = new byte[PhotinoQue.udppacketsize];
		int dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
		String dnrSeed = "";
		try
		{
			URL url = new URL(PhotinoQue.apiPath+"getSeed.php");
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			InputStream stream = connection.getInputStream();
			dnrSeed = new BufferedReader(new InputStreamReader(stream))
					.lines().collect(Collectors.joining("\n"));
			stream.close();
			
			if ( !dnrSeed.equals("") && validateIP(dnrSeed) )
			{
				sendDataDnr = ("92|"+PhotinoQue.validationID+"|").getBytes();
				DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrSeed), dnrPort);
				PhotinoQue.socketOutDnr.send(sendPacketDnr);
			}
		}
		catch (IOException e) {e.printStackTrace();}

		return ret;
	}
	public static boolean validateIP(final String ip) 
	{
	    String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	    return ip.matches(PATTERN);
	}
	public static Boolean SendMessageFromQue(String ip, String messageid)
	{
		Boolean ret = false;
		
		if ( !ip.equals("") )
		{
			if ( PhotinoQue.messageque.containsKey(messageid) )
			{
				String message = (String) PhotinoQue.messageque.get(messageid);
				PhotinoQue.messageque.remove(messageid);
				Random generator = new Random();
				int port = generator.nextInt((33009 - 33000) + 1) + 33000;
				int socketnum = generator.nextInt(PhotinoQue.maxnumOut+1);
				try
				{
					byte[] sendData = new byte[PhotinoQue.udppacketsize];
					sendData = ("20|"+message).getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), port);
					PhotinoQue.socketOut[socketnum].send(sendPacket);
				} 
				catch (IOException e) {e.printStackTrace();}
			}
		}
		
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

