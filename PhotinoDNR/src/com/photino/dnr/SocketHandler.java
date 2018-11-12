package com.photino.dnr;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


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
						// transmission encrypted message
						SendData("2|Message Sent", IPAdr, port);
						break;
					case 2 :
					    GUI.outputText("Relay encrypted message has been received at assembly \n");
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
						// 10 - key fragments
						SendData("2|Fragment sent", IPAdr, port);
						recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
						break;
					case 11:
						recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
						break;
					case 12:
					    GUI.outputText("Relay fragment has been received at destination ");
						break;
					case 20:
						// info1 has recipient, info2 has messageid || message
						int secs = (int) ((new Date().getTime())/1000);
						// messageque has format of key = messageid, value = createtime | mtype | recipient |  message
						toreturn = secs + "|" + Integer.toString(mtype) + "|" + info1 + "|" + info2.substring(info2.indexOf("||")+2);
						PhotinoDNR.messageque.put(info2.substring(0, info2.indexOf("||")), toreturn);
						PhotinoDNR.messageQueued = true;
						SendData("21|Message queued - recipient gateway not currently available.",IPAdr,port);
						break;
					case 21:
					    GUI.outputText("Relay message has been received at destination ");
						break;
					case 30:
						// store usertag with public key
						// info1 has usertag, info2 has: did || public key string
						toreturn = RegisterUserPublicKey(info1,info2);
						SendData(toreturn,IPAdr,port);
						if ( toreturn.substring(toreturn.indexOf("|")+1,7).equals("TRUE") )
						{
							SendToDNR("30|"+info1+"|"+info2);
						}
						break;
					case 32:
						// get stored public key from usertag
						toreturn = GetUserPublicKey(info1);
						SendData(toreturn,IPAdr,port);
						break;
					case 40:
						// get transmission encryption
						toreturn = SetTransmissionEncryption(info1,info2);
						SendData(toreturn,IPAdr,port);
						//GenerateKeyPair.CreateKeyPair();
						break;
					case 42:
						// validate user tag, get fragmentation servers, get transmission encryption
						toreturn = ValidateFragmentEncrypt(info1);
						SendData(toreturn,IPAdr,port);
						break;
					case 50:
						// retrieve messages for a user
						SendData("51|"+messagelist,IPAdr,port);
						break;
					case 52:
						toreturn = ValidateUser(info1,info2);
						if ( toreturn.substring(toreturn.indexOf("|")+1,7).equals("TRUE") )
						{
							recipientPort = rand.nextInt((33009 - 33000) + 1) + 33000;
							recipientDomain = toreturn.substring(toreturn.indexOf("|")+1);
							recipientDomain = recipientDomain.substring(recipientDomain.indexOf("|")+1);
							SendData("52|"+info1+"|0||"+IPAdr.getHostAddress()+":"+port,recipientDomain,recipientPort);
						}
						else
						{
							SendData("51|FALSE|User tag is not valid.",IPAdr,port);
						}
						break;
					case 56:
						// sending remote notification to device, info1 is message, info2 is token
						if ( ValidateQueDomain(IPAdr) )
						{
							sendNotification(info1,info2);
						}
						break;
					//
					// DNR
					//
					// for dnr to dnr communications
					// 60 request dnr registration
					// 61 receive response from registration request
					// 62 request user registry update
					// 63 receive user registry update
					// 64 request domaintopic update
					// 65 receive domaintopic update
					//
					// for user registration from API
					// 70 request user registration
					// 71 receive response for user registration
					// 72 request/receive userid - did - from user tag
					// 73 request/receive user device from token
					//
					// 74 request for current dnr domain list
					// 75 receive current dnr domain list
					//
					// 76 request for usertag validation - respond with 77
					// 77 request for user validation - respond with 77
					//
					// 78 request for user token registration
					// 79 receive response for user token registration
					//
					// for topic domains
					// 80 get current topic domains from DNR
					// 81 receive current topic domains from DNR
					// 82 receive from DNR topic domain refresh required
					// 84 get topic domain resolution
					// 85 receive topic domain resolution
					// 86 get fragmentation and assembly server domain resolution
					// 87 receive fragmentation and assembly server domain resolution
					// 88 get topics list (partial list for API)
					// 89 receive topics list
					//
					// 90 send validation request to DNR with validID token and topic list
					// 91 receive validation OK from DNR with list of valid topics
					// 94 send keep alive
					// 95 receive keep alive
					//
					case 70:
						toreturn = RegisterUser(info1,info2);
						SendData(toreturn,IPAdr,port);
						if ( toreturn.substring(toreturn.indexOf("|")+1,7).equals("TRUE") )
						{
							SendToDNR("70|"+info1+"|"+info2);
						}
						break;
					case 72:
						toreturn = GetUserDID(info1);
						SendData(toreturn,IPAdr,port);
						break;
					case 73:
						toreturn = GetDeviceInfoFromToken(info1);
						SendData(toreturn,IPAdr,port);
						break;
					case 74:
						GetDNRDomains(IPAdr,port-1,info2);
						break;
					case 75:
						GetQueDomains(IPAdr,port-1,info2);
						break;
					case 76:
						toreturn = ValidateUserTag(info1);
						SendData(toreturn,IPAdr,port);
						break;
					case 77:
						toreturn = ValidateUser(info1,info2);
						SendData(toreturn,IPAdr,port);
						break;
					case 78:
						toreturn = RegisterTokendevice(info1,info2);
						SendData(toreturn,IPAdr,port);
						if ( toreturn.substring(toreturn.indexOf("|")+1,7).equals("TRUE") )
						{
							toreturn = toreturn.substring(toreturn.indexOf("|")+1);
							toreturn = toreturn.substring(toreturn.indexOf("|")+1);
							if ( !toreturn.equals("") )
							{
								SendToDNR("79|"+info1+"|"+info2+","+toreturn);
								SendToQues("79|"+info1+"|"+toreturn);
							}
						}
						break;
					case 79:
						GetRelayOnlyGateways(IPAdr,port-1,info2);
						break;
					case 80:
						GetTopicDomains(IPAdr,port-1,info2);
						break;
					case 81:
						// info1 has number of topicdomain to ip mappings in this message
						// info2 has the mappings
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								UpdateTopicDomainsDNR(Integer.parseInt(info1), info2, "ADD");
							}
						}
						break;
					case 83:
						if ( PhotinoDNR.hasBeenValidated && ValidateDNRDomain(IPAdr) )
						{
							if ( Integer.parseInt(info1) > 0 && !info2.equals("") )
							{
								UpdateTopicDomainsDNR(Integer.parseInt(info1), info2, "REMOVE");
							}
						}
						break;
					case 84:
						toreturn = ResolveTopic(info1);
						toreturn = "85|"+toreturn+"|"+info2;
						if ( info2.equals("API")) SendData(toreturn,IPAdr,port);
						else SendData(toreturn,IPAdr,port-1);
						break;
					case 86:
						toreturn = ResolveTopicsForFragmentation(info1);
						//toreturn = "87|"+toreturn+"|"+info2;
						toreturn = "87|"+toreturn;
						SendData(toreturn,IPAdr,port);
						break;
					case 88:
						toreturn = GetTopicList();
						SendData(toreturn,IPAdr,port);
						break;
					case 90:
						toreturn = ValidateGateway(info1, info2, IPAdr, port-1);
						SendData(toreturn,IPAdr,port-1);
						// this can be true or false, if true gateway is a validated gateway, 
						// if false gateway is a node (relay) only, unvalidated
						//if ( toreturn.substring(toreturn.indexOf("|")+1,7).equals("TRUE") )
						//{
							if ( info1.substring(info1.length()-3).equals("--R") ) info1 = "R";
							else info1 = "";
							SendToDNR("72|"+IPAdr.getHostAddress()+"|"+info1);
							info2 = toreturn.substring(toreturn.indexOf("|")+1);
							info2 = info2.substring(info2.indexOf("|")+1);
							if ( !info2.equals("") )
							{
								SendTopicDomainUpdate(info2,IPAdr.getHostAddress());
							}
						//}
						break;
					case 91:
						break;
					case 92:
						toreturn = ValidateQueNode(info1,IPAdr,port-1);
						SendData(toreturn,IPAdr,port-1);
						if ( toreturn.substring(toreturn.indexOf("|")+1,7).equals("TRUE") )
						{
							SendToDNR("73|"+IPAdr.getHostAddress()+"|Que Add");
							SendToGateways("77|1|"+IPAdr.getHostAddress());
						}
						break;
					case 95:
						ReceiveKeepAlive(IPAdr,"G");
						//int gport = rand.nextInt((33009 - 33000) + 1) + 33000;
						//SendData("950||",IPAdr,gport);
						break;
					case 96:
						if ( PhotinoDNR.activeip.containsKey(IPAdr.getHostAddress()) ) toreturn = "96|TRUE|";
						else toreturn = "96|FALSE|";
						SendData(toreturn,IPAdr,port-1);
						break;
					case 97:
						ReceiveKeepAlive(IPAdr,"Q");
						break;
					case 98:
						if ( PhotinoDNR.quedomain.containsKey(IPAdr.getHostAddress()) ) toreturn = "98|TRUE|";
						else toreturn = "98|FALSE|";
						SendData(toreturn,IPAdr,port-1);
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
	public String ResolveTopic(String recipient)
	{
		String ret = "";
		//
		// resolve the ip from the domain via the DNR Map
		//
		if ( recipient.indexOf('@') >= 0 )
		{
			recipient = recipient.substring(recipient.indexOf("@")+1);
			recipient = recipient.toUpperCase();
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
	public String ResolveTopicsForFragmentation(String recipient)
	{
		String ret = "";
		String recipientip = "";
		String assemblyip = "";
		Boolean recipientbehindR = false;
		Boolean assemblybehindR = false;
		Random generator = new Random();
		if ( !recipient.equals(""))
		{
			if ( recipient.indexOf('@') >= 0 )
			{
				recipient = recipient.substring(recipient.indexOf("@")+1);
				if ( PhotinoDNR.topicdomain.containsKey(recipient) )
				{
					recipientip = (String) PhotinoDNR.topicdomain.get(recipient);
					if ( PhotinoDNR.activeiprouter.containsKey(recipientip) ) recipientbehindR = true;
				}
			}
		}
		//
		// get 6 random active gateways
		//
		Map<String,String> tactiveip = new HashMap<String, String>();
		tactiveip.putAll(PhotinoDNR.activeip);
		
		Object[] values = tactiveip.keySet().toArray();// values().toArray();
		String randomGateway = "";
		if ( tactiveip.size() > 1 )
		{
			for ( int i=0; i < 6; i++ )
			{
				randomGateway = (String) values[generator.nextInt(values.length)];
				if ( !randomGateway.equals(recipientip) )
				{
					// the assembly server (i=5) cannot be behind a fireqall-router
					if ( i == 5 && PhotinoDNR.activeiprouter.containsKey(randomGateway) )
					{
						i = i - 1;
						assemblyip = randomGateway;
						if ( PhotinoDNR.activeiprouter.containsKey(assemblyip) ) assemblybehindR = true;
					}
					else
					{
						if ( ret.equals("") ) ret = randomGateway;
						else ret = ret + "," + randomGateway;
					}
				}
				else i = i - 1;
			}
		}
		else if ( tactiveip.size() == 1 ) 
		{
			ret = (String) values[generator.nextInt(values.length)];
			assemblyip = ret;
			if ( PhotinoDNR.activeiprouter.containsKey(assemblyip) ) assemblybehindR = true;
			ret = ret + "," + ret + "," + ret + "," + ret + "," + ret + "," + ret;
			
		}
		values = null;
		tactiveip.clear();;
		
		// let the caller know the if recipient/assembly gateway is behind a firewall-router
		ret = ret + "|";
		if ( recipientbehindR ) ret = ret + recipientip;
		ret = ret + ",";
		if ( assemblybehindR ) ret = ret + assemblyip;
		
		return ret;
	}
	public String SetTransmissionEncryption(String recipient, String message)
	{
		Boolean ret = false;
		String retstring = "41|FALSE|";
		if ( !recipient.equals("") && !message.equals("") )
		{
			try
			{
				PrivateKey privKey  = GenerateKeyPair.GetPrivateKey();
				if ( privKey != null )
				{
					String pubKey = GenerateKeyPair.GetPubKey(privKey);		
					if ( pubKey != null && !pubKey.isEmpty() )
					{
						Cipher cipher = Cipher.getInstance("RSA");
						cipher.init(Cipher.ENCRYPT_MODE, privKey);
						//String encrypted_msg = Base64.getEncoder().encodeToString(cipher.doFinal((recipient+"||"+message).getBytes("UTF-8")));
						String encrypted_recipient = Base64.getEncoder().encodeToString(cipher.doFinal((recipient).getBytes("UTF-8")));
						retstring = "41|"+pubKey+"|"+encrypted_recipient;
					}
				}
			}
			catch (Exception e) 
			{
				GUI.outputText(e.toString());
			}
			
		}
		
		return retstring;
	}
	public String ValidateFragmentEncrypt(String recipient)
	{
		Boolean ret = false;
		String retstring = "44|False|recipient";
		String fragmentationset = "";
		String encrypted = "";
		String validate = ValidateUserTag(recipient);
		if ( validate.substring(validate.indexOf("|")+1,7).equals("TRUE") )
		{
			fragmentationset = ResolveTopicsForFragmentation(recipient);
			if ( !fragmentationset.equals("") )
			{
				encrypted = SetTransmissionEncryption(recipient," ");
				if ( !encrypted.substring(encrypted.indexOf("|")+1,8).equals("FALSE") )
				{
					retstring = "44|TRUE|"+fragmentationset+encrypted.substring(encrypted.indexOf("|"));
				}
			}
		}
		
		return retstring;
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
				resp = "Tag is already registered.";
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
	    
	    retstring = "71|"+Boolean.toString(ret).toUpperCase()+"|"+resp;
		
		return retstring;
	}
	public String RegisterTokendevice(String usertag, String deviceid)
	{
		Boolean ret = false;
		usertag = usertag.toUpperCase();
		String tag = usertag.substring(usertag.indexOf("@")+1);
		String retstring = "";
		String resp = "";
		String token = "";
		
		if ( PhotinoDNR.quedomain.size() > 0 )
		{
			if ( !deviceid.equals("") && PhotinoDNR.userregistry.containsKey(usertag) )
			{
				SecureRandom random = new SecureRandom();
				byte bytes[] = new byte[64];
				random.nextBytes(bytes);
				//token = bytes.toString();
				Encoder encoder = Base64.getUrlEncoder().withoutPadding();
				token = encoder.encodeToString(bytes);
			
				if ( !PhotinoDNR.tokendevice.containsKey(token) )
				{
					// now encrypt the deviceid
					try 
					{
						String keyst = PhotinoDNR.myIP;
						keyst = keyst.replace(".", "");
						while ( keyst.length() < 16 ) keyst = keyst + keyst;
						keyst = keyst.substring(0,16);
						byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
						IvParameterSpec ivSpec = new IvParameterSpec(iv);
						SecretKeySpec skeySpec = new SecretKeySpec(keyst.getBytes("UTF-8"), "AES");
						Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
						c.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
						byte[] edeviceid = c.doFinal(deviceid.getBytes());
						deviceid = Base64.getEncoder().encodeToString((edeviceid));
					
						int secs = (int) ((new Date().getTime())/1000); 
						PhotinoDNR.tokendevice.put(token,secs+","+deviceid);
						resp = token;
						ret = true;
					} 
					catch (Exception e1) 
					{
						e1.printStackTrace();
						GUI.outputText(e1.getMessage());
					}
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
		}
		else
		{
			resp = "Device registration is currently unavailable.";
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
		
		String validate = ValidateUser(tag,idpk.substring(0,idpk.indexOf("||")));
		if ( validate.substring(validate.indexOf("|")+1,7).equals("TRUE") )
		{
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
		}
		else
		{
			retstring = "31|"+Boolean.toString(ret).toUpperCase()+"|Invalid User Tag.";
		}
		return retstring;
	}
	public String GetUserPublicKey(String tag)
	{
		Boolean ret = false;
		String retstring = "";
		String pubkey = "";
		if ( PhotinoDNR.userpublickey.containsKey(tag) )
		{
			pubkey = (String) PhotinoDNR.userpublickey.get(tag);
			ret = true;
		}
		
		retstring = "33|"+Boolean.toString(ret).toUpperCase()+"|"+pubkey;
		
		return retstring;
		
	}
	public String GetUserDID(String tag)
	{
		Boolean ret = false;
		String retstring = "";
		String did = "";
		if ( PhotinoDNR.userregistry.containsKey(tag) )
		{
			did  = (String) PhotinoDNR.userregistry.get(tag);
			ret = true;
		}
		
		retstring = "72|"+Boolean.toString(ret).toUpperCase()+"|"+did;
		
		return retstring;
	}
	public String GetDeviceInfoFromToken(String token)
	{
		Boolean ret = false;
		String retstring = "";
		String deviceid = "Not Found";
		if ( PhotinoDNR.tokendevice.containsKey(token) )
		{
			deviceid  = (String) PhotinoDNR.tokendevice.get(token);
			//
			// token value is in format of: 'timeOfCreation,usertag,deviceid'
			// changed to only contain timeOfCreation,deviceid
			//
			deviceid = deviceid.substring(deviceid.indexOf(",")+1);
			
			// decrypt deviceid
			try
			{
				String keyst = PhotinoDNR.myIP;
				keyst = keyst.replace(".", "");
				while ( keyst.length() < 16 ) keyst = keyst + keyst;
				keyst = keyst.substring(0,16);
				byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
				IvParameterSpec iv5pec = new IvParameterSpec(iv);
				SecretKeySpec skeySpec = new SecretKeySpec(keyst.getBytes("UTF-8"), "AES");
				Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
				c.init(Cipher.DECRYPT_MODE, skeySpec, iv5pec);
				byte[] original = c.doFinal(Base64.getDecoder().decode(deviceid));
				deviceid = new String(original); 	
				ret = true;
			}
			catch (Exception e ) {}
		}
		
		retstring = "73|"+Boolean.toString(ret).toUpperCase()+"|"+deviceid;
		
		return retstring;
	}
	public String ValidateUserTag(String tag)
	{
		Boolean ret = false;
		String retstring = "";
		tag = tag.toUpperCase();
		if ( PhotinoDNR.userregistry.containsKey(tag) )
		{
			ret = true;
			//
			// resolve the ip from the domain via the DNR Map
			//
			if ( tag.indexOf('@') >= 0 )
			{
				String tagip = tag.substring(tag.indexOf("@")+1);
				if ( PhotinoDNR.topicdomain.containsKey(tagip) )
				{
					tag = (String) PhotinoDNR.topicdomain.get(tagip);
				}
				else
				{
					// return the que server for this message
					if ( PhotinoDNR.quedomain.size() > 0 )
					{
						Random generator = new Random();
						Object[] values = PhotinoDNR.quedomain.keySet().toArray();// values().toArray();
						String randomIP = (String) values[generator.nextInt(values.length)];
						tag = "Q" + randomIP;
						values = null;
					}
					else tag = "D" + PhotinoDNR.myIP;
				}
			}
		}
		else
		{
			ret = false;
			tag = "Usertag is not recognized";
		}
		
		retstring = "77|"+Boolean.toString(ret).toUpperCase()+"|"+tag;
		
		return retstring;
	}
	public String ValidateUser(String tag, String did)
	{
		Boolean ret = false;
		String retstring = "";
		tag = tag.toUpperCase();
		if ( PhotinoDNR.userregistry.containsKey(tag) )
		{
			if ( did.equals(PhotinoDNR.userregistry.get(tag)))
			{
				ret = true;
				//
				// resolve the ip from the domain via the DNR Map
				//
				if ( tag.indexOf('@') >= 0 )
				{
					String tagip = tag.substring(tag.indexOf("@")+1);
					if ( PhotinoDNR.topicdomain.containsKey(tagip) )
					{
						tag = (String) PhotinoDNR.topicdomain.get(tagip);
					}
				}
			}
		}
		else
		{
			ret = false;
			tag = "Usertag is not valid";
		}
		
		retstring = "77|"+Boolean.toString(ret).toUpperCase()+"|"+tag;
		
		return retstring;
	}
	public Boolean GetDNRDomains(InetAddress IPAdr, int port, String info2)
	{
		//
		// Note when sending the set of dnr domains must split packets to be smaller than PhotinoDNR.updpacketsie bytes
		//
		if ( info2.equals("API") ) port = port + 1;
		Random rand = new Random();
		Boolean ret = false;
		String key = "";
		String value = "";
		String sendset = "";
		int numberinset = 0;
		int maxpacketsize = (PhotinoDNR.udppacketsize/1000) * 1000;
		Map<String,String> tdnrdomain = new HashMap<String, String>();
		tdnrdomain.putAll(PhotinoDNR.dnrdomain);
		Set s = tdnrdomain.entrySet();
		Iterator it = s.iterator();
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    key = (String) m.getKey();
		    //value = (String)m.getValue();
		    if ( sendset.length() < maxpacketsize )
		    {
		    	if ( sendset.length() == 0 ) sendset = key;
		    	else sendset = sendset + "," + key;
		    	numberinset = numberinset + 1;
		    }
		    else
		    {
		    	SendData("75|"+Integer.toString(numberinset)+"|"+sendset,IPAdr,port);
		    	sendset = "";
		    	numberinset = 0;
		    }
		}	
		if ( sendset.length() > 0 ) SendData("75|"+Integer.toString(numberinset)+"|"+sendset,IPAdr,port);
		tdnrdomain.clear();
		return ret;
	}
	public Boolean GetQueDomains(InetAddress IPAdr, int port, String info2)
	{
		//
		// Note when sending the set of que domains must split packets to be smaller than PhotinoDNR.updpacketsie bytes
		//
		if ( info2.equals("API") ) port = port + 1;
		Random rand = new Random();
		Boolean ret = false;
		String key = "";
		String value = "";
		String sendset = "";
		int numberinset = 0;
		int maxpacketsize = (PhotinoDNR.udppacketsize/1000) * 1000;
		Map<String,String> tquedomain = new HashMap<String, String>();
		tquedomain.putAll(PhotinoDNR.quedomain);
		Set s = tquedomain.entrySet();
		Iterator it = s.iterator();
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    key = (String) m.getKey();
		    //value = (String)m.getValue();
		    if ( sendset.length() < maxpacketsize )
		    {
		    	if ( sendset.length() == 0 ) sendset = key;
		    	else sendset = sendset + "," + key;
		    	numberinset = numberinset + 1;
		    }
		    else
		    {
		    	SendData("77|"+Integer.toString(numberinset)+"|"+sendset,IPAdr,port);
		    	sendset = "";
		    	numberinset = 0;
		    }
		}	
		if ( sendset.length() > 0 ) SendData("77|"+Integer.toString(numberinset)+"|"+sendset,IPAdr,port);
		tquedomain.clear();
		return ret;
	}
	public Boolean GetRelayOnlyGateways(InetAddress IPAdr, int port, String info2)
	{
		//
		// Note when sending the set of ips must split packets to be smaller than PhotinoDNR.udppacketsize bytes
		//
		Random rand = new Random();
		//if ( info2.equals("DNR") ) port = rand.nextInt((55009 - 55000) + 1) + 55000; 
		Boolean ret = false;
		Boolean includeR = false;
		if ( info2.equals("DNR") ) includeR = true;
		String key = "";
		String value = "";
		String sendset = "";
		int numberinset = 0;
		int maxpacketsize = (PhotinoDNR.udppacketsize/1000) * 1000;
		Map<String,String> tactiveip = new HashMap<String, String>();
		tactiveip.putAll(PhotinoDNR.activeip);
		Map<String,String> tdomaintopic = new HashMap<String, String>();
		tdomaintopic.putAll(PhotinoDNR.domaintopic);
		Set s = tactiveip.entrySet();
		Iterator it = s.iterator();
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    key = (String) m.getKey();
		    value = (String)m.getValue();
		    if ( !tdomaintopic.containsKey(key) )
		    {
		    	if ( includeR ) { if ( PhotinoDNR.activeiprouter.containsKey(key) ) key = "R" + key;}
		    	if ( ( sendset.length() + key.length() + 2 ) < maxpacketsize )
		    	{
		    		if ( sendset.length() == 0 ) sendset = key;
		    		else sendset = sendset + "," + key;
		    		numberinset = numberinset + 1;
		    	}
		    	else
		    	{
		    		SendData("74|"+Integer.toString(numberinset)+"|"+sendset,IPAdr,port);
		    		sendset = key;
		    		numberinset = 1;
		    	}
		    }
		}
		if ( sendset.length() > 0 ) SendData("74|"+Integer.toString(numberinset)+"|"+sendset,IPAdr,port);
		tactiveip.clear();
		tdomaintopic.clear();
		return ret;
	}
	public Boolean GetTopicDomains(InetAddress IPAdr, int port, String info2)
	{
		//
		// Note when sending the set of topic domains must split packets to be smaller than PhotinoDNR.udppacketsize bytes
		//
		Random rand = new Random();
		//if ( info2.equals("DNR") ) port = rand.nextInt((55009 - 55000) + 1) + 55000; 
		Boolean ret = false;
		Boolean includeR = false;
		if ( info2.equals("DNR") ) includeR = true;
		String key = "";
		String value = "";
		String sendset = "";
		int numberinset = 0;
		int maxpacketsize = (PhotinoDNR.udppacketsize/1000) * 1000;
		Map<String,String> ttopicdomain = new HashMap<String, String>();
		ttopicdomain.putAll(PhotinoDNR.topicdomain);
		Set s = ttopicdomain.entrySet();
		Iterator it = s.iterator();
		while(it.hasNext())
		{
		    Map.Entry m =(Map.Entry)it.next();
		    key = (String) m.getKey();
		    value = (String)m.getValue();
		    if ( includeR ) { if ( PhotinoDNR.activeiprouter.containsKey(value) ) value = "R" + value;}
		    if ( ( sendset.length() + key.length() + value.length() + 2 ) < maxpacketsize )
		    {
		    	if ( sendset.length() == 0 ) sendset = key + ":" + value;
		    	else sendset = sendset + "," + key + ":" + value;
		    	numberinset = numberinset + 1;
		    }
		    else
		    {
		    	SendData("81|"+Integer.toString(numberinset)+"|"+sendset,IPAdr,port);
		    	sendset = key + ":" + value;;
		    	numberinset = 1;
		    }
		}	
		if ( sendset.length() > 0 ) SendData("81|"+Integer.toString(numberinset)+"|"+sendset,IPAdr,port);
		ttopicdomain.clear();
		return ret;
	}
	public String GetTopicList()
	{
		String ret = "";
		int numoftopics = 0;
		Random generator = new Random();
		//
		// get 10 random topics
		//
		Map<String,String> ttopicdomain = new HashMap<String, String>();
		ttopicdomain.putAll(PhotinoDNR.topicdomain);
		
		Object[] values = ttopicdomain.keySet().toArray();// values().toArray();
		String randomTopic = "";
		if ( ttopicdomain.size() > 10 )
		{
			for ( int i=0; i < 10; i++ )
			{
				randomTopic = (String) values[generator.nextInt(values.length)];
				if ( !ret.contains(randomTopic) )
				{
					if ( ret.equals("") ) ret = randomTopic;
					else ret = ret + "," + randomTopic;
					numoftopics = numoftopics + 1;
				}
				else i = i - 1;
			}
		}
		else if ( ttopicdomain.size() <= 10 && ttopicdomain.size() > 0 ) 
		{
			for ( int j=0; j < values.length; ++ j )
			{
				if ( ret.equals("") ) ret = (String) values[j];
				else ret = ret + ',' + (String) values[j];
				numoftopics = numoftopics + 1;
			}
		}
		values = null;
		ttopicdomain.clear();;
		GUI.outputText("Sending topiclist: "+ ret);
		ret = "89|"+Integer.toString(numoftopics)+"|"+ret;
		return ret;
	}
	public Boolean UpdateTopicDomainsDNR(int numoftopics, String topiclist, String updatetype)
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
					PhotinoDNR.topicdomain.put(topicIP[0],topicIP[1]);
				}
				else if ( updatetype.equals("REMOVE") )
				{
					if ( PhotinoDNR.topicdomain.containsKey("topicIP[0]") )
					{
						PhotinoDNR.topicdomain.remove(topicIP[0]);
					}
				}
			}
			ret = true;
		}
		GUI.outputText(updatetype+" topics: "+Integer.toString(PhotinoDNR.topicdomain.size()));
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
	public Boolean ValidateQueDomain(InetAddress IPAdr)
	{
		Boolean ret = false;
		String ip = IPAdr.getHostAddress();
		if ( PhotinoDNR.quedomain.containsKey(ip))
		{
			ret = true;
		}
		return ret;
	}
	public String ValidateGateway(String validationID, String topics, InetAddress IPAdr, int port)
	{
		Boolean ret = false;
		Boolean behindR = false;
		if ( validationID.substring(validationID.length()-3).equals("--R") ) 
		{
			behindR = true;
			validationID = validationID.substring(0,validationID.length()-3 );
		}
		String validatedtopiclist = "";
		// validate the gateway credentials via the API
		try 
		{
			
			String url = PhotinoDNR.apiPath + "validateNode.php";
			String param = "{\"validationID\":\""+validationID+"\", \"type\":\""+"G"+"\"}";
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
		
		if ( ret )
		{
			// validated as full gateway node
			validatedtopiclist = UpdateTopicDomain(topics,IPAdr);
			int secs = (int) ((new Date().getTime())/1000); 	
			PhotinoDNR.activeip.put(IPAdr.getHostAddress(),Integer.toString(secs));
			if ( behindR ) PhotinoDNR.activeiprouter.put(IPAdr.getHostAddress(),"");
		}
		else
		{
			// validation failed, so make this node a relay only node not full gateway
			validatedtopiclist = "";
			int secs = (int) ((new Date().getTime())/1000); 	
			PhotinoDNR.activeip.put(IPAdr.getHostAddress(),Integer.toString(secs));
			if ( behindR ) PhotinoDNR.activeiprouter.put(IPAdr.getHostAddress(),"");
		}
		validatedtopiclist = "91|"+Boolean.toString(ret).toUpperCase()+"|"+validatedtopiclist;
	
		return validatedtopiclist;
	}
	public String ValidateQueNode(String validationID, InetAddress IPAdr, int port)
	{
		Boolean ret = false;
		String queip = "";
		String rstring = "";
		// validate the que node credentials via the API
		try 
		{	
			String url = PhotinoDNR.apiPath + "validateNode.php";
			String param = "{\"validationID\":\""+validationID+"\", \"type\":\""+"Q"+"\"}";
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
		
		if ( ret )
		{
			// validated as que node
			// get ip of another validated que node
			if ( PhotinoDNR.quedomain.size() > 0 )
			{
				Random generator = new Random();
				Map<String,String> tquedomain = new HashMap<String, String>();
				tquedomain.putAll(PhotinoDNR.quedomain);
				
				Object[] values = tquedomain.keySet().toArray();// values().toArray();
				queip = (String) values[generator.nextInt(values.length)];
				
				values = null;
				tquedomain.clear();
			}
			int secs = (int) ((new Date().getTime())/1000); 	
			PhotinoDNR.quedomain.put(IPAdr.getHostAddress(),Integer.toString(secs));
		}
		else
		{
			// validation failed

		}
		rstring = "93|"+Boolean.toString(ret).toUpperCase()+"|"+queip;
	
		return rstring;
	}
	public String UpdateTopicDomain(String topiclist, InetAddress IPAdr)
	{
		//
		// gateway has been validated, now clear out old listening topics 
		// and record new listening topics
		//
		String ret = "";
		String ipaddress = IPAdr.getHostAddress();
		String[] tlist;
		topiclist = topiclist.toUpperCase();
		//
		// get list of current topics by this ip
		// and remove
		//
		String taglist = "";
		if ( PhotinoDNR.domaintopic.containsKey(ipaddress) )
		{
			taglist = (String) PhotinoDNR.domaintopic.get(ipaddress);
			PhotinoDNR.domaintopic.remove(ipaddress);
		}
		if ( !taglist.equals("") )
		{
			tlist = taglist.split(",");
			for ( int i=0; i < tlist.length; i++ )
			{
				if ( PhotinoDNR.topicdomain.containsKey(tlist[i]) )
				{
					PhotinoDNR.topicdomain.remove(tlist[i]);
				}
			}
		}
		//
		// now process new topics
		//
		tlist = topiclist.split(",");
		if ( tlist.length >= 1 )
		{
			for ( int i=0; i < tlist.length; i++ )
			{
				if ( !PhotinoDNR.topicdomain.containsKey(tlist[i]) )
				{
					PhotinoDNR.topicdomain.put(tlist[i],ipaddress);
					if ( ret.equals("") ) ret = tlist[i];
					else ret = ret + "," + tlist[i];
				}

			}
			if ( !PhotinoDNR.domaintopic.containsKey(ipaddress) && !ret.equals("") )
			{
				PhotinoDNR.domaintopic.put(ipaddress,ret);
			}
		}
	    GUI.outputText("Added Topics:"+ret);
		return ret;
	}
	public Boolean SendToDNR(String message)
	{
		Boolean ret = false;
		
		String ip = "";
		Map.Entry m;
		Map<String,String> tdnrdomain = new HashMap<String, String>();
		tdnrdomain.putAll(PhotinoDNR.dnrdomain);
		
		Set s = tdnrdomain.entrySet();
		Iterator it=s.iterator();
		
		byte[] sendData = new byte[PhotinoDNR.udppacketsize];
		
		sendData = (message).getBytes();
		
    	if ( tdnrdomain.size() > 1 )
    	{
	    	s = tdnrdomain.entrySet();
			it = s.iterator();
			while(it.hasNext())
			{
				m =(Map.Entry)it.next();
				ip = (String)m.getKey();
				if ( !ip.equals(PhotinoDNR.myIP) )
				{
					try
					{
						DatagramPacket sendPacketDnr = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), PhotinoDNR.dnrIn);
						PhotinoDNR.socketOutDnr.send(sendPacketDnr);
		    		} 
		    		catch (IOException e) 
		    		{
		    			e.printStackTrace();
		    		}
				}
			}
    	}
    	tdnrdomain.clear();
		
		return ret;
	}
	public Boolean SendToQues(String message)
	{
		Boolean ret = false;
		String ip = "";
		int qPort = 59100;
		Map.Entry m;
		Map<String,String> tquedomain = new HashMap<String, String>();
		tquedomain.putAll(PhotinoDNR.quedomain);
		
		Set s = tquedomain.entrySet();
		Iterator it=s.iterator();
		
		byte[] sendData = new byte[PhotinoDNR.udppacketsize];
		DatagramPacket sendPacket;
		
		sendData = (message).getBytes();
		
    	if ( tquedomain.size() > 0 )
    	{
	    	s = tquedomain.entrySet();
			it = s.iterator();
			while(it.hasNext())
			{
				m =(Map.Entry)it.next();
				ip = (String)m.getKey();
				try
				{
		    		sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), qPort);
		    		sOut.send(sendPacket);
		    	} 
		    	catch (IOException e) 
		    	{
		    		e.printStackTrace();
		    	}
			}
    	}
    	tquedomain.clear();
		
		return ret;
	}
	public Boolean SendToGateways(String message)
	{
		Boolean ret = false;
		String ip = "";
		int gPort = 53000;
		Map.Entry m;
		Map<String,String> tactiveip = new HashMap<String, String>();
		tactiveip.putAll(PhotinoDNR.activeip);
		
		Set s = tactiveip.entrySet();
		Iterator it=s.iterator();
		
		byte[] sendData = new byte[PhotinoDNR.udppacketsize];
		DatagramPacket sendPacket;
		
		sendData = (message).getBytes();
		
    	if ( tactiveip.size() > 0 )
    	{
	    	s = tactiveip.entrySet();
			it = s.iterator();
			while(it.hasNext())
			{
				m =(Map.Entry)it.next();
				ip = (String)m.getKey();
				try
				{
		    		sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), gPort);
		    		sOut.send(sendPacket);
		    	} 
		    	catch (IOException e) 
		    	{
		    		e.printStackTrace();
		    	}
			}
    	}
    	tactiveip.clear();
		
		return ret;
	}
	public Boolean SendTopicDomainUpdate(String topics, String ip)
	{
		//
		// this sends out the update of the registered topics to all active gateways and dnrs
		//
		Boolean ret = false;
		if ( !topics.equals("") )
		{
			String[] tlist = topics.split(",");
			String info2 = "";
			for ( int i=0; i < tlist.length; i++ )
			{
				if ( info2.equals("") ) info2 = tlist[i] + ":" + ip;
				else info2 = info2 + "," + tlist[i] + ":" + ip;
			}
			
			Map.Entry m;
			Map<String,String> tdnrdomain = new HashMap<String, String>();
			tdnrdomain.putAll(PhotinoDNR.dnrdomain);
			
			Set s = tdnrdomain.entrySet();
			Iterator it=s.iterator();
			
			byte[] sendData = new byte[PhotinoDNR.udppacketsize];
			
			sendData = ("81|"+tlist.length+"|"+info2).getBytes();
			
			//
			// this could be
			// SendToDNR("81|"+tlist.length+"|"+info2);
			
	    	if ( tdnrdomain.size() > 1 )
	    	{
		    	s = tdnrdomain.entrySet();
				it = s.iterator();
				while(it.hasNext())
				{
					m =(Map.Entry)it.next();
					ip = (String)m.getKey();
					if ( !ip.equals(PhotinoDNR.myIP) )
					{
						try
						{
							DatagramPacket sendPacketDnr = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), PhotinoDNR.dnrIn);
							PhotinoDNR.socketOutDnr.send(sendPacketDnr);
			    		} 
			    		catch (IOException e) 
			    		{
			    			e.printStackTrace();
			    		}
					}
				}
	    	}
	    	
	    	//
	    	// this sends the new topics to the ques
	    	//
	    	Map<String,String> tquedomain = new HashMap<String, String>();
			tquedomain.putAll(PhotinoDNR.quedomain);
	    	
			if ( tquedomain.size() > 0 )
			{

				int port = 59100; 
				String sendto = "";
				
				DatagramPacket sendPacket;
				s = tquedomain.entrySet();
				it=s.iterator();

				while(it.hasNext())
				{
				    m =(Map.Entry) it.next();
				    sendto = (String) m.getKey();
				    //String value=(String)m.getValue();
				    //if ( !sendto.equals(ip) )
				    //{
				    	try
				    	{
				    		sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(sendto), port);
				    		sOut.send(sendPacket);
				    	} 
				    	catch (IOException e) 
				    	{
				    		e.printStackTrace();
				    	}
				    	GUI.outputText("Sent Topics:"+info2+" to:"+sendto);
				    //}
				}
			}
	    	
	    	//
	    	// this sends the new topics to the gateways
	    	//
	    	Map<String,String> tactiveip = new HashMap<String, String>();
			tactiveip.putAll(PhotinoDNR.activeip);
	    	
			if ( tactiveip.size() > 0 )
			{

				int port = 53000; 
				String sendto = "";
				
				DatagramPacket sendPacket;
				s = tactiveip.entrySet();
				it=s.iterator();

				while(it.hasNext())
				{
				    m =(Map.Entry) it.next();
				    sendto = (String) m.getKey();
				    //String value=(String)m.getValue();
				    //if ( !sendto.equals(ip) )
				    //{
				    	try
				    	{
				    		sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(sendto), port);
				    		sOut.send(sendPacket);
				    	} 
				    	catch (IOException e) 
				    	{
				    		e.printStackTrace();
				    	}
				    	GUI.outputText("Sent Topics:"+info2+" to:"+sendto);
				    //}
				}
			}
			
			tdnrdomain.clear();
			tquedomain.clear();
			tactiveip.clear();
		}
		
		
		return ret;
		
	}
	public Boolean ReceiveKeepAlive(InetAddress IPAdr, String katype)
	{
		Boolean ret = false;
		String ipaddress = IPAdr.getHostAddress();
		int secs = (int) ((new Date().getTime())/1000);
GUI.outputText("KAType: "+katype+" ip: "+ipaddress);
		if ( katype.equals("G") )
		{
			if ( PhotinoDNR.activeip.containsKey(ipaddress) )
			{
				PhotinoDNR.activeip.put(ipaddress,Integer.toString(secs));
			}
		}
		if ( katype.equals("Q") )
		{
			if ( PhotinoDNR.quedomain.containsKey(ipaddress) )
			{
				PhotinoDNR.quedomain.put(ipaddress,Integer.toString(secs));
			}
		}
		ret = true;
		return ret;
	}
	public Boolean sendNotification(String token, String message)
	{
		Boolean ret = false;
		if ( !message.equals("") && !token.equals("") )
		{
			if ( PhotinoDNR.tokendevice.containsKey(token) )
			{
				String deviceid = GetDeviceInfoFromToken(token);
				// deviceid is returned as 73|TRUE|deviceid or FALSE if not found
				String status = deviceid.substring(deviceid.indexOf("|")+1);
				status = status.substring(0,status.indexOf("|"));
				if ( status.equals("TRUE") )
				{
					deviceid = deviceid.substring(deviceid.indexOf("|")+1);
					deviceid = deviceid.substring(deviceid.indexOf("|")+1);		
					String title = message.substring(0,message.indexOf("|"));
					message = message.substring(message.indexOf("|")+1);
					String url = PhotinoDNR.apiPath + "sendNotification.php";
					String param = "{\"title\":\""+title+"\",\"message\":\""+message+"\", \"deviceid\":\""+deviceid+"\"}";
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
						byte[] respb = new byte[PhotinoDNR.udppacketsize];
						response.read(respb);
						String resp = new String(respb);
						resp = resp.replaceAll("\u0000.*", "");
						ret = true;
					}
					catch (Exception e ) {}
				}
			}
		}
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
	public Boolean SendData(String content, String ip, int port)
	{
		Boolean ret = true;
		byte[] sendData = new byte[PhotinoDNR.udppacketsize];
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
