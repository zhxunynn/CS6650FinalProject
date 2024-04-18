package edu.northeastern.cs6650.Assignment4;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SkierDayProducer extends Producer {

    private final int THREAD_COUNT; // Number of threads in the pool

    private static AtomicInteger skierIdCounter = new AtomicInteger(1);

    private final int dayID;
    public SkierDayProducer(BlockingQueue<GetParam> queue, int totalEvents) {
        super(queue, totalEvents);
        this.THREAD_COUNT = Config.getThreadCount();
        this.dayID = Config.getDayID();
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < totalEvents; i++) {
            executor.submit(() -> {
                int skierID = skierIdCounter.incrementAndGet();
                int resortID = 1;
                int seasonID = 2024;

                GetParam param = new GetParam(resortID, seasonID, dayID, skierID);
                try {
                    queue.put(param);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        try {
            // Wait for all tasks to finish
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}