package edu.northeastern.cs6650.Assignment4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SkierVerticalProducer extends Producer {

    private final int THREAD_COUNT; // Number of threads in the pool

    private AtomicInteger skierIdCounter = new AtomicInteger(1);
    private List<String> resortList = new ArrayList<>();
    private List<String> seasonList = new ArrayList<>();


    public SkierVerticalProducer(BlockingQueue<GetParam> queue, int totalEvents) {
        super(queue, totalEvents);
        this.THREAD_COUNT = Config.getThreadCount();
        this.resortList.add("1");
        this.seasonList.add("2024");
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < totalEvents; i++) {
            executor.submit(() -> {
                int skierID = skierIdCounter.incrementAndGet();

                GetParam param = new GetParam(skierID,resortList,seasonList);
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