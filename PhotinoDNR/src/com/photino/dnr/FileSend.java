package com.photino.dnr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FileSend 
{

	public static Boolean SendFile(String filename, int port)
	{
		Boolean ret = false;
		ServerSocket sendServer = null;
		Socket sendSocket;
		byte data[];
		FileDetails details; 
		try 
		{
			File file = new File(filename);

			// Getting file name and size
			if (file.length() > Integer.MAX_VALUE) 
			{
				System.out.println("File size exceeds 2 GB");
			} 
			else 
			{
				sendServer = new ServerSocket(port);
				GUI.outputText("Waiting for Client...");
				sendSocket = sendServer.accept();
				// File Object for accessing file Details
				GUI.outputText("Connected to Client...");
				data = new byte[PhotinoDNR.udppacketsize]; // Here you can increase the size also which will send it faster
				details = new FileDetails();
				details.setDetails(file.getName(), file.length());

            	// Sending file details to the client
            	GUI.outputText("Sending file details...");
            	ObjectOutputStream sendDetails = new ObjectOutputStream(sendSocket.getOutputStream());
            	sendDetails.writeObject(details);
            	sendDetails.flush();
            	// Sending File Data 
            	GUI.outputText("Sending file data...");
            	FileInputStream fileStream = new FileInputStream(file);
            	BufferedInputStream fileBuffer = new BufferedInputStream(fileStream);
            	OutputStream out = sendSocket.getOutputStream();
            	int count;
            	while ((count = fileBuffer.read(data)) > 0) 
            	{
            		GUI.outputText("Data Sent : " + count);
            		out.write(data, 0, count);
            		out.flush();
            	}
            	out.close();
            	fileBuffer.close();
            	fileStream.close();
            	sendSocket.close();
            	sendServer.close();

			}
		} 
		catch (Exception e) 
		{
			System.out.println("Error : " + e.toString());
		}
	
	return ret;
	}
}
