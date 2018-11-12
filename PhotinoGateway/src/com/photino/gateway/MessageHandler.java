package com.photino.gateway;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.photino.gateway.Shamir.SecretShare;


//
//Message Handler class
//
class MessageHandler
{
//class MessageHandler extends Thread
//{
//	final Map messages;
//	final String action;
//	final String message;
//	final String recipient;
	
//	public MessageHandler(Map messages, String action, String message, String recipient ) 
//	{
//		this.messages = messages;
//		this.action = action;
//		this.message = message;
//		this.recipient = recipient;
//	}
		
//	@Override
//	public void run() 
//	{
//		Boolean result = false;
//		String returnmessages;
//			if ( action.equals("ADD"))
//			{
//				result = AddMessage(recipient,message);
//			}
//			if ( action.equals("RETRIEVE"))
//			{
//				
//			}
//	}
	//
	// add and retrieve messages for a specified recipient
	//
	public static Boolean AddMessage(String recipient, String message)
	{
		Boolean ret = false;
		if ( PhotinoGateway.messages.containsKey(recipient) )
		{
			String existingmessages = (String) PhotinoGateway.messages.get(recipient);
			message = existingmessages + "|" + message;
		}
		PhotinoGateway.messages.put(recipient, message);
		PhotinoGateway.messageReceived = true;
		ret = true;
		return ret;
	}
	public static String GetMessages(String recipient)
	{
		//
		// returns the total number of messages for this recipient that are on this gateway
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
		int maxpacketsize = (PhotinoGateway.udppacketsize/1000) * 1000;
		String[] mlist;
		if ( PhotinoGateway.messages.containsKey(recipient) )
		{
			messagelist = (String) PhotinoGateway.messages.get(recipient);
			numberofmessages = messagelist.split("\\|").length;
			if ( messagelist.length() < maxpacketsize )
			{
				PhotinoGateway.messages.remove(recipient);
			}
			else
			{
				mlist = messagelist.split("\\|");
				messagelist = "";
				for ( int i=0; i < mlist.length; ++i )
				{
					if ( ( messagelist.length() + mlist[i].length() + 1 ) < maxpacketsize )
					{
						if ( messagelist.equals("") ) messagelist = mlist[i];
						else messagelist = messagelist + "|" + mlist[i];
					}
					else
					{
						for (int j=i; j < mlist.length; ++j )
						{
							if ( messagestokeep.equals("") ) messagestokeep = mlist[j];
							else messagestokeep = messagestokeep + "|" + mlist[j];
						}
						i = mlist.length + 1;
					}
				}
			}
		}
		
		if ( !messagestokeep.equals("") )
		{
			PhotinoGateway.messages.put(recipient, messagestokeep);
		}
		
		messagelist = numberofmessages + "|" + messagelist;
		if ( numberofmessages > 0 ) PhotinoGateway.messageReceived = true;
		return messagelist;
	}
	//
	// for transmission encrypted messages - before local decryption to determine recipient
	// 
	// Note these are separated - use AddTransmissionFragment for both keys and message
	//
	public static Boolean AddEMessage(String msgid, String message)
	{
		Boolean ret = false;
		if ( PhotinoGateway.emessages.containsKey(msgid) )
		{
			String existingmessages = (String) PhotinoGateway.emessages.get(msgid);
			message = existingmessages + "," + message;
		}
		PhotinoGateway.emessages.put(msgid, message);
		ret = true;
		return ret;
	}
	public static String GetEMessages(String msgid)
	{
		String messagelist = "";
		if ( PhotinoGateway.emessages.containsKey(msgid) )
		{
			messagelist = (String) PhotinoGateway.emessages.get(msgid);
			PhotinoGateway.emessages.remove(msgid);
		}
		
		return messagelist;
	}
	//
	// for transmission encrypted messages - the message and key fragments
	// 
	//
	public static String AddTranmissionFragment(String mtype, String msgid, String content)
	{
		String ret = "";
		int nowsec = (int)(new Date().getTime())/1000;
		String fragmentlist = "";
		String[] flist = null;
		String emsg = "";
GUI.outputText("Adding fragment type "+mtype);
		if ( mtype.equals("K") )
		{
			//Map<String,String> tkeyfragments = new HashMap<String, String>();
			//tkeyfragments.putAll(PhotinoGateway.keyfragments);
			//if ( tkeyfragments.containsKey(msgid) )
			if ( PhotinoGateway.keyfragments.containsKey(msgid) )
			{
				String existingfragments = (String) PhotinoGateway.keyfragments.get(msgid);
				content = existingfragments + "," + content;
			}
			//else
			//{
			//	content = Integer.toString(nowsec) + "," + content;
			//}
			PhotinoGateway.keyfragments.put(msgid, content);
			//tkeyfragments.clear();
GUI.outputText("Added fragment type "+mtype);
		}
		else if ( mtype.equals("M") )
		{
			if ( PhotinoGateway.emessages.containsKey(msgid) )
			{
				String existingmessages = (String) PhotinoGateway.emessages.get(msgid);
				content = existingmessages + "," + content;
			}
			//else
			//{
			//	content = Integer.toString(nowsec) + "," + content;
			//}
			PhotinoGateway.emessages.put(msgid, content);
GUI.outputText("Added fragment type "+mtype);
		}
		//
		// check to see if enough fragments and the transmission encryption message have arrived
		//
		int minnumoffragments = 5;
GUI.outputText("Getting fragment list ");
		fragmentlist = (String) PhotinoGateway.keyfragments.get(msgid);
		if ( !fragmentlist.equals(null) )
		{
			flist = fragmentlist.split(",");
GUI.outputText("Fragment: "+Integer.toString(flist.length)+" "+fragmentlist);
			if ( flist.length == minnumoffragments )
			{
				if ( PhotinoGateway.emessages.containsKey(msgid) )
				{
GUI.outputText("Got Fragmentation message: ");
					emsg = (String) PhotinoGateway.emessages.get(msgid);
					//ret = DecryptTransmissionMessage("SPLIT",msgid,emsg,flist);
					ret = DecryptTransmissionMessage("SHAMIR",msgid,emsg,flist);
					if ( !ret.substring(0,5).equals("FALSE") )
					{
						PhotinoGateway.keyfragments.remove(msgid);
						PhotinoGateway.emessages.remove(msgid);
					}
				}
			}
		}
		return ret;
	}
	//
	// To get the list of transmission key fragments only
	//
	public static String GetKeyFragments(String msgid)
	{
		String fragmentlist = "";
		String[] flist = null;
		if ( PhotinoGateway.keyfragments.containsKey(msgid) )
		{
			fragmentlist = (String) PhotinoGateway.keyfragments.get(msgid);
			flist = fragmentlist.split(",");
			if ( flist.length >= 3 )
			{
				PhotinoGateway.keyfragments.remove(msgid);
			}
			else
			{
				fragmentlist = "";
			}
		}
		
		return fragmentlist;
	}
	//
	// decrypt the transmission message using the transmission key fragments
	//
	public static String DecryptTransmissionMessage(String fragmentationtype, String msgid,String encmessage, String[] keys)
	{
		String ret = "";
		String keystring = "";
		String dmsg = "";
		String recipient = "FALSE";
		String decryptedmsg = "";
		//
		// decrypt transmission message
		// determine the recipient and original encrypted message
		//
		
		//
		// the following uses reconstruction of key via the key being split into 5 parts
		// all parts required to reconstruct
		//
		if ( fragmentationtype.equals("SPLIT") )
		{
			for ( int i=0; i < 5; ++i )
			{
				for ( int j=0; j < 5; ++j )
				{
					if ( keys[j].substring(keys[j].length()-1).equals(String.valueOf(i)) )
					{
						keystring = keystring + keys[j].substring(0,keys[j].length()-1);
						j = 5;
					}
				}
			}
		}
		//
		// The following uses Shamirs secret sharing to reconstruct the key
		// 3 fragments of 5 are required
		//
		if ( fragmentationtype.equals("SHAMIR") )
		{
			int numberOfShares = 5;
			int minimumShares = keys.length;
			Shamir shamir = new Shamir(3, 5);
			SecretShare[] shares = new SecretShare[numberOfShares];
			BigInteger[] phpbi = new BigInteger[numberOfShares];
			BigInteger tempsharebi = null;
			for (int i=0;i < minimumShares; ++i )
			{
				for ( int j=0; j < minimumShares; ++j )
				{
					if ( keys[j].substring(keys[j].length()-1).equals(String.valueOf(i)) )
					{
						keystring = keys[j].substring(0,keys[j].length()-1);
						tempsharebi = new BigInteger(keystring);
						shares[i] = shamir.new SecretShare(i,tempsharebi);
						
						j = minimumShares;
					}
				}
			}
			
			BigInteger prime = new BigInteger("76519765397073379826870597102711705607677685032930161372389654437165520395079812211499863424897779707419349496973992647895315598419715157623365351692117687730484703842543932965569422956799495357637818363534442035523994467036108030610654548130073434627660273669969638314873426659848427798238982713281282725135494159019290963562138582013187722565481276195182012387351830614116286028194031371566966417998831276624733639618641440018272665498291537129800305867578592465208269795769000824780605249373201924055480338930082928002971900372119917857384653600185925197043911018169466120864194960695330213691195716226170942815338580073158498402250968096306876817914614883926651760538992210356952236387291632752053889284248481952510007495132934558100846463697119283746263167223389811693509549691048740441988567676161540544993357722566944128844134757098195227140347176687406841719409908084541484793423079383546910957273800627125641332315194810697870702050447");
			BigInteger result = shamir.combine(shares, prime);

			keystring = new String(result.toByteArray());
			
		}
		
GUI.outputText("keystring="+keystring);
		//
		// public key string to publickey, then decrypt
		//
		try
		{
			Cipher cipher = Cipher.getInstance("RSA");
			byte[] pubData = Base64.getDecoder().decode(keystring);
			X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubData);
			KeyFactory pubFact3 = KeyFactory.getInstance("RSA");
			PublicKey pub = pubFact3.generatePublic(pubSpec);
			cipher.init(Cipher.DECRYPT_MODE, pub);
			recipient = encmessage.substring(0,encmessage.indexOf("||"));
			recipient = new String (cipher.doFinal(Base64.getDecoder().decode(recipient)), "UTF-8");
			//decryptedmsg = new String (cipher.doFinal(Base64.getDecoder().decode(encmessage)), "UTF-8");
			
		}
		catch (Exception e) {}
GUI.outputText("dmsg="+recipient);		
		try
		{
			//dmsg = "WINE007@WINE.COM||e1de9ba102b62617f8a36a3aaaebec9827a4e9ccb345ca10a198a6448f59615bc8c3533f7cdb87c73f25e51d5e5a947bae213b6d6579d94278185580b9fbd058d70ac4c3ea3d8938dc5c7c43345214ade5ba5457e4fd1e01499678a1bf24c833fc9a99a9c3b3c5292f6dc3229bab2769adc8ad65faa9e84081ddbceaaa9fc553beeb289266f642593b95d2ef680a1877a003efb5d1cf1e72a1445758dad7b603442ca94262c0d01a4844f90482698b73bffc095b8e3e768c9fac820b2cfdb99e23dff4f7e0f7a7fdfd583e4a27fc57028d6a2b77becb847db37f34286eb4a9b3";
			//dmsg = decryptedmsg;
			//recipient = dmsg.substring(0,dmsg.indexOf("||"));
			//dmsg = dmsg.substring(dmsg.indexOf("||")+2);
			dmsg = encmessage.substring(encmessage.indexOf("||")+2);
		}
		catch (Exception e)
		{
			recipient = "FALSE";
			dmsg = "";
		}
		
		ret = recipient+"|"+msgid+"||"+dmsg;
		
		return ret;
	}
	
	public static Boolean SendMessageFromQue(String ip, String messageid)
	{
		Boolean ret = false;
		
		if ( !ip.equals("") )
		{
			if ( PhotinoGateway.messageque.containsKey(messageid) )
			{
				String message = (String) PhotinoGateway.messageque.get(messageid);
				PhotinoGateway.messageque.remove(messageid);
				PhotinoGateway.messageQueued = true;
				Random generator = new Random();
				int port = generator.nextInt((33009 - 33000) + 1) + 33000;
				int socketnum = generator.nextInt(PhotinoGateway.maxnumOut+1);
				try
				{
					byte[] sendData = new byte[PhotinoGateway.udppacketsize];
					sendData = ("20|"+message).getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), port);
					PhotinoGateway.socketOut[socketnum].send(sendPacket);
				} 
				catch (IOException e) {e.printStackTrace();}
				//
				// now add topic domain resolution to local cache maps
				//
				String domain = message.substring(0,message.indexOf("|"));
				domain = domain.substring(domain.indexOf("@")+1);
				PhotinoGateway.topicdomain.put(domain,ip);
				GUI.outputText("Added domain: "+domain+"::"+ip);
			}
		}
		
		return ret;
	}

}