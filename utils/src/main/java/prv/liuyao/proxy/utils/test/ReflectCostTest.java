package prv.liuyao.proxy.utils.test;

import io.netty.channel.*;
import prv.liuyao.proxy.utils.handler.CreatHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectCostTest {

    public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        int loop = 100_0000;
        testNew(loop);
        testReflectNew(loop);
        testReflectNewImpl(loop);

        System.out.println("--------------");

        testNew(loop);
        testReflectNew(loop);
        testReflectNewImpl(loop);

    }

    static void testNew(int loop) {
        Object o;
        long l = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            o = new ReflectEntity();
        }
        System.out.println("testNew cost ms: " + (System.currentTimeMillis() - l));
    }

    static void testReflectNew(int loop) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        String handlerClass = "prv.liuyao.proxy.utils.test.ReflectEntity";

        // 反射方式创建handler
        Constructor<ChannelHandler> handlerConstructor;
        try {
            Class aClass = Class.forName(handlerClass);
            Constructor constructor = aClass.getConstructor();
            handlerConstructor = constructor;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Object o;
        long l = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            o = handlerConstructor.newInstance();
        }
        System.out.println("testReflectNew cost ms: " + (System.currentTimeMillis() - l));
    }

    static void testReflectNewImpl(int loop) {
        String handlerClass = "prv.liuyao.proxy.utils.test.ReflectEntity";
        // 反射方式创建handler
        CreatHandler usedHandler;
        try {
            Class aClass = Class.forName(handlerClass);
            Constructor constructor = aClass.getConstructor();
            usedHandler = (CreatHandler) constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Object o;
        long l = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            o = usedHandler.newEntity();
        }
        System.out.println("testReflectNewImpl cost ms: " + (System.currentTimeMillis() - l));
    }
}

