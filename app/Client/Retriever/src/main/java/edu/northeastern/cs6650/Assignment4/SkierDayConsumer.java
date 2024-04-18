package edu.northeastern.cs6650.Assignment4;

import io.swagger.client.ApiException;
import io.swagger.client.JSON;
import io.swagger.client.api.ResortsApi;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.ResortSkiers;

import java.util.concurrent.BlockingQueue;


public class SkierDayConsumer extends Consumer{
    private final SkiersApi skiersApi;

    public SkierDayConsumer(BlockingQueue<GetParam> queue, int numOfReq, SkiersApi skiersApi) {
        super(queue, numOfReq);
        this.skiersApi = skiersApi;
        this.skiersApi.getApiClient().setBasePath(Config.getBasePath());
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
                Integer skiersVertical = skiersApi.getSkierDayVertical(event.getResortID(), String.valueOf(event.getSeasonID()), String.valueOf(event.getDayID()), event.getSkierID());
                System.out.println("Total vertical for the skier" + event.getSkierID() + " at resort 1 on day " + event.getDayID() + "season" + event.getSeasonID()+ " is " + skiersVertical );
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
