package part2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

  /**
   * The constant processedRequests.
   */
  public static final AtomicInteger processedRequests = new AtomicInteger(0);


  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    BlockingQueue<Ride> queue = new LinkedBlockingQueue<>();
    startTime.set(System.currentTimeMillis());

    Thread producerThread = new Thread(new LiftRideProducer(queue, TOTAL_REQUESTS));
    producerThread.start();

    ThreadPoolExecutor initialExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(
        INITIAL_THREAD_COUNT);
    ThreadPoolExecutor dynamicExecutorService = new ThreadPoolExecutor(
        INITIAL_THREAD_COUNT, // Start with INITIAL_THREAD_COUNT to allow immediate expansion
        Integer.MAX_VALUE, // Allow a large number of threads if needed
        60L, TimeUnit.SECONDS,
        new SynchronousQueue<>(), // Direct hand-off
        Executors.defaultThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy());
    // Submit initial tasks to the initial pool
    for (int i = 0; i < INITIAL_THREAD_COUNT; i++) {
      initialExecutorService.submit(new part2.LiftRideConsumer(queue, REQUESTS_PER_THREAD));
      processedRequests.addAndGet(REQUESTS_PER_THREAD);
      logPerformanceData("ThroughPutPlot.csv",processedRequests.get(),((System.currentTimeMillis() - startTime.get())/1000.0));
    }

    initialExecutorService.shutdown();
    // Continuously check if more tasks can be submitted
    dynamicExecutorService.submit(() -> {
      try {
        while (processedRequests.get() < TOTAL_REQUESTS) {
          // Ensure not to exceed total request count
          if (processedRequests.get() + REQUESTS_PER_THREAD <= TOTAL_REQUESTS) {
            dynamicExecutorService.submit(new part2.LiftRideConsumer(queue, REQUESTS_PER_THREAD));
            processedRequests.addAndGet(REQUESTS_PER_THREAD);
            logPerformanceData("ThroughPutPlot.csv",processedRequests.get(),((System.currentTimeMillis()-startTime.get())/1000.0));
          }
        }
      } finally {
        dynamicExecutorService.shutdown();
      }
    });

    // Wait for dynamic pool to complete all tasks
    try {
      dynamicExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    endTime.set(System.currentTimeMillis());
    LiftRideConsumer.writePerformanceRecordsToCsv("performance_records.csv");
    long totalRunTime = endTime.get() - startTime.get();
    double totalRunTimeInSeconds = totalRunTime / 1000.0;
    int totalProcessedRequests = successCount.get() + failureCount.get();
    double throughput = totalProcessedRequests / totalRunTimeInSeconds;

    System.out.println("Number of successful requests: " + successCount.get());
    System.out.println("Number of unsuccessful requests: " + failureCount.get());
    System.out.println("Total run time (seconds): " + totalRunTimeInSeconds);
    System.out.println("Throughput (requests/second): " + throughput);
    LiftRideConsumer.calculateAndPrintStatistics();
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

  public static void logPerformanceData(String filename, int processedRequests, double currentTime) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
      writer.write(processedRequests + "," + currentTime + "\n");
    } catch (IOException e) {
      System.err.println("Error writing performance data to CSV: " + e.getMessage());
    }
  }
}

