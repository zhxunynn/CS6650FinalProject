package edu.northeastern.cs6650.Assignment4;

import io.swagger.client.ApiException;
import io.swagger.client.api.ResortsApi;
import io.swagger.client.model.ResortSkiers;

import java.util.concurrent.BlockingQueue;

public class ResortConsumer extends Consumer{
    private final ResortsApi resortsApi;

    public ResortConsumer(BlockingQueue<GetParam> queue, int numOfReq, ResortsApi resortsApi) {
        super(queue, numOfReq);
        this.resortsApi = resortsApi;
        this.resortsApi.getApiClient().setBasePath(Config.getBasePath());
    }

    @Override
    public void run() {
        int processed = 0;
        while (processed < numOfReq && !Thread.currentThread().isInterrupted()) {
            try {
                GetParam event = queue.take();
                sendGetRequest(event);
                processed++;
                ClientApp.processedReqNum.incrementAndGet(); // Update processed requests count in Part1.ClientApp
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendGetRequest(GetParam event) {
        final int maxRetries = 5;
        int attempts = 0;
        boolean success = false;

        while (!success && attempts < maxRetries && !Thread.currentThread().isInterrupted()) {
            try {
                ResortSkiers skiers = resortsApi.getResortSkiersDay(event.getResortID(), event.getSeasonID(), event.getDayID());
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
