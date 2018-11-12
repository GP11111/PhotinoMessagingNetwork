package com.photino.dnr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


//
//DNR SocketHandler class
//
class DNRSocketHandler extends Thread 
{
	final String content;
	final InetAddress IPAdr;
	final int port;
	final DatagramSocket sOut;


	// Constructor
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
		byte[] sendData = new byte[PhotinoDNR.udppacketsize];
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
				if ( !PhotinoDNR.whitelistip.containsKey(IPAdr.getHostAddress()) )
				{
					if ( PhotinoDNR.receivedip.containsKey(IPAdr.getHostAddress()) )
					{
						PhotinoDNR.receivedip.put(IPAdr.getHostAddress(), Integer.toString(Integer.parseInt(PhotinoDNR.receivedip.get(IPAdr.getHostAddress()))+1));
					}
					else
					{
						PhotinoDNR.receivedip.put(IPAdr.getHostAddress(), "1");
					}
				}
				
				//
				// determine action based on mtype
				//
				switch (mtype) 
				{
					case 0:
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
					    GUI.outputText("Relay fragment has been received at destination ");
						break;

					case 20:
						GUI.outputText("Message for: "+recipient+" has been received - "+received);
						break;
					case 21:
						break;
					case 30:
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							RegisterUserPublicKey(info1,info2);
						}
						break;
					case 50:
						// retrieve messages for a user
						break;
					case 60:
						//
						// receive dnr request for validation 60, respond with 61
						//
						toreturn = ValidateDNR(info1,info2,IPAdr.getHostAddress());
						SendData(toreturn,IPAdr,port-1);
						if ( toreturn.substring(toreturn.indexOf("|")+1,7).equals("TRUE") )
						{
							SendDNRUpdate(IPAdr.getHostAddress(),port-1);
						}
						break;
					case 61:
						GUI.outputText(info1+" "+info2+" "+IPAdr.getHostAddress());
						if ( info1.equals("TRUE") && ValidateDNRDomain(IPAdr) )
						{
							PhotinoDNR.hasBeenValidated = true;
							GUI.updateStatus("Status: Validated and Processing");
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("80|Refresh Topic|DNR",IPAdr,recipientPort);
							SendData("75|Refresh Ques|DNR",IPAdr,recipientPort);
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("79|Get Relay Only Nodes|DNR",IPAdr,recipientPort);
							PhotinoDNR.dnrdomain.put(PhotinoDNR.myIP, Integer.toString(PhotinoDNR.dnrdomain.size()+1));
							UpdateDNRDomains(info2);
							if ( !PhotinoDNR.myIP.equals(PhotinoDNR.dnrSeed) ) SendData("86|Get UserRegistry|userregistrydid.ser",IPAdr,PhotinoDNR.dnrIn);
							GUI.outputText("DNR domain="+Integer.toString(PhotinoDNR.dnrdomain.size()));
						}
						else if ( !info1.equals("TRUE") && ValidateDNRDomain(IPAdr) )
						{
							PhotinoDNR.hasBeenValidated = false;
						}
						else
						{
							//
							// do nothing
							//
							PhotinoDNR.hasBeenValidated = true;
							GUI.updateStatus("Status: Validated and Processing default");
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("80|Refresh Topic|DNR",IPAdr,recipientPort);
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("79|Get Relay Only Nodes|DNR",IPAdr,recipientPort);
							PhotinoDNR.dnrdomain.put(PhotinoDNR.myIP, Integer.toString(PhotinoDNR.dnrdomain.size()+1));
							UpdateDNRDomains(info2);
							if ( !PhotinoDNR.myIP.equals(PhotinoDNR.dnrSeed) ) SendData("86|Get UserRegistry|userregistrydid.ser",IPAdr,PhotinoDNR.dnrIn);
							GUI.outputText("DNR domain="+Integer.toString(PhotinoDNR.dnrdomain.size()));
						}
						break;
					
					case 70:
						// receiving user registry update from registering dnr
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							toreturn = RegisterUser(info1,info2);
						}
						break;
					
					case 71:
						// for dnr domains
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							UpdateDNRDomains(info1);
						}
						break;
					
					case 72:
						// for gateway adds
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							UpdateGateways(info1,info2);
						}
						break;
					
					case 73:
						// for que node adds
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							UpdateQueNodes(info1);
						}
						break;
					case 74:
						// for relay only gateway adds
						// info1 has number of relay only gateways in this message
						// info2 has the list of ips
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								UpdateRelayOnlyGateways(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 77:
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateQueDomains(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 78:
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateQueDomains(Integer.parseInt(info1), info2, "REMOVE");
							}
						}
						break;
					case 79:
						// for user token adds
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							toreturn = info2.substring(info2.indexOf(",")+1);
							info2 = info2.substring(0, info2.indexOf(","));
							Registertokendevice(info1,info2,toreturn);
						}
						break;
					//
					// DNR
					//
					// for topic domains
					// 80 request current topic domains from DNR
					// 81 receive current (added) topic domains in from DNR
					// 83 receive removed topic domains from DNR
					// 84 receive from DNR topic domain refresh required
					// 85 receive domain resolution from DNR and process message from que
					// 86 request userregistry file from the DNR seed
					// 87 receive the userregistry file from the DNR seed (done through server sockets)
					// 88 request tokendevice file from the DNR seed
					// 89 receive the tokendevice file from the DNR seed (done through server sockets)
					//
					// 90 send validation request to DNR with validID token and topic list
					// 91 receive validation OK from DNR with list of valid topics
					// 94 send keep alive
					// 95 receive keep alive confirmation
					//
					case 80:
						break;

					case 81:
						//
						// info1 has number of topicdomain to ip mappings in this message
						// info2 has the mappings
						//
						GUI.outputText(Boolean.toString(PhotinoDNR.hasBeenValidated));
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							GUI.outputText(Boolean.toString(PhotinoDNR.hasBeenValidated));
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateTopicDomains(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 83:
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
						
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								 UpdateTopicDomains(Integer.parseInt(info1), info2, "REMOVE");
							}
						}
						break;
					case 84:
						SendData("80|Refresh Topic|Domains",IPAdr,port);
						PhotinoDNR.topicdomain.clear();
						break;
					case 85:
						// info1 has domain ip, info2 has messageid from the messageque map
						//MessageHandler.SendMessageFromQue(info1,info2);
						break;
					case 86:
						if ( ValidateDNRDomain(IPAdr) )
						{
							int sport = rand.nextInt((58099 - 58000) + 1) + 58000;;// random port for file transfer
							File f = new File(info2);
							if(f.exists() && !f.isDirectory()) 
							{
								SendData("87|"+info2+"|"+Integer.toString(sport),IPAdr,PhotinoDNR.dnrIn);
								FileSend.SendFile(info2,sport);
							}
							else
							{
								if ( info2.equals("tokendevice.ser"))
								{
									info2 = "userpublickey.ser";
									f = new File(info2);
									if(f.exists() && !f.isDirectory()) 
									{
										SendData("87|"+info2+"|"+Integer.toString(sport),IPAdr,PhotinoDNR.dnrIn);
										FileSend.SendFile(info2,sport);
									}
								}
							}
						}
						break;
					case 87:
						if ( ValidateDNRDomain(IPAdr) )
						{
							FileReceive.ReceiveFile(info1,IPAdr.getHostAddress(),Integer.parseInt(info2));
						}
						break;
				//
				// 88 and 89 no longer used
				//
					case 88:
						if ( ValidateDNRDomain(IPAdr) )
						{
							File f = new File("tokendevice.ser");
							if(f.exists() && !f.isDirectory()) 
							{
								int sport = rand.nextInt((58099 - 58000) + 1) + 58000;;// random port for file transfer
								SendData("89|tokendevice.ser|"+Integer.toString(sport),IPAdr,PhotinoDNR.dnrIn);
								FileSend.SendFile("tokendevice.ser",sport);
							}
						}
						break;
					case 89:
						if ( ValidateDNRDomain(IPAdr) )
						{
							FileReceive.ReceiveFile(info1,IPAdr.getHostAddress(),Integer.parseInt(info2));
						}
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
							PhotinoDNR.hasBeenValidated = true;
							GUI.updateStatus("Status: Validated and Processing");
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("80|Refresh Topic|DNR",IPAdr,recipientPort);
							SendData("75|Refresh Ques|DNR",IPAdr,recipientPort);
							SendData("86|Request Userregistry|userregistrydid.ser",IPAdr,PhotinoDNR.dnrIn);
						}
						else if ( !info1.equals("TRUE") && ValidateDNRDomain(IPAdr) )
						{
							PhotinoDNR.hasBeenValidated = false;
						}
						else
						{
							//
							// do nothing
							//
							PhotinoDNR.hasBeenValidated = true;
							GUI.updateStatus("Status: Validated and Processing default");
							recipientPort = rand.nextInt((55024 - 55000) + 1) + 55000;
							SendData("80|Refresh Topic|DNR",IPAdr,port);
						}
						break;
					case 95:
						ReceiveKeepAlive(IPAdr);
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
	
	public Boolean UpdateQueDomains(int numofques, String quelist, String updatetype)
	{
		// update type needs to be ADD or REMOVE
		Boolean ret = false;
		int secs = (int) ((new Date().getTime())/1000);
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
					PhotinoDNR.quedomain.put(queIP,Integer.toString(secs));
				}
				else if ( updatetype.equals("REMOVE") )
				{
					if ( PhotinoDNR.quedomain.containsKey(queIP) )
					{
						PhotinoDNR.quedomain.remove(queIP);
					}
				}
			}
			ret = true;
		}
		GUI.outputText(updatetype+" ques: "+Integer.toString(PhotinoDNR.quedomain.size()));
		return ret;
	}
	public Boolean UpdateTopicDomains(int numoftopics, String topiclist, String updatetype)
	{
		// update type needs to be ADD or REMOVE
		Boolean ret = false;
		Boolean behindR = false;
		int secs = (int) ((new Date().getTime())/1000);
		String[] tlist;
		String[] topicIP;
		String topiclistcs = "";
		String iplistcs = "";
		tlist = topiclist.split(",");
GUI.outputText("\n"+updatetype+" Topics: "+topiclist);
		if ( tlist.length >= 1 )
		{
			for ( int i=0; i < tlist.length; i++ )
			{
				topicIP = tlist[i].split(":");
				iplistcs = topicIP[1];
				behindR = false;
				if ( iplistcs.substring(0, 1).equals("R") )
				{
					behindR = true;
					iplistcs = iplistcs.substring(1);
				}
				if ( updatetype.equals("ADD") )
				{	
					if ( !PhotinoDNR.topicdomain.containsKey(topicIP[0]) )
					{
						PhotinoDNR.topicdomain.put(topicIP[0],topicIP[1]); 	
						PhotinoDNR.activeip.put(topicIP[1],Integer.toString(secs));
						if ( behindR ) PhotinoDNR.activeiprouter.put(topicIP[1],Integer.toString(secs));
						if ( topiclistcs.equals("") ) topiclistcs = topicIP[0];
						else topiclistcs = topiclistcs + "," + topicIP[0];
					}
					
				}
				else if ( updatetype.equals("REMOVE") )
				{
GUI.outputText("Removing "+topicIP[0]+" "+topicIP[1]);
					if ( PhotinoDNR.topicdomain.containsKey(topicIP[0]) )
					{
						PhotinoDNR.topicdomain.remove(topicIP[0]);
					}
				}
			}
			//
			// now add-remove from domaintopic list
			//
			if ( !topiclistcs.equals("") && !iplistcs.equals("") && updatetype.equals("ADD") )
			{
				if ( !PhotinoDNR.domaintopic.containsKey(iplistcs) )
				{
					PhotinoDNR.domaintopic.put(iplistcs,topiclistcs);
				}
			}
			if ( updatetype.equals("REMOVE") )
			{
GUI.outputText("Removing domaintopic "+iplistcs);
				PhotinoDNR.domaintopic.remove(iplistcs);
			}
			ret = true;
		}
		GUI.outputText(updatetype+" topics: "+Integer.toString(PhotinoDNR.topicdomain.size())+"\n");
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
			if ( PhotinoDNR.topicdomain.containsKey(recipient) )
			{
				ret = (String) PhotinoDNR.topicdomain.get(recipient);
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
		if ( PhotinoDNR.dnrdomain.containsKey(ip))
		{
			ret = true;
		}
		
		return ret;
	}
	public String ValidateDNR(String validationID, String info2, String dnr)
	{
		Boolean ret = false;
		String validateddnr = "";
		// validate the dnr credentials via the API
		try 
		{
			
			String url = PhotinoDNR.apiPath + "validateNode.php";
			String param = "{\"validationID\":\""+validationID+"\", \"type\":\""+"D"+"\"}";
			String charset = "UTF-8"; 
			URLConnection connection = new URL(url).openConnection();
			connection.setDoOutput(true);// set to post
			connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
			OutputStream output = connection.getOutputStream();
			output.write(param.getBytes(charset));
			InputStream response = connection.getInputStream();
			byte[] respb = new byte[PhotinoDNR.udppacketsize];
			response.read(respb);
			String result = new String(respb);
			result = result.replaceAll("\u0000.*", "");
			
			if ( result.equals("TRUE"))
			{
				ret = true;
			}
			GUI.outputText(result);
		} 
		catch (IOException e) {e.printStackTrace();}
		
		String dnrlist = "";
		
		if ( ret )
		{
			int secs = (int) ((new Date().getTime())/1000);
			PhotinoDNR.dnrdomain.put(dnr, Integer.toString(secs));
			PhotinoDNR.whitelistip.put(dnr, Integer.toString(secs));
			//
			// now create existing list of dnrs to send in response
			//
			Map<String,String> tdnrdomain = new HashMap<String, String>();
			tdnrdomain.putAll(PhotinoDNR.dnrdomain);
			
			Set s = tdnrdomain.entrySet();
			Iterator it = s.iterator();
			String ip = "";
			
			while(it.hasNext())
			{
			    Map.Entry m =(Map.Entry)it.next();
			    ip = (String)m.getKey();
				if ( !ip.equals(PhotinoDNR.myIP) && !ip.equals(dnr) )
				{
					if ( dnrlist.equals("" )) dnrlist = ip;
					else dnrlist = dnrlist +"," + ip;
				}
			}
			tdnrdomain.clear();
		}
		
		validateddnr = "61|"+Boolean.toString(ret).toUpperCase()+"|"+dnrlist;
	
		GUI.outputText("DNR Added: "+Boolean.toString(ret).toUpperCase()+" "+dnr);
		
		return validateddnr;
	}
	public Boolean UpdateRelayOnlyGateways(int numofgateways, String gatewaylist, String updatetype)
	{
		// update type needs to be ADD or REMOVE
		Boolean ret = false;
		String behindR = "";
		int secs = (int) ((new Date().getTime())/1000);
		String[] glist;
		glist = gatewaylist.split(",");
		
		if ( glist.length >= 1 )
		{
			for ( int i=0; i < glist.length; i++ )
			{
				behindR = "";
				if ( glist[i].substring(0, 1).equals("R") )
				{
					behindR = "R";
					glist[i] = glist[i].substring(1);
				}
				UpdateGateways(glist[i], behindR);
			}
		}
		return ret;
	}
	public Boolean UpdateGateways(String ip, String behindR)
	{
		Boolean ret = false;
		
		int secs = (int) ((new Date().getTime())/1000);
		PhotinoDNR.activeip.put(ip,Integer.toString(secs));
		if ( behindR.equals("R") )
		{
			PhotinoDNR.activeiprouter.put(ip,Integer.toString(secs));
		}
		return ret;
	}
	public Boolean UpdateQueNodes(String ip)
	{
		Boolean ret = false;
		
		int secs = (int) ((new Date().getTime())/1000);
		PhotinoDNR.quedomain.put(ip,Integer.toString(secs));
		
		return ret;
	}
	public Boolean ReceiveKeepAlive(InetAddress IPAdr)
	{
		Boolean ret = false;
		String ipaddress = IPAdr.getHostAddress();
		//GUI.outputText("Received KA DNRDomain="+Integer.toString(PhotinoDNR.dnrdomain.size())+" "+ipaddress);
		if ( PhotinoDNR.dnrdomain.containsKey(ipaddress) )
		{
			int secs = (int) ((new Date().getTime())/1000); 	
			PhotinoDNR.dnrdomain.put(ipaddress,Integer.toString(secs));
			//GUI.outputText("Updated activeip "+ipaddress);
		}		
		ret = true;
		return ret;
	}
	public Boolean UpdateDNRDomains(String ips)
	{
		Boolean ret = true;
		int secs = (int) ((new Date().getTime())/1000);
		String[] iplist = ips.split(",");
		if ( iplist.length > 0 )
		{
			for ( int i=0; i < iplist.length; i++ )
			{
				if ( !iplist[i].trim().equals("") )
				{
					PhotinoDNR.dnrdomain.put(iplist[i], Integer.toString(secs));
					PhotinoDNR.whitelistip.put(iplist[i], Integer.toString(secs));
				}
			}
		}
		return ret;
	}
	public String RegisterUser(String usertag, String id)
	{
		Boolean ret = false;
		usertag = usertag.toUpperCase();
		String tag = usertag.substring(usertag.indexOf("@")+1);
		String retstring = "";
		String resp = usertag;
		if ( !PhotinoDNR.userregistry.containsKey(usertag) )
		{
			if ( PhotinoDNR.topicdomain.containsKey(tag) )
			{
				PhotinoDNR.userregistry.put(usertag,id);
				if ( !PhotinoDNR.userregistrydid.containsKey(id))
				{
					PhotinoDNR.userregistrydid.put(id, usertag);
				}
				else
				{
					String taglist = (String) PhotinoDNR.userregistrydid.get(id);
					taglist = taglist + "," + usertag;
					PhotinoDNR.userregistrydid.put(id,taglist);
				}
			
				ret = true;
				//
				// write maps to local file
				//
				try 
				{
					FileOutputStream fos;
					fos = new FileOutputStream("userregistry.ser");
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(PhotinoDNR.userregistry);
					oos.close();
					fos = new FileOutputStream("userregistrydid.ser");
					oos = new ObjectOutputStream(fos);
					oos.writeObject(PhotinoDNR.userregistrydid);
					oos.close();
				} 
				catch (IOException e) {e.printStackTrace();}
			}
			else
			{
				resp = "Topic is invalid"; 
			}
		}
		else
		{
			String uid = (String) PhotinoDNR.userregistry.get(usertag);
			if ( uid.equals(id) )
			{
				ret = true;
			}
			else
			{
				resp = "Tag is already registered to another user";
			}
		}
		
	    if ( ret ) 
	    {	
	    	GUI.outputText("Added User: "+usertag+" "+id);
	    }
	    else 
	    {
	    	GUI.outputText("User "+usertag+" "+resp);
	    	
	   	}
	    
	    retstring = "Added User: "+Boolean.toString(ret).toUpperCase()+" - "+resp;
		
		return retstring;
	}
	public String Registertokendevice(String usertag, String deviceid, String token)
	{
		Boolean ret = false;
		usertag = usertag.toUpperCase();
		String tag = usertag.substring(usertag.indexOf("@")+1);
		String retstring = "";
		String resp = "";
		
		if ( !deviceid.equals("") && PhotinoDNR.userregistry.containsKey(usertag) )
		{
			
			if ( !PhotinoDNR.tokendevice.containsKey(token) )
			{
				// encrypt the deviceid
				try
				{
					String keyst = PhotinoDNR.myIP;
					keyst = keyst.replace(".", "");
					while ( keyst.length() < 16 ) keyst = keyst + keyst;
					keyst = keyst.substring(0,16);
					byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
					IvParameterSpec ivspec = new IvParameterSpec(iv);
					SecretKeySpec skeySpec = new SecretKeySpec(keyst.getBytes("UTF-8"), "AES");
					Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
					c.init(Cipher.ENCRYPT_MODE, skeySpec, ivspec);
					byte[] edeviceid = c.doFinal(deviceid.getBytes());
					deviceid = Base64.getEncoder().encodeToString((edeviceid));
					int secs = (int) ((new Date().getTime())/1000); 
					PhotinoDNR.tokendevice.put(token,secs+","+deviceid);
					resp = token;
					ret = true;
				}
				catch (Exception e) {}
				//
				// write maps to local file
				//
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
			else
			{
				resp = "Token is invalid, try again."; 
			}
		}
		else
		{
				resp = "Invalid usertag and/or device.";
		}
		

	    GUI.outputText("Token Added: "+Boolean.toString(ret).toUpperCase()+" "+resp);
	    
	    retstring = "79|"+Boolean.toString(ret).toUpperCase()+"|"+resp;
		
		return retstring;
	}
	public String RegisterUserPublicKey(String tag, String idpk)
	{
		// info1 has usertag, info2 has: did || public key string
		Boolean ret = false;
		String retstring = "";
		if ( !idpk.equals("") )
		{
			PhotinoDNR.userpublickey.put(tag, idpk.substring(idpk.indexOf("||")+2));
			retstring = "31|"+Boolean.toString(ret).toUpperCase()+"|Public Key Added for usertag "+tag;
			//
			// write maps to local file
			//
			try 
			{
				FileOutputStream fos;
				fos = new FileOutputStream("userpublickey.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(PhotinoDNR.userpublickey);
				oos.close();
			} 
			catch (IOException e) {e.printStackTrace();}
		}
		else
		{
			retstring = "31|"+Boolean.toString(ret).toUpperCase()+"|Invalid Public Key.";
		}

		return retstring;
	}
	
	public Boolean SendDNRUpdate(String ip, int port)
	{
		Boolean ret = true;
		String newDNRip = ip;
		//
		// now send update to other dnrs
		//
		Map<String,String> tdnrdomain = new HashMap<String, String>();
		tdnrdomain.putAll(PhotinoDNR.dnrdomain);
		
		byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		sendDataDnr = ("71|"+newDNRip+"|DNR Update").getBytes();
		
		Set s = tdnrdomain.entrySet();
		Iterator it = s.iterator();
		
		GUI.outputText("Updating DNRs");
		
		while(it.hasNext())
		{
		    // key=value separator this by Map.Entry to get key and value
		    Map.Entry m =(Map.Entry)it.next();

		    // getKey is used to get key of Map
		    ip = (String)m.getKey();
		    
			if ( !ip.equals(PhotinoDNR.myIP) )
			{
				try 
				{
					DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), port);
					PhotinoDNR.socketOutDnr.send(sendPacketDnr);
				} 
				catch (IOException e) {}
			}
		}
		tdnrdomain.clear();
		
		//
		//
		// send validated dnr info to gateways
		//
		//
		sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		sendDataDnr = ("75|1|"+newDNRip).getBytes();
		DatagramPacket sendPacketDnr;
		
		Map<String,String> tactiveip = new HashMap<String, String>();
		tactiveip.putAll(PhotinoDNR.activeip);

		Random rand = new Random();
		
		s = tactiveip.entrySet();
		it = s.iterator();
		port = 53000;
		int outSocket = 0;
		
		// message 75|1|ip to gateways at port 53000 using random out sockets
		
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    ip = (String)m.getKey();
		    
		    outSocket = rand.nextInt(24);
		    
				try 
				{
					sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), port);
					PhotinoDNR.socketOut[outSocket].send(sendPacketDnr);
				} 
				catch (IOException e) {}
		}
		tactiveip.clear();
		
		//
		//
		// send validated dnr info to ques
		//
		//
		sendDataDnr = new byte[PhotinoDNR.udppacketsize];
		sendDataDnr = ("75|1|"+newDNRip).getBytes();
		
		Map<String,String> tquedomain = new HashMap<String, String>();
		tquedomain.putAll(PhotinoDNR.quedomain);
		
		s = tquedomain.entrySet();
		it = s.iterator();
		port = 59100;
		outSocket = 0;
		
		// message 75|1|ip to ques at port 59100 using random out sockets
		
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    ip = (String)m.getKey();
		    
		    outSocket = rand.nextInt(24);
		    
				try 
				{
					sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(ip), port);
					PhotinoDNR.socketOut[outSocket].send(sendPacketDnr);
				} 
				catch (IOException e) {}
		}
		tquedomain.clear();
		
		return ret;
	}
	public Boolean SendData(String content, InetAddress IPAdr, int port)
	{
		Boolean ret = true;
		byte[] sendData = new byte[PhotinoDNR.udppacketsize];
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
