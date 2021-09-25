package demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class TempTest {

    @Test
    public void testTemp() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Future<?> worker1 = executorService.submit(new ProduceWorker("Worker-1"));
        Future<?> worker2 = executorService.submit(new Worker("Worker-2"));

        TimeUnit.SECONDS.sleep(5L);
        worker1.cancel(true);

        TimeUnit.SECONDS.sleep(5L);
    }

    public static abstract class AbstractWorker implements Runnable {

        protected String name;

        public AbstractWorker(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    runInternal();
                    TimeUnit.SECONDS.sleep(1L);
                }
            } catch (InterruptedException e) {
                System.out.printf("[%s] InterruptedException occur: %s\n", name, e.getMessage());
            }
        }

        protected abstract void runInternal();
    }

    public static class ProduceWorker extends AbstractWorker {
        public ProduceWorker(String name) {
            super(name);
        }

        @Override
        protected void runInternal() {
            System.out.printf("[%s] working..\n", name);
        }
    }

    public static class Worker implements Runnable {
        private String name;

        public Worker(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("Working.. " + name);
                    TimeUnit.SECONDS.sleep(1L);
                }
            } catch (InterruptedException e) {
                System.out.println("InterruptedException occur: " + e.getMessage());
            }
        }
    }
}

