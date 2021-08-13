package prv.liuyao.proxy.utils.test;

import prv.liuyao.proxy.utils.queue.LinkedMQ;
import prv.liuyao.proxy.utils.queue.ProxyMQ;
import prv.liuyao.proxy.utils.queue.SimpleDisruptor;

public class TestMQ {

    public static void main(String[] args) throws InterruptedException {

//        test0(new SimpleDisruptor<String>());
        test0(new LinkedMQ<>());

    }


    private static void test0(ProxyMQ<String> dis) throws InterruptedException {

        for (int n = 0; n < 2; n++) {
            Thread[] threads = new Thread[100];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 5; j++) {
                        dis.push(Thread.currentThread().getName() + " -> " + j);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }, "thread-" + n + "-" + i);
                threads[i].start();
            }
            dis.registryConsumer(o -> System.out.println(o + " --2"));
            dis.registryConsumer(o -> System.out.println(o));
//        dis.registryConsumer(o -> { });
            dis.start();
            for (Thread thread : threads) {
                thread.join();
            }

            Thread.sleep(10000);
        }

        dis.shutdown();
    }

}
