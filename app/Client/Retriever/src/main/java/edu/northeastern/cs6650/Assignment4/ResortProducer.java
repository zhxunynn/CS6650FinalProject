package edu.northeastern.cs6650.Assignment4;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ResortProducer extends Producer {
    private final int THREAD_COUNT; // Number of threads in the pool
    private final int dayID;

    private static AtomicInteger skierIdCounter = new AtomicInteger(1);

    public ResortProducer(BlockingQueue<GetParam> queue, int totalEvents) {
        super(queue, totalEvents);
        this.THREAD_COUNT = Config.getThreadCount();
        this.dayID = Config.getDayID();
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < totalEvents; i++) {
            executor.submit(() -> {
                int resortID =  1; // resortID to be constant as 1
                int seasonID = 2024; // seasonID to be constant as 2024
                int skierID = skierIdCounter.incrementAndGet();
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
