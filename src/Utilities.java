import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utilities {

    /**
     * Hash a string input with SHA1 method
     * @param input string input to be hashed
     * @return hashed string
     */
    public static BigInteger SHA1 (String input){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1"); //choose hashing function
            byte[] result = md.digest(input.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }

            return new BigInteger(sb.toString(), 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized void print(String str){
        System.out.println(str);
    }

    public static synchronized void printError(String str){
        System.err.println(str);
    }

}
