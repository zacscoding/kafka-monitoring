package demo.worker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractWorker {

    protected final AtomicBoolean running = new AtomicBoolean(false);
    protected final String name;
    protected final long intervalMills;
    protected Thread thread;
    protected CountDownLatch terminationLatch;

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new RuntimeException("Already worker is running");
        }

        terminationLatch = new CountDownLatch(1);
        thread = new Thread(() -> {
            try {
                while (running.get()) {
                    try {
                        workInternal();
                        if (intervalMills != 0L) {
                            TimeUnit.MILLISECONDS.sleep(intervalMills);
                        }
                    } catch (InterruptedException e) {
                        logger.info("{} try to stop", name);
                        return;
                    }
                }
            } finally {
                onStop();
                terminationLatch.countDown();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            throw new RuntimeException("Already worker was stopped");
        }

        try {
            terminationLatch.await();
        } catch (InterruptedException e) {
            logger.warn("InterruptedException exception occur while waiting termination latch", e);
        }
    }

    public String getName() {
        return name;
    }

    protected abstract void workInternal();

    protected abstract void onStop();
}
