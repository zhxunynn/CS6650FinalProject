package edu.northeastern.cs6650.Assignment4;

import io.swagger.client.ApiException;
import io.swagger.client.api.ResortsApi;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.ResortSkiers;
import io.swagger.client.model.SkierVertical;
import io.swagger.client.model.SkierVerticalResorts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class SkierVerticalConsumer extends Consumer{
    private final SkiersApi skiersApi;

    public SkierVerticalConsumer(BlockingQueue<GetParam> queue, int numOfReq, SkiersApi skiersApi) {
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
                SkierVertical json = skiersApi.getSkierResortTotals(event.getSkierID(), event.getResortList(), event.getSeasonList());
                List<SkierVerticalResorts> SkierVerticalResorts = json.getResorts();
                for (SkierVerticalResorts skierVerticalResort : SkierVerticalResorts) {
                    System.out.println(String.format("Total vertical for the skier %d on Season %s is %d",
                            event.getSkierID(),
                            skierVerticalResort.getSeasonID(),
                            skierVerticalResort.getTotalVert()));
                }
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

