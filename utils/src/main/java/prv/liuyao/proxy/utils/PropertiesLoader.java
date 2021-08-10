package prv.liuyao.proxy.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置文件加载类
 * 优先级：jar包相对路径 > jar包相对路径/config > jar包相对路径/conf > jar包内部
 */
public class PropertiesLoader {

    private static final String DEFAULT_PROP_NAME = "application.properties";
    protected static final Map<String, Properties> PROPS = new ConcurrentHashMap<>();
    private static final String STARTER_PATH = new File("").getAbsolutePath() + File.separator;
//    private static final String JAR_PATH = getLastFolderFromPath(
//            PropertiesLoader.class.getClassLoader().getResource("META-INF").getPath()) + File.separator;
    private static final String[] RELATIVE_PATH = {"", "config" + File.separator, "conf" + File.separator};

    static {

//        System.out.println("JAR_PATH: " + JAR_PATH);
//        System.out.println("STARTER_PATH: " + STARTER_PATH);
//
//        System.out.println();
//        System.out.println("System.getProperty(\"java.class.path\"): " + System.getProperty("java.class.path"));
//        System.out.println("System.getProperty(\"path.separator\"): " + System.getProperty("path.separator"));
//        System.out.println("System.getProperty(\"java.ext.dirs\"): " + System.getProperty("java.ext.dirs"));
//        System.out.println("new File(\"" + File.separator + "\").getAbsolutePath(): " + new File(File.separator).getAbsolutePath());
//        //启动脚本所在路劲
//        System.out.println("new File(\"\").getAbsolutePath(): " + new File("").getAbsolutePath());
//        //启动脚本所在路劲
//        System.out.println("System.getProperty(\"user.dir\"): " + System.getProperty("user.dir"));
//        System.out.println("File.separator: " + File.separator);
//
//        System.out.println();
//        // 打印 file:/D:/PROJECT/javatest/target/demo-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes!/
//        System.out.println(PropertiesLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
//        System.out.println(PropertiesLoader.class.getClassLoader().getResource("").getPath());
//        System.out.println();
//
//        System.out.println("getLastFolderFromPath(): " +
//                getLastFolderFromPath("file:/D:/PROJECT/javatest/target/demo-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes!/"));
//        String path = PropertiesLoader.class.getClassLoader().getResource("").getPath();
//        path = getLastFolderFromPath(path);
//        System.out.println("getLastFolderFromPath(): " + path);
//        System.out.println("==================== static {} end ================================");

    }

    public static String getLastFolderFromPath(String path){
        File file = new File(path);
        if (file.isDirectory()) { return file.getAbsolutePath(); }
        while (null != file.getParentFile()){
            file = file.getParentFile();
//            System.out.println(file.getPath());
            if (file.isDirectory()) { return file.getAbsolutePath(); }
        }
        String path1 = path.replaceFirst(file.getPath(), "");
        return getLastFolderFromPath(path1);
    }

    public static Properties getProperties(String propName){
        if (null == PROPS.get(propName)){
            synchronized (PROPS){
                if (null == PROPS.get(propName)){
                    while (propName.startsWith("/")) { propName = propName.substring(1); }
                    while (propName.startsWith("\\")) { propName = propName.substring(1); }
                    File file = new File(STARTER_PATH + propName);
                    for (int i = 0; i < RELATIVE_PATH.length; i++) {
                        if (file.exists()) break;
                        file = new File(STARTER_PATH + RELATIVE_PATH[i] + propName);
                    }
                    InputStream is = null;
                    try {
                        if (file.exists()){
                            // 从jar包相对位置加载
                            System.out.println("load properties：" + file.getAbsolutePath());
                            is = new FileInputStream(file);
                        } else {
                            // 从class 文件处加载文件 jar包内的也可以
                            is = PropertiesLoader.class.getClassLoader().getResourceAsStream(propName);
                            if (is == null) {
                                is = PropertiesLoader.class.getClassLoader().getParent().getResourceAsStream(propName);
                            }
                        }
                        Properties p = new Properties();
                        p.load(is);
                        PROPS.put(propName, p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (null != is) is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return PROPS.get(propName);
    }

    public static String getString(String key) {
        return getProperties(DEFAULT_PROP_NAME).getProperty(key);
    }

    public static int getInteger(String key) {
        return Integer.parseInt(getProperties(DEFAULT_PROP_NAME).getProperty(key));
    }

}
