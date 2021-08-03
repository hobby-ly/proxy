package prv.liuyao.proxy.utils;

import javax.crypto.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AES加密工具
 */
public class AESUtil {

    /** 密钥长度: 128, 192 or 256 */
    private static final int KEY_SIZE = 128;
    /** 加密/解密算法名称 */
    private static final String ALGORITHM = "AES";
    /** 随机数生成器（RNG）算法名称 */
    private static final String RNG_ALGORITHM = "SHA1PRNG";

    private Cipher encryptCipher;
    private Cipher decryptCipher;

    public AESUtil(String key) {
        // 创建安全随机数生成器
        try {
            SecureRandom random = SecureRandom.getInstance(RNG_ALGORITHM);
            // 设置 密钥key的字节数组 作为安全随机数生成器的种子
            random.setSeed(key.getBytes());
            // 创建 AES算法生成器
            KeyGenerator gen = KeyGenerator.getInstance(ALGORITHM);
            // 初始化算法生成器
            gen.init(KEY_SIZE, random);
            // 生成 AES密钥对象, 也可以直接创建密钥对象: return new SecretKeySpec(key, ALGORITHM);
            SecretKey secretKey = gen.generateKey();

            // 加密器
            this.encryptCipher = Cipher.getInstance(ALGORITHM);
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // 解密器
            this.decryptCipher = Cipher.getInstance(ALGORITHM);
            this.decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public byte[] encrypt(byte[] bytes) {
        return doFinal(this.encryptCipher, bytes);
    }

    public byte[] decrypt(byte[] bytes) {
        return doFinal(this.decryptCipher, bytes);
    }

    private byte[] doFinal(Cipher cipher, byte[] data) {
        try {
            return cipher.doFinal(data);

//            byte[] buf = new byte[1024];
//            cipher.update(buf, 0, len);
//            cipher.doFinal();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }


    public static void main(String[] args) throws InterruptedException {
        String uid = UUID.randomUUID().toString();
        final AESUtil aesUtil = new AESUtil(uid);

        Map<String, byte[][]> result = new HashMap<>();

        Thread[] threads = new Thread[10_0000];
        for (int i = 0; i < threads.length; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> test(result, aesUtil, "hello"+ finalI));
        }

        long t = System.currentTimeMillis();
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        long cost = System.currentTimeMillis() - t;

        printResult(result);

        System.out.println(threads.length + "次 花费(ms):" + cost);
        System.out.println(uid);

//        ExecutorService exe = Executors.newCachedThreadPool();
//        for (int i = 0; i < 100; i++) {
//            int finalI = i;
//            exe.submit(() -> test(aesUtil, "hello"+ finalI));
//        }

    }

    public static void printResult(Map<String, byte[][]> result) {
        for (String str : result.keySet()) {
            System.out.print(str);
            System.out.print(" -加密-> ");
            System.out.print(new String(result.get(str)[0]));
            System.out.print(" -解密-> ");
            System.out.println(new String(result.get(str)[1]));
        }
    }

    public static void test(Map<String, byte[][]> result, AESUtil aesUtil, String str) {
        /*
        并不是每个字节数和编码集上的字符都有对应关系,如果一个字节数在编码集上没有对应,
        编码new String(byte[]) 后,往往解出来的会是一些乱码无意义的符号:例如:��
         */
        byte[] encryptByte = aesUtil.encrypt(str.getBytes());
        byte[] decryptByte = aesUtil.decrypt(encryptByte);
        result.put(str, new byte[][]{encryptByte, decryptByte});
    }
}