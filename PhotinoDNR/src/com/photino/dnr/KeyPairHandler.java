package com.photino.dnr;

import java.util.Timer;
import java.util.TimerTask;

//
// KeyPairProcessor class
//
class KeyPairHandler extends Thread 
{
	public int numOfKeyPairs = 50;
	public int maxNumToCreate = 10;
	
	// Constructor
	public KeyPairHandler() 
	{
		
		Timer timer = new Timer();
		int timeout = 35;// keep alive timeout in seconds
		
		//for ( int i=0; i < maxNumToCreate; ++i )
		//{
		//	GenerateKeyPair.CreateKeyPair();
		//}
		
		timer.scheduleAtFixedRate(new TimerTask() {
		  @Override
		  public void run() {
		    ManageKeyPairs();
		  }
		}, timeout*1000, timeout*1000);
	}

	public void ManageKeyPairs()
	{
		if ( PhotinoDNR.hasBeenValidated && PhotinoDNR.keypairs.size() < numOfKeyPairs )
		{
			int numToCreate = numOfKeyPairs - PhotinoDNR.keypairs.size();
			if ( numToCreate > maxNumToCreate ) numToCreate = maxNumToCreate;
			if ( numToCreate > 0 )
			{
				for ( int i = 0; i < numToCreate; ++ i )
				{
					GenerateKeyPair.CreateKeyPair();
				}
			}
			GUI.outputText("Keypairs in memory..."+Integer.toString(PhotinoDNR.keypairs.size()));
		}
	}
}
