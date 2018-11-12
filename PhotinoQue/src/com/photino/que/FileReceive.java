package com.photino.que;

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
				byte data[] = new byte[PhotinoQue.udppacketsize]; // Here you can increase the size also which will receive it faster
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
					if ( filenametoreceive.equals("usertoken.ser") )
					{
						FileInputStream fis = new FileInputStream("usertoken.ser");
						ObjectInputStream ois = new ObjectInputStream(fis);
						PhotinoQue.usertoken = (ConcurrentHashMap) ois.readObject();
						ois.close();
		
						//
						// send request for next file
						//
						//SendData("86|Request |.ser",sip,randomQueport);
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
	
	public static Boolean SendData(String content, String sip, int port)
	{
		Boolean ret = true;
		try 
		{
			byte[] sendDataDnr = new byte[PhotinoQue.udppacketsize];
			sendDataDnr = (content).getBytes();
			DatagramPacket sendPacketDnr = new DatagramPacket(sendDataDnr, sendDataDnr.length, InetAddress.getByName(sip), port);
			PhotinoQue.socketOutDnr.send(sendPacketDnr);
			ret = true;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return ret;
	}
}

