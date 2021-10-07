package prv.liuyao.proxy.utils.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LinkedMQ<T> implements ProxyMQ<T> {

    private BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private List<Consumer<T>> consumer = new ArrayList<>();
    private Thread consumerThread = new Thread(() -> {
        try {
            T poll;
            for (;;) {
                poll = this.queue.poll(1, TimeUnit.SECONDS);
                if (null == poll) {
                    continue;
                }
                for (Consumer<T> c : consumer) {
                    c.accept(poll);
                }
            }
        } catch (InterruptedException e) {

        }
    });

    @Override
    public void push(T t) {
        this.queue.add(t);
    }

    @Override
    public ProxyMQ<T> registryConsumer(Consumer<T> consumer) {
        this.consumer.add(consumer);
        return this;
    }

    @Override
    public void start() {
        if (!this.consumerThread.isAlive()) {
            this.consumerThread.start();
        }
    }

    @Override
    public void shutdown() {
        if (!this.consumerThread.isInterrupted()) {
            this.consumerThread.interrupt();
        }
    }
}
