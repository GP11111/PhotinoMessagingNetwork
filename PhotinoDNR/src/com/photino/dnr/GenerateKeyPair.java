package com.photino.dnr;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;

public class GenerateKeyPair 
{
	//
	// create new public private key pair and store in keypairs
	//
	public static Boolean CreateKeyPair()
	{
		Boolean ret = false;
		try
		{
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(2048);
			KeyPair pair = gen.generateKeyPair();
			PublicKey pub = pair.getPublic();
			PrivateKey priv = pair.getPrivate();
	
			KeyFactory fact = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec spec = fact.getKeySpec(pub,X509EncodedKeySpec.class);
			String pub2String = Base64.getEncoder().encodeToString(spec.getEncoded());
			
			//System.out.println("pubkey="+pub2String);
		
			PhotinoDNR.keypairs.put(priv, pub2String);

			ret = true;
		}
		catch (Exception e) {}
		
		return ret;
	}
	public static PrivateKey GetPrivateKey()
	{
		PrivateKey privKey = null;
		if ( PhotinoDNR.keypairs.size() == 0 ) GenerateKeyPair.CreateKeyPair();
		//if ( PhotinoDNR.keypairs.size() > 0 )
		//{
			Random generator = new Random();
			Object[] values = PhotinoDNR.keypairs.keySet().toArray();// values().toArray();
			privKey = (PrivateKey) values[generator.nextInt(values.length)];
		//}
		return privKey;
	}
	public static String GetPubKey(PrivateKey privKey)
	{
		String pubKey = "";
		if ( privKey != null )
		{
			pubKey = (String) PhotinoDNR.keypairs.get(privKey);
			PhotinoDNR.keypairs.remove(privKey);
		}
		return pubKey;
	}
}
