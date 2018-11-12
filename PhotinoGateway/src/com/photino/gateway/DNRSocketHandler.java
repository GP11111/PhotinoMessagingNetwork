package com.photino.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
		byte[] sendData = new byte[PhotinoGateway.udppacketsize];
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
					    GUI.outputText("Relay fragment has been received at destination ");
						break;

					case 20:
						GUI.outputText("Message for: "+recipient+" has been received - "+received);
						break;
					case 21:
						break;
					
					case 50:
						// retrieve messages for a user
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
						//if ( PhotinoGateway.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						if ( ValidateDNRDomain(IPAdr) )
						{
						
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateDNRDomains(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 76:
						//if ( PhotinoGateway.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						if ( ValidateDNRDomain(IPAdr) )
						{
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateDNRDomains(Integer.parseInt(info1), info2, "REMOVE");
							}
						}
						break;
					case 77:
						//if ( PhotinoGateway.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						if ( ValidateDNRDomain(IPAdr) )
						{
						
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateQueDomains(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 78:
						//if ( PhotinoGateway.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						if ( ValidateDNRDomain(IPAdr) )
						{
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateQueDomains(Integer.parseInt(info1), info2, "REMOVE");
							}
						}
						break;
					case 80:
						break;

					case 81:
						//
						// info1 has number of topicdomain to ip mappings in this message
						// info2 has the mappings
						//
						//if ( PhotinoGateway.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						if ( ValidateDNRDomain(IPAdr) )
						{
						
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateTopicDomains(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 83:
						//if ( PhotinoGateway.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						if ( ValidateDNRDomain(IPAdr) )
						{
						
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateTopicDomains(Integer.parseInt(info1), info2, "REMOVE"); 
							}
						}
						break;
					case 84:
						SendData("80|Refresh Topic|Domains",IPAdr,port);
						PhotinoGateway.topicdomain.clear();
						break;
					case 85:
						// info1 has domain ip, info2 has messageid from the messageque map
						MessageHandler.SendMessageFromQue(info1,info2);
						break;
					case 90:
						break;
					case 91:
						//
						// info1 has validation code or not
						// info2 has list of validated topics to listen for
						//
						GUI.outputText(info1+" "+info2+" "+IPAdr.getHostAddress());
						if ( info1.equals("TRUE") && ValidateDNRDomain(IPAdr) )
						{
							UpdateMyTopics(info2);
							PhotinoGateway.hasBeenValidated = true;
							GUI.updateTitle("Photino Gateway Node");
							GUI.updateStatus("Status: Validated and Processing");
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("74|Refresh DNR|DNRs",IPAdr,recipientPort);
							SendData("75|Refresh Ques|Ques",IPAdr,recipientPort);
						}
						else if ( !info1.equals("TRUE") && ValidateDNRDomain(IPAdr) )
						{
							PhotinoGateway.hasBeenValidated = false;
							UpdateMyTopics("");
							GUI.updateTitle("Photino Relay-Only Node");
							GUI.updateStatus("Status: Relay Node Processing");
						}
						else
						{
							//
							// do nothing
							//
							UpdateMyTopics(info2);
							PhotinoGateway.hasBeenValidated = true;
							GUI.updateTitle("Photino Gateway Node");
							GUI.updateStatus("Status: Validated and Processing default");
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("74|Refresh DNR|DNRs",IPAdr,recipientPort);
							SendData("75|Refresh Ques|Ques",IPAdr,recipientPort);
						}
						break;
					case 96:
						if ( info1.equals("FALSE") && ValidateDNRDomain(IPAdr) )
						{
							ReinitializeGateway();
						}
						break;
					case 97:
						SendKeepAlives(info1);
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
	
	public Boolean UpdateMyTopics(String mytopiclist)
	{
		Boolean ret = false;
		String[] tlist;
		PhotinoGateway.mytopics.clear();
		tlist = mytopiclist.split(",");
		if ( tlist.length >= 1 )
		{
			for ( int i=0; i < tlist.length; i++ )
			{
				PhotinoGateway.mytopics.put(tlist[i],Integer.toString(i));
				PhotinoGateway.topicdomain.put(tlist[i],PhotinoGateway.myIP);
			}
			ret = true;
		}
		else
		{
			mytopiclist = "RELAY NODE - NO TOPICS";
		}
	    GUI.updateMyTopics(mytopiclist);
		return ret;
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
					PhotinoGateway.dnrdomain.put(dnrIP,Integer.toString(PhotinoGateway.dnrdomain.size()));
				}
				else if ( updatetype.equals("REMOVE") )
				{
					if ( PhotinoGateway.dnrdomain.containsKey(dnrIP) )
					{
						PhotinoGateway.dnrdomain.remove(dnrIP);
					}
				}
			}
			ret = true;
		}
		GUI.outputText(updatetype+" dnrs: "+Integer.toString(PhotinoGateway.dnrdomain.size()));
		return ret;
		
	}
	public Boolean UpdateQueDomains(int numofques, String quelist, String updatetype)
	{
		// update type needs to be ADD or REMOVE
		Boolean ret = false;
		String[] qlist;
		String queIP;
		qlist = quelist.split(",");
		if ( qlist.length >= 1 )
		{
			for ( int i=0; i < qlist.length; i++ )
			{
				queIP = qlist[i];
				if ( updatetype.equals("ADD") )
				{
					PhotinoGateway.quedomain.put(queIP,Integer.toString(PhotinoGateway.quedomain.size()));
				}
				else if ( updatetype.equals("REMOVE") )
				{
					if ( PhotinoGateway.quedomain.containsKey(queIP) )
					{
						PhotinoGateway.quedomain.remove(queIP);
					}
				}
			}
			ret = true;
		}
		GUI.outputText(updatetype+" ques: "+Integer.toString(PhotinoGateway.quedomain.size()));
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
					PhotinoGateway.topicdomain.put(topicIP[0],topicIP[1]);
				}
				else if ( updatetype.equals("REMOVE") )
				{
					if ( PhotinoGateway.topicdomain.containsKey(topicIP[0]) )
					{
						PhotinoGateway.topicdomain.remove(topicIP[0]);
					}
				}
			}
			ret = true;
		}
		GUI.outputText(updatetype+" topics: "+Integer.toString(PhotinoGateway.topicdomain.size()));
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
			if ( PhotinoGateway.topicdomain.containsKey(recipient) )
			{
				ret = (String) PhotinoGateway.topicdomain.get(recipient);
				System.out.println("Domain "+recipient+"  IP :"+ret);
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
	public Boolean ReinitializeGateway()
	{
		Boolean ret = true;
		Random rand = new Random(); 
		byte[] sendDataDnr = new byte[PhotinoGateway.udppacketsize];
		int dnrPort = rand.nextInt((55024 - 55000) + 1) + 55000;
		String dnrSeed = "";
		try
		{
			URL url = new URL(PhotinoGateway.apiPath+"getSeed.php");
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			InputStream stream = connection.getInputStream();
			dnrSeed = new BufferedReader(new InputStreamReader(stream))
					.lines().collect(Collectors.joining("\n"));
			stream.close();
			
			if ( !dnrSeed.equals("") && validateIP(dnrSeed) )
			{
				sendDataDnr = ("90|"+PhotinoGateway.validationID+"|"+PhotinoGateway.myTopicList).getBytes();
				DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(dnrSeed), dnrPort);
				PhotinoGateway.socketOutDnr.send(sendPacketDnr);
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
	public Boolean SendKeepAlives(String iplist)
	{
		Boolean ret = true;
		Random rand = new Random(); 
		byte[] sendDataDnr = new byte[PhotinoGateway.udppacketsize];
		//int gPort = rand.nextInt((33009 - 33000) + 1) + 33000;
		int gPort = 44051;
		String[] ip = iplist.split(",");
		sendDataDnr = ("950|R|0||").getBytes();
		try
		{
			for ( int j=0; j < PhotinoGateway.maxnumIn; ++j )
			{
				for ( int k=0; k < ip.length; ++ k )
				{
					if ( !ip[k].equals("") && !ip[k].equals(PhotinoGateway.myIP) )
					{
						DatagramPacket sendPacketDnrI = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip[k]), gPort);
						PhotinoGateway.socketIn[j].send(sendPacketDnrI);
						System.out.println("port "+Integer.toString(j)+" Sending KA to Gateway "+ip[k]+" on port "+Integer.toString(gPort));
					}
				}
			}
		}
		catch (Exception e ) {}
		
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
}
