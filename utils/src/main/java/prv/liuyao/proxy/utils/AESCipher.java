package prv.liuyao.proxy.utils;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * AES 加密类
 */
public class AESCipher {

//    public static final AESCipher COMMON = new AESCipher(SpringInit.getEnvironment().getProperty("sso.3rd.aes.key"));

    private Cipher encryptCipher;
    private Cipher decryptCipher;

    public AESCipher(String key) {
        try {
            // 创建安全随机数生成器
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            // 设置 密钥key的字节数组 作为安全随机数生成器的种子
            random.setSeed(key.getBytes());
            // 创建 AES算法生成器
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            // 初始化算法生成器 密钥长度: 128, 192 or 256
            gen.init(128, random);
            // 生成 AES密钥对象, 也可以直接创建密钥对象: return new SecretKeySpec(key, ALGORITHM);
            SecretKey secretKey = gen.generateKey();
            // 加密器
            this.encryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            // 解密器
            this.decryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            this.decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public byte[] encrypt(byte[] bytes) throws Exception {
        return this.encryptCipher.doFinal(bytes);
    }

    public byte[] decrypt(byte[] bytes) throws Exception {
        return this.decryptCipher.doFinal(bytes);
    }

    public static String byteArray2HexStr(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < buf.length; ++i) {
            String hex = Integer.toHexString(buf[i] & 255);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    public static byte[] hexStr2ByteArray(String hexStr) {
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0, j = 0; i < hexStr.length(); i+=2) {
            int high = hexToInt(hexStr.charAt(i));
            int low = hexToInt(hexStr.charAt(i+1));
            result[j++] = (byte)(high * 16 + low);
        }
        return result;
    }

    public static int hexToInt(char c) {
        int result;
        char[][] judge = {{48, 10, 0}, {65, 6, 10}, {97, 6, 10}};
        for (char[] j : judge) {
            result = c - j[0];
            if (result > -1 && result < j[1]){
                return result + j[2];
            }
        }
        throw new RuntimeException(c + " is not a hex str.");
    }

    public static char intToHex(int c) {
        if (c < 0 || c > 15) {
            throw new RuntimeException(c + " is not a hex number");
        }
        if (c < 10) {
            return (char) (c + 48);
        }
        return (char) (c + 55);
    }

    public static String byteToHex(byte c) {
        int n = c < 0 ? c + 256 : c;
        char[] hex = {intToHex(n >> 4), intToHex(c& 15)};
        return new String(hex);
    }

    public String encryptStr(String str) {
        try {
            return byteArray2HexStr(encrypt(str.getBytes("utf-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String decryptStr(String hexStr) {
        try {
            return new String(decrypt(hexStr2ByteArray(hexStr)), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) throws Exception {

        String hexStr = "134769873649817346ABDCEFADFCB";
//        byte[] bytes = hexStr2ByteArray(hexStr);
//        String s = byteArray2HexStr(bytes);
//        System.out.println(hexStr);


        AESCipher cipher = new AESCipher("adadad");
        String str, encrypt, decrypt;

        for (int i = 0; i < 100000; i++) {

            str = UUID.randomUUID().toString();
//        byte[] encrypt = cipher.encrypt(str.getBytes("utf-8"));
//        printArray(encrypt);
//        String s = byteArray2HexStr(encrypt);
//        System.out.println(s);
//        byte[] bytes = hexStr2ByteArray(s);
//        printArray(bytes);
//        byte[] decrypt = cipher.decrypt(encrypt);
//        System.out.println(new String(decrypt, "utf-8"));

//            System.out.println(str);
            encrypt = cipher.encryptStr(str);
//            System.out.println(str);
            decrypt = cipher.decryptStr(encrypt);
//            System.out.println(decrypt);
            if (!str.equals(decrypt)) {
                System.out.println(str + " vs " + decrypt);
            }
        }

//        System.out.println("0: " + (int) '0');
//        System.out.println("A: " + (int) 'A');
//        System.out.println("a: " + (int) 'a');
//
//        System.out.println(hexToInt('A'));
//        System.out.println(hexToInt('F'));
//        System.out.println(hexToInt('e'));
//        System.out.println(hexToInt('5'));

        int hex = 123;
//        System.out.println(Integer.toHexString(hex));
//
//        System.out.println(Integer.parseInt("0f", 16));

        for (int i = 0; i < 16; i++) {
//            System.out.println(intToHex(i) + " vs " + Integer.toHexString(i));
        }

//        System.out.println((byte) 128);
//
//        System.out.println(-111 % 16);
//        System.out.println(-111 / 16);

        for (int i = 0; i < 256; i++) {
            String s1 = byteToHex((byte) i);
            String s2 = Integer.toHexString(i);
            if (!s1.equalsIgnoreCase(s2)) {
//                System.out.println(i + ": " + s1 + " vs2 " + s2);
            }
        }
        for (int i = -128; i < 256; i++) {
//            System.out.println(i + ": " + (i >> 4) + " / " + i / 16 + " / " + (i & 15) + " / " + i % 16 + " / " + ((i + 256) & 15));
        }
        int i = 'F' - 12;
        byte aa = 12;
        int i1 = aa * 16 + aa;
    }

    static void printArray(byte[] arr) {
        for (byte b : arr) {
            System.out.print(b);
            System.out.print("\t");
        }
        System.out.println();
    }
}
