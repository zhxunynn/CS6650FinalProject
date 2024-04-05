package part2;

import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type Lift ride consumer.
 */
public class LiftRideConsumer implements Runnable {
  private final BlockingQueue<Ride> queue;
  private final int numberOfRequests; // Number of requests this consumer should process
  private final SkiersApi apiInstance = new SkiersApi(); // Instantiate the API class

  /**
   * The constant performanceRecords.
   */
  public static Queue<PerformanceRecord> performanceRecords = new ConcurrentLinkedQueue<>();;

  private long startTime;
  private long duration;

  private String requestType = "POST";

  private int responseCode = 201;

  /**
   * Instantiates a new Lift ride consumer.
   *
   * @param queue            the queue
   * @param numberOfRequests the number of requests
   */
  public LiftRideConsumer(BlockingQueue<Ride> queue, int numberOfRequests) {
    this.queue = queue;
    this.numberOfRequests = numberOfRequests;
    apiInstance.getApiClient().setBasePath("http://54.148.188.229:8080/Upic_war%20exploded/");
  }

  @Override
  public void run() {
    int processed = 0;
    while (processed < numberOfRequests && !Thread.currentThread().isInterrupted()) {
      try {
        Ride event = queue.take();
        startTime = System.currentTimeMillis();
        sendPostRequest(event);
        long endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        PerformanceRecord record = new PerformanceRecord(startTime, "POST", duration, 201); // Commented out
        performanceRecords.add(record);
        processed++;
        ClientApp.processedRequests.incrementAndGet();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void sendPostRequest(Ride event) {
    final int maxRetries = 5;
    int attempts = 0;
    

    LiftRide liftRide = new LiftRide();
    liftRide.setLiftID(event.getLiftID());
    // Add other required fields from event to liftRide as needed

    while (attempts < maxRetries) {
      try {
        // Call the API method
        apiInstance.writeNewLiftRide(liftRide, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());
        ClientApp.incrementSuccessCount(); // Update success count
        break; // Break out of loop if successful
      } catch (ApiException e) {
        attempts++;
        handleApiException(e, attempts, maxRetries);
      } catch (Exception e) {
        System.err.println("Non-API exception occurred: " + e.getMessage());
        ClientApp.incrementFailureCount(); // Update failure count
        break; // Break loop for non-API exceptions
      }
    }
  }

  private void handleApiException(ApiException e, int attempts, int maxRetries) {
    if (e.getCode() >= 500 && e.getCode() < 600) {
      // 5XX Server Error, retry
      System.err.println("Retry attempt #" + attempts + " after server error: " + e.getMessage());
    } else if (e.getCode() >= 400 && e.getCode() < 500) {
      // 4XX Client Error, retry
      System.err.println("Retry attempt #" + attempts + " after client error: " + e.getMessage());
    } else {
      // Non-retryable error, break loop
      System.err.println("Non-retryable error occurred: " + e.getMessage());
    }

    if (attempts >= maxRetries) {
      System.err.println("Max retries reached for error.");
      ClientApp.incrementFailureCount(); // Update failure count if max retries reached
    }
  }

  /**
   * Write performance records to csv.
   *
   * @param filename the filename
   */
  public static void writePerformanceRecordsToCsv(String filename) {
    try (FileWriter writer = new FileWriter(filename, true)) {
      writer.append("StartTime,RequestType,Latency,ResponseCode\n");
      for (PerformanceRecord record : LiftRideConsumer.performanceRecords) {
        writer.append(record.toString()).append("\n");
      }
      System.out.println("Performance records written to " + filename);
    } catch (IOException e) {
      System.err.println("Error writing performance records to CSV file: " + e.getMessage());
    }
  }

  public static void calculateAndPrintStatistics() {
    List<PerformanceRecord> records = new ArrayList<>(LiftRideConsumer.performanceRecords);
    // Ensure records are sorted by latency for median and percentile calculations
    records.sort(Comparator.comparingLong(PerformanceRecord::getLatency));

    int totalRequests = records.size();
    double mean = records.stream().mapToLong(PerformanceRecord::getLatency).average().orElse(0);
    double median = records.size() % 2 == 0 ?
        (records.get(records.size() / 2).getLatency() + records.get(records.size() / 2 - 1).getLatency()) / 2.0 :
        records.get(records.size() / 2).getLatency();
    double p99 = records.get((int) (totalRequests * 0.99)).getLatency();
    long min = records.stream().mapToLong(PerformanceRecord::getLatency).min().orElse(0);
    long max = records.stream().mapToLong(PerformanceRecord::getLatency).max().orElse(0);

    System.out.println("Mean response time (ms): " + mean);
    System.out.println("Median response time (ms): " + median);
    System.out.println("99th percentile response time (ms): " + p99);
    System.out.println("Min response time (ms): " + min);
    System.out.println("Max response time (ms): " + max);
  }
  @Override
  public String toString() {
    return startTime + "," + requestType + "," + duration + "," + responseCode;
  }
}
