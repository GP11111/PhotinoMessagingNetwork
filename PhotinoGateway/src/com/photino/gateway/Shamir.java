package com.photino.gateway;

import java.math.BigInteger;
import java.util.Random;

public final class Shamir {

    public final class SecretShare {
        public SecretShare(final int num, final BigInteger share) {
            this.num = num;
            this.share = share;
        }

        public int getNum() {
            return num;
        }

        public BigInteger getShare() {
            return share;
        }

        @Override
        public String toString() {
            return "SecretShare [num=" + num + ", share=" + share + "]";
        }

        private final int num;
        private final BigInteger share;
    }

    public Shamir(final int k, final int n) {
        this.k = k;
        this.n = n;

        random = new Random();
    }

    public SecretShare[] split(final BigInteger secret) {
        final int modLength = secret.bitLength() + 1;

        prime = new BigInteger(modLength, CERTAINTY, random);
        final BigInteger[] coeff = new BigInteger[k - 1];

        System.out.println("Prime Number: " + prime);

        for (int i = 0; i < k - 1; i++) {
            coeff[i] = randomZp(prime);
            System.out.println("a" + (i + 1) + ": " + coeff[i]);
        }

        final SecretShare[] shares = new SecretShare[n];
        for (int i = 1; i <= n; i++) {
            BigInteger accum = secret;

            for (int j = 1; j < k; j++) {
                final BigInteger t1 = BigInteger.valueOf(i).modPow(BigInteger.valueOf(j), prime);
                final BigInteger t2 = coeff[j - 1].multiply(t1).mod(prime);

                accum = accum.add(t2).mod(prime);
            }
            shares[i - 1] = new SecretShare(i - 1, accum);
            System.out.println("Share " + shares[i - 1]);
        }

        return shares;
    }

    public BigInteger getPrime() {
        return prime;
    }

    public BigInteger combine(final SecretShare[] shares, final BigInteger primeNum) {
        BigInteger accum = BigInteger.ZERO;
        for (int i = 0; i < k; i++) {
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i != j) {
                    num = num.multiply(BigInteger.valueOf(-j - 1)).mod(primeNum);
                    den = den.multiply(BigInteger.valueOf(i - j)).mod(primeNum);
                }
            }

            System.out.println("den: " + den + ", num: " + den + ", inv: " + den.modInverse(primeNum));
            final BigInteger value = shares[i].getShare();

            final BigInteger tmp = value.multiply(num).multiply(den.modInverse(primeNum)).mod(primeNum);
            accum = accum.add(primeNum).add(tmp).mod(primeNum);

            System.out.println("value: " + value + ", tmp: " + tmp + ", accum: " + accum);
        }

        System.out.println("The secret is: " + accum);

        return accum;
    }

    private BigInteger randomZp(final BigInteger p) {
        while (true) {
            final BigInteger r = new BigInteger(p.bitLength(), random);
            if (r.compareTo(BigInteger.ZERO) > 0 && r.compareTo(p) < 0) {
                return r;
            }
        }
    }

    private BigInteger prime;

    private final int k;
    private final int n;
    private final Random random;

    private static final int CERTAINTY = 50;

    public static void main(final String[] args) {
        //final Shamir shamir = new Shamir(11, 20);
        
/*        final Shamir shamir = new Shamir(3, 5);
        
        String key = "MIICXAIBAAKBgQDA/+RZFBMidVW95Od/w8O3QKlBINzKMF//DxDonL4Vt7ZnMoxo8cjJTia1mVSyBx0f46Q3eXQiyXKhYYyEwxuesFc5HT/Z1I7rUGHk7OToU24/lcJPvufZwb/jk/ft/8CDtBQsLJeUEzuFGF3/c4hZhcH3uIbNR3dUefsrGQWYwwIDAQABAoGAc7d5tCcjKd/sHsUmQCaiLDeqw4/ukZPbmtPvsWh5WBAkX0+hfDKrznb7IpriezNktQAKL/C0Pz9FaZlRZTCOV9ZDfOd33JuaGtN35ckkbSf+NiWAq927Japp4fHL6R1uQ1Kx3Io4L/b7kWospjfqlz+NPL7fjrnL+hFbXgnRNsECQQDp0kvZkkWqTFT5gIQwD3sxHrmEZw2UmntoeGujpCkjxt0oNXs5Qx4Up46uTnRKiKf5sin/iDF1cwgAWidE0BXRAkEA005Zgt5SzBMkbx/ciq4PkCGk7LX6L11BitXK6nBJIYHZGLCv9a8BSlMEv7t/Z0beZTdY/9YqjhLrU/MvmhmmUwJAe7sE2EVHCC8MaFtRl/0ZO4z+rsm5rgFxfH78tsOP4ZbCQRzL8ClMKbHhFuv9LdPSz5cwEkCHq5cLjOQoE4npgQJAFdJRjCJJiqmvaYpwzCGNmeTfk1J3s8x9qEjL28ocw0kVkmcsxMJ9758DLom+bnvzG6DLoCAN5P4vL0w97jwX4QJBAN4pKLmHK6gp6jmeK8R3JyDpaX3dj8bfj7igrzxCD337FhmS69wDciFtt76kr31OyPPsmQ6ISUqPhyStUlPOWJE=";
        //String key = "abdefert6723urjki456iuhnd78i9w32";
        //String key = "abdefert6723urjki456iuhnd78i9w32abdefert6723urjki456iuhnd78i9w64abdefert6723urjki456iuhnd78i9w96abdefert6723urjki456iuhnd78i9w12abdefert6723urjki456iuhnd78i9w32abdefert6723urjki456iuhnd78i9w64abdefert6723urjki456iuhnd78i9w96abdefert6723urjki456iuhnd78i9w25";
        BigInteger bi = new BigInteger(key.getBytes());
        Random randomkey = new Random();
        int modLength = bi.bitLength() + 1;
        BigInteger primebi = new BigInteger(modLength, CERTAINTY, randomkey);
        System.out.println("prime="+primebi);
        
        System.out.println("bi="+bi);
        String omsg = new String(bi.toByteArray());
        System.out.println(omsg);
        
        

        final BigInteger secret = new BigInteger("1234567890123456789012345678901234567890");
        //final BigInteger secret = new BigInteger("123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
        final SecretShare[] shares = shamir.split(bi);
        final BigInteger prime = shamir.getPrime();

        System.out.println("Share 0 = "+shares[0].getShare());
        
        SecretShare[] newshares = new SecretShare[4];
        //BigInteger share4 = new BigInteger("6159951456689143084395414169204219903076895543033991128071543035423641764438614368974235262224998059614956967193078345425644707123968360165541533567951363968533207942903662858713191672067167028294921069344430556118188165887938912930858847221321151753626206877699572119757579884292463986662524697334602415646824887395003250488228391365689238247591615756846507862303588574314094506433571129432162984383260132152185601102972424313484595116327520575186617606139876786436066511227835715939896906203947740483609305692390599247525746960116092806993503114965395129410513654982611986106490989655939478108247526534791160120456");
        BigInteger share0 = shares[0].getShare();
        newshares[0] = shamir.new SecretShare(0,share0);
        System.out.println("Share 0 = "+newshares[0]);
        
        final Shamir shamir2 = new Shamir(3, 5);
       
        final BigInteger result = shamir2.combine(shares, prime);
        
        String okey = new String(result.toByteArray());
        System.out.println(okey);
        
        //String y = "oiu291981u39u192u3198u389u28u389u";
        //BigInteger bi = new BigInteger(y, 36);
        //System.out.println(bi);
        //String x = bi.toString();
        //System.out.println(x);
        */


    }
}
