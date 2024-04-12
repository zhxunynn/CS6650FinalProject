package edu.northeastern.cs6650.Assignment4;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.swagger.client.api.ResortsApi;
import okhttp3.OkHttpClient;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The type Client app.
 */
public class ClientApp {
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int INITIAL_THREAD_COUNT = 32;
    private static final int TOTAL_REQUESTS = 200000;

    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final AtomicLong startTime = new AtomicLong(0);
    private static final AtomicLong endTime = new AtomicLong(0);
    private static final ResortsApi apiClient = new ResortsApi();

    /**
     * The constant processedRequests.
     */
    public static final AtomicInteger processedReqNum = new AtomicInteger(0);

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        BlockingQueue<ResortGetParam> queue = new LinkedBlockingQueue<>();
        startTime.set(System.currentTimeMillis());


        ThreadPoolExecutor producerExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(INITIAL_THREAD_COUNT);
        for (int i = 0; i < INITIAL_THREAD_COUNT; i++) {
            producerExecutorService.submit(new ResortDataProducer(queue, TOTAL_REQUESTS / INITIAL_THREAD_COUNT));
        }

        ThreadPoolExecutor consumerExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(INITIAL_THREAD_COUNT);
        ThreadPoolExecutor dynamicExecutorService = new ThreadPoolExecutor(
                INITIAL_THREAD_COUNT, // Start with INITIAL_THREAD_COUNT to allow immediate expansion
                Integer.MAX_VALUE, // Allow a large number of threads if needed
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000), // Direct hand-off
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        // Submit initial tasks to the initial pool
        for (int i = 0; i < INITIAL_THREAD_COUNT; i++) {
            consumerExecutorService.submit(new ResortDataConsumer(queue, REQUESTS_PER_THREAD, apiClient));
            processedReqNum.addAndGet(REQUESTS_PER_THREAD);
        }

        consumerExecutorService.shutdown();
        // Continuously check if more tasks can be submitted
        dynamicExecutorService.submit(() -> {
            try {
                while (processedReqNum.get() < TOTAL_REQUESTS) {
                    // Ensure not to exceed total request count
                    if (processedReqNum.get() + REQUESTS_PER_THREAD <= TOTAL_REQUESTS) {
                        dynamicExecutorService.submit(new ResortDataConsumer(queue, REQUESTS_PER_THREAD,apiClient));
                        processedReqNum.addAndGet(REQUESTS_PER_THREAD);
                    }
                }
            } finally {
                dynamicExecutorService.shutdown();
            }
        });

        // Wait for dynamic pool to complete all tasks
        try {
            dynamicExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            System.out.println("Largest Pool Size: " + dynamicExecutorService.getLargestPoolSize());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        endTime.set(System.currentTimeMillis());
        long totalRunTime = endTime.get() - startTime.get();
        double totalRunTimeInSeconds = totalRunTime / 1000.0;
        int totalProcessedRequests = successCount.get() + failureCount.get();
        double throughput = totalProcessedRequests / totalRunTimeInSeconds;

        System.out.println("Number of successful requests: " + successCount.get());
        System.out.println("Number of unsuccessful requests: " + failureCount.get());
        System.out.println("Total run time (seconds): " + totalRunTimeInSeconds);
        System.out.println("Throughput (requests/second): " + throughput);
    }

    /**
     * Increment success count.
     */
    public static void incrementSuccessCount() {
        successCount.incrementAndGet();
    }

    /**
     * Increment failure count.
     */
    public static void incrementFailureCount() {
        failureCount.incrementAndGet();
    }
}
