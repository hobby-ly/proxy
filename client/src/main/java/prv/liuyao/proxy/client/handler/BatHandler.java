package prv.liuyao.proxy.client.handler;

import java.io.*;

/**
 * @author zhangpl
 * @description StartBat
 * @date 2021/8/13 18:08
 */
public class BatHandler {

    public static void  callCmd(String locationCmd){
        StringBuilder sb = new StringBuilder();
        try {
            Process child = Runtime.getRuntime().exec(locationCmd);
            InputStream in = child.getInputStream();
            BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(in,"GBK"));
            String line;
            while((line=bufferedReader.readLine())!=null)
            {
                sb.append(line + "\n");
            }
            in.close();
            try {
                child.waitFor();
            } catch (InterruptedException e) {
                System.out.println(e);
            }
            System.out.println("sb:" + sb.toString());
//            System.out.println("callCmd execute finished");
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
//        String batPath ="C:/Users/zhangpl/Desktop/startPro.bat"; // 把你的bat脚本路径写在这里
        String batPath ="C:/Users/zhangpl/Desktop/stopPro.bat"; // 把你的bat脚本路径写在这里
        File batFile = new File(batPath);
        boolean batFileExist = batFile.exists();
        System.out.println("batFileExist:" + batFileExist);
//        if (batFileExist) {
//            callCmd(batPath);
//        }
    }

}
