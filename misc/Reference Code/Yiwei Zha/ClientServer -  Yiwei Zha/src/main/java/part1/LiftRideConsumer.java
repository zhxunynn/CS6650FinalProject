package part1;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response; // Ensure you have Gson imported for JSON conversion

// Your sendPostRequest method


/**
 * The type Lift ride consumer.
 */
public class LiftRideConsumer implements Runnable {
  private final BlockingQueue<Ride> queue;
  private final int numberOfRequests; // Number of requests this consumer should process
  OkHttpClient client = new OkHttpClient();
  /**
   * Instantiates a new Lift ride consumer.
   *
   * @param queue            the queue
   * @param numberOfRequests the number of requests
   */
  public LiftRideConsumer(BlockingQueue<Ride> queue, int numberOfRequests) {
    this.queue = queue;
    this.numberOfRequests = numberOfRequests;

  }



  @Override
  public void run() {
    int processed = 0;
    while (processed < numberOfRequests && !Thread.currentThread().isInterrupted()) {
      try {
        Ride event = queue.take();
        sendPostRequest(event);
        processed++;
        ClientApp.processedRequests.incrementAndGet(); // Update processed requests count in Part1.ClientApp
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

    Gson gson = new Gson();

    // Prepare JSON payload from the Ride object
    String json = gson.toJson(event);
    MediaType JSON = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(json, JSON);

    // Construct the request URL
    String url = String.format("http://localhost:8080/Upic_1_0/skiers/%d/seasons/%s/days/%s/skiers/%d",
            event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());

    while (attempts < maxRetries && !Thread.currentThread().isInterrupted()) {
      Request request = new Request.Builder().url(url).post(body).build();
      try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful()) {
          // Handle successful response
          ClientApp.incrementSuccessCount();
          break; // Exit loop on success
        } else {
          // Handle HTTP error responses
          handleHttpError(null, response, attempts, maxRetries);
          attempts++;
        }
      } catch (IOException e) {
        // Handle network errors or issues executing the request
        handleHttpError(e, null, attempts, maxRetries);
        attempts++;
      }

      if (attempts >= maxRetries) {
        System.err.println("Max retries reached for error.");
        ClientApp.incrementFailureCount();
      }
    }
  }

  private void handleHttpError(IOException e, Response response, int attempts, int maxRetries) {
    int statusCode = response != null ? response.code() : -1;
    String responseBody = null;
    try {
      responseBody = response != null && response.body() != null ? response.body().string() : null;
    } catch (IOException ioException) {
      System.err.println("Error reading response body: " + ioException.getMessage());
    }

    System.err.println("HTTP Status Code: " + statusCode);
    if (responseBody != null) {
      System.err.println("Response Body: " + responseBody);
    }

    if (statusCode >= 500 && statusCode < 600) {
      // 5XX Server Error, retry
      System.err.println("Retry attempt #" + attempts + " after server error: " + e.getMessage());
    } else if (statusCode >= 400 && statusCode < 500) {
      // 4XX Client Error, do not retry
      System.err.println("Client error occurred: " + e.getMessage());
    } else {
      // Other errors, including network issues
      System.err.println("Non-retryable error occurred: " + e.getMessage());
    }

    if (attempts >= maxRetries) {
      System.err.println("Max retries reached for error.");
      ClientApp.incrementFailureCount(); // Update failure count if max retries reached
    }
  }

}
