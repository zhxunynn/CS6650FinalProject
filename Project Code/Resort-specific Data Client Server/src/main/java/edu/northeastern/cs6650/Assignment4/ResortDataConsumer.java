package edu.northeastern.cs6650.Assignment4;
import com.google.gson.Gson;
import io.swagger.client.ApiException;
import io.swagger.client.api.ResortsApi;
import io.swagger.client.model.ResortSkiers;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ResortDataConsumer implements Runnable {
    private final BlockingQueue<ResortGetParam> queue;
    private final int numberOfRequests; // Number of requests this consumer should process
    private final ResortsApi apiClient;

    Gson gson = new Gson();
    /**
     * Instantiates a new Lift ride consumer.
     *
     * @param queue            the queue
     * @param numberOfRequests the number of requests
     */
    public ResortDataConsumer(BlockingQueue<ResortGetParam> queue, int numberOfRequests, ResortsApi apiClient) {
        this.queue = queue;
        this.numberOfRequests = numberOfRequests;
        this.apiClient = apiClient;
        apiClient.getApiClient().setBasePath("http://localhost:8080");
    }



    @Override
    public void run() {
        int processed = 0;
        while (processed < numberOfRequests && !Thread.currentThread().isInterrupted()) {
            try {
                ResortGetParam event = queue.take();
                sendGetRequest(event);
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

    private void sendGetRequest(ResortGetParam event) {
        final int maxRetries = 5;
        int attempts = 0;
        boolean success = false;

        while (!success && attempts < maxRetries && !Thread.currentThread().isInterrupted()) {
            try {
                ResortSkiers skiers = apiClient.getResortSkiersDay(event.getResortID(), event.getSeasonID(), event.getDayID());
                System.out.println("Unique skiers: " + skiers.getNumSkiers() + "at: Day " + event.getDayID() + ", Resort " + event.getResortID() + " , Season" + event.getSeasonID()); // Correctly placed inside the try block
                ClientApp.incrementSuccessCount(); // Assuming ClientApp has this static method
                success = true; // Break the loop on success
            } catch (ApiException e) {
                attempts++;
                System.err.println("Attempt " + attempts + " failed: " + e.getMessage());
                // Implement appropriate backoff strategy here, e.g., Thread.sleep for simple cases
                if (attempts >= maxRetries) {
                    System.err.println("Max retries reached for error.");
                    ClientApp.incrementFailureCount(); // Assuming ClientApp has this static method
                }
            } catch (Exception e) {
                System.err.println("Unexpected error occurred: " + e.getMessage());
                ClientApp.incrementFailureCount(); // Assuming ClientApp has this static method
                break; // Exit on unexpected errors
            }
        }
    }

}
