package com.photino.dnr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FileReceive 
{

	public static Boolean ReceiveFile(String filenametoreceive, String sip, int port)
	{
		Boolean ret = false;
		try 
		{
			GUI.outputText("Connecting to Server...");
			Socket receiveSocket = new Socket(sip, port);
			GUI.outputText("Connected to Server...");
			// Getting file details

			GUI.outputText("Getting details from Server...");
			ObjectInputStream getDetails = new ObjectInputStream(receiveSocket.getInputStream());
			FileDetails details = (FileDetails) getDetails.readObject();
			GUI.outputText("Now receiving file..."+details.getName());
			// Storing file name and sizes

			String fileName = details.getName();
			if ( fileName.equals(filenametoreceive) )
			{			
				GUI.outputText("File Name : " + fileName);
				byte data[] = new byte[PhotinoDNR.udppacketsize]; // Here you can increase the size also which will receive it faster
				FileOutputStream fileOut = new FileOutputStream(fileName);
				InputStream fileIn = receiveSocket.getInputStream();
				BufferedOutputStream fileBuffer = new BufferedOutputStream(fileOut);
				int count;
				int sum = 0;
				while ((count = fileIn.read(data)) > 0) 
				{
					sum += count;
					fileBuffer.write(data, 0, count);
					GUI.outputText("Data received : " + sum);
					fileBuffer.flush();
				}
				GUI.outputText("File Received Completed...");
				fileBuffer.close();
				fileIn.close();

				
				File f = new File(filenametoreceive);
				if( f.exists() && !f.isDirectory()) 
				{
					if ( filenametoreceive.equals("userregistrydid.ser") )
					{
						FileInputStream fis = new FileInputStream("userregistrydid.ser");
						ObjectInputStream ois = new ObjectInputStream(fis);
						PhotinoDNR.userregistrydid = (ConcurrentHashMap) ois.readObject();
						ois.close();
						//
						// now update the userregistry based on the userregistrydid file received from the DNR seed
						//
						PhotinoDNR.userregistry.clear();
						String did, usertaglist;
						String[] usertag;
						Map<String,String> tuserregistrydid = new HashMap<String, String>();
						tuserregistrydid.putAll(PhotinoDNR.userregistrydid);
						Set s = tuserregistrydid.entrySet();
						Iterator it = s.iterator();
						s = tuserregistrydid.entrySet();
						it = s.iterator();
						while(it.hasNext())
						{
						    Map.Entry m =(Map.Entry)it.next();
						    did = (String)m.getKey();
						    usertaglist = (String)m.getValue();
						    usertag = usertaglist.split(",");
						    for ( int i=0; i < usertag.length; ++i )
						    {
						    	PhotinoDNR.userregistry.put(usertag[i], did);
						    }
						}
						FileOutputStream fos;
						fos = new FileOutputStream("userregistry.ser");
						ObjectOutputStream oos = new ObjectOutputStream(fos);
						oos.writeObject(PhotinoDNR.userregistry);
						oos.close();
						
						//
						// now request the tokendevice file from the dnr - 88
						//
						
						SendData("86|Request tokendevice|tokendevice.ser");
					}
					
					if ( filenametoreceive.equals("tokendevice.ser") )
					{
						// now read entries and decrypt from origin and encrypt local
						FileInputStream fis = new FileInputStream("tokendevice.ser");
						ObjectInputStream ois = new ObjectInputStream(fis);
						ConcurrentHashMap<String,String> ttokendevice = (ConcurrentHashMap) ois.readObject();
						ois.close();
						
						Set s = ttokendevice.entrySet();
						Iterator it = s.iterator();
						String token = "";
						String content = "";
						String[] contentlist;
						String createtime = "";
						String edeviceid = "";
						String deviceid = "";
						String keyst = "";
						Cipher c;
						while(it.hasNext())
						{
						    Map.Entry m =(Map.Entry)it.next();
						    token = (String)m.getKey();
						    content = (String) m.getValue();
						    contentlist = content.split(",");
						    createtime = contentlist[0];
						    edeviceid = contentlist[1];
							if ( !token.equals("") && !createtime.equals("") && !edeviceid.equals("") )
							{
								//decrypt edeviceid
								keyst = sip;
								keyst = keyst.replace(".", "");
								while ( keyst.length() < 16 ) keyst = keyst + keyst;
								keyst = keyst.substring(0,16);
								byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
								IvParameterSpec ivSpec = new IvParameterSpec(iv);
								SecretKeySpec skeySpec = new SecretKeySpec(keyst.getBytes("UTF-8"), "AES");
								c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
								c.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
								byte[] original = c.doFinal(Base64.getDecoder().decode(edeviceid));
								deviceid = new String(original); 
								
								//encrypt local
								keyst = PhotinoDNR.myIP;
								keyst = keyst.replace(".", "");
								while ( keyst.length() < 16 ) keyst = keyst + keyst;
								keyst = keyst.substring(0,16);
								ivSpec = new IvParameterSpec(iv);
								skeySpec = new SecretKeySpec(keyst.getBytes("UTF-8"), "AES");
								c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
								c.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
								byte[] encrypted = c.doFinal(deviceid.getBytes());
								deviceid = Base64.getEncoder().encodeToString((encrypted));
								
								ttokendevice.put(token, createtime+","+deviceid);
							}
						}
						
						PhotinoDNR.tokendevice.clear();
						PhotinoDNR.tokendevice.putAll(ttokendevice);
						
						ttokendevice.clear();
						
						FileOutputStream fos;
						fos = new FileOutputStream("tokendevice.ser");
						ObjectOutputStream oos = new ObjectOutputStream(fos);
						oos.writeObject(PhotinoDNR.tokendevice);
						oos.close();
						
						//
						// now request the userpublickey file from the dnr - 88
						//
						
						SendData("86|Request UserPublicKey|userpublickey.ser");
					}
					
					if ( filenametoreceive.equals("userpublickey.ser") )
					{
						FileInputStream fis = new FileInputStream("userpublickey.ser");
						ObjectInputStream ois = new ObjectInputStream(fis);
						PhotinoDNR.userpublickey = (ConcurrentHashMap) ois.readObject();
						ois.close();
					}
				}
			}
			receiveSocket.close();
		} 
		catch (Exception e) 
		{
			System.out.println("Error : " + e.toString());
		}
		
		return ret;
	}
	
	public static Boolean SendData(String content)
	{
		Boolean ret = true;
		try 
		{
			byte[] sendDataDnr = new byte[PhotinoDNR.udppacketsize];
			sendDataDnr = (content).getBytes();
			DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(PhotinoDNR.dnrSeed), PhotinoDNR.dnrIn);
			PhotinoDNR.socketOutDnr.send(sendPacketDnr);
			ret = true;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return ret;
	}
}
