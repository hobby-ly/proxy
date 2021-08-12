package prv.liuyao.proxy.utils.queue;

import java.util.function.Consumer;

public interface ProxyMQ<T> {

    void push(T t);

    public ProxyMQ<T> registryConsumer(Consumer<T> consumer);

    public void start();

    public void shutdown();

}
