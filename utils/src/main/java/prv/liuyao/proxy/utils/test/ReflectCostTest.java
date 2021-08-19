package prv.liuyao.proxy.utils.test;

import io.netty.channel.*;
import prv.liuyao.proxy.utils.handler.CreatHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectCostTest {

    public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        String handlerClass = "prv.liuyao.proxy.utils.test.ReflectEntity";

        // 反射方式创建handler
        CreatHandler usedHandler;
        Constructor<ChannelHandler> handlerConstructor;
        try {
            Class aClass = Class.forName(handlerClass);
            Constructor constructor = aClass.getConstructor();
            Object handler = constructor.newInstance();
            if (!(handler instanceof ChannelHandler) || !(handler instanceof CreatHandler)) {
                System.out.println("handler class error: " + handler.getClass());
                return;
            }
            usedHandler = (CreatHandler) handler;
            handlerConstructor = constructor;
            System.out.println("use handler " + aClass.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        long loop = 100_0000;
        long l;
        Object o;

        l = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            o = usedHandler.newEntity();
        }
        System.out.println(System.currentTimeMillis() - l);

        l = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            o = new ReflectEntity();
        }
        System.out.println(System.currentTimeMillis() - l);

        l = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            o = handlerConstructor.newInstance();
        }
        System.out.println(System.currentTimeMillis() - l);

    }
}

