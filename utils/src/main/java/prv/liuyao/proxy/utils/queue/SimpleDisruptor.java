package prv.liuyao.proxy.utils.queue;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SimpleDisruptor<T> {

    protected Disruptor<LyEvent<T>> disruptor;

    public SimpleDisruptor() {
        this(128);
    }

    public SimpleDisruptor(int ringBufferSize) {
        this.disruptor = new Disruptor(new EventFactory<LyEvent<T>>() {
            @Override
            public LyEvent<T> newInstance() {
                return new LyEvent<T>();
            }
        }, ringBufferSize, Executors.defaultThreadFactory());
        this.disruptor.setDefaultExceptionHandler(new ExceptionHandler<LyEvent<T>>() {
            @Override
            public void handleEventException(Throwable throwable, long l, LyEvent<T> tLyEvent) {
                System.out.println(tLyEvent.get());
                throwable.printStackTrace();
            }
            @Override
            public void handleOnStartException(Throwable throwable) {
                throwable.printStackTrace();
            }
            @Override
            public void handleOnShutdownException(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    public void push(T t) {
        // 生产者
        RingBuffer<LyEvent<T>> ringBuffer = this.disruptor.getRingBuffer();
        long sequence = ringBuffer.next(); // 下一个可用位置
        ringBuffer.get(sequence).set(t);
        ringBuffer.publish(sequence);
    }

    public SimpleDisruptor<T> registryConsumer(Consumer<T> consumer) {
        this.disruptor.handleEventsWith(new EventHandler<LyEvent<T>>() {
            @Override
            public void onEvent(LyEvent<T> tLyEvent, long l, boolean b) throws Exception {
                consumer.accept(tLyEvent.get());
            }
        });
        return this;
    }

    public void start() {
        this.disruptor.start();
    }

    public void shutdown() {
        this.disruptor.shutdown();
    }

    private class LyEvent<T> {
        private T t;

        public void set(T t) {
            this.t = t;
        }

        public T get() {
            return t;
        }
    }
}
