package cn.workbench;
import java.time.Instant;import java.security.*;import javax.crypto.*;import javax.crypto.spec.*;import java.nio.ByteBuffer;import java.util.*;
public final class Totp {
 private static final String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
 public static String secret(){byte[] b=new byte[20];new SecureRandom().nextBytes(b);StringBuilder s=new StringBuilder();int buffer=0,bits=0;for(byte x:b){buffer=(buffer<<8)|(x&255);bits+=8;while(bits>=5){bits-=5;s.append(ALPHABET.charAt((buffer>>bits)&31));}}return s.toString();}
 static byte[] decode(String s){byte[] out=new byte[s.length()*5/8];int b=0,n=0,i=0;for(char c:s.toUpperCase().toCharArray()){int v=ALPHABET.indexOf(c);if(v<0)throw new IllegalArgumentException();b=(b<<5)|v;n+=5;if(n>=8){n-=8;out[i++]=(byte)(b>>n);}}return out;}
 public static String code(String secret,long step){try{Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(decode(secret),"HmacSHA1"));byte[] h=mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());int o=h[19]&15;int n=((h[o]&127)<<24)|((h[o+1]&255)<<16)|((h[o+2]&255)<<8)|(h[o+3]&255);return String.format("%06d",n%1000000);}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
 public static long verify(String secret,String supplied,Instant now,long last){if(supplied==null||!supplied.matches("[0-9]{6}"))return -1;long current=now.getEpochSecond()/30;for(long step=current-1;step<=current+1;step++)if(step>last&&MessageDigest.isEqual(code(secret,step).getBytes(),supplied.getBytes()))return step;return -1;}
}
