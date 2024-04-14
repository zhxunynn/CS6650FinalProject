import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class ProcessThread implements Runnable {
    // As per indicated in Handling Errors section of Assignment 1
    public static final int RETRIES = 5;
    final private int numOfReqs;
    final private CountDownLatch latch;
    final private SkiersApi api;

    public ProcessThread(int numOfReqs, CountDownLatch latch) {
        this.numOfReqs = numOfReqs;
        this.latch = latch;
        api = new SkiersApi();
        api.getApiClient().setBasePath(ArgCommand.serverURL);
    }

    @Override
    public void run() {
        for (int i = 0; i < numOfReqs; i++) {
            int resortID = ThreadLocalRandom.current().nextInt(10) + 1;
            int skierID = ThreadLocalRandom.current().nextInt(100000) + 1;
            int time = ThreadLocalRandom.current().nextInt(360) + 1;
            int liftID = ThreadLocalRandom.current().nextInt(40) + 1;
            int triedCount = 0;
            while (triedCount < RETRIES) {
                try {
                    api.writeNewLiftRide(
                            new LiftRide().liftID(liftID).time(time),
                            resortID,
                            "2024",
                            "1",
                            skierID
                    );
                    break;
                } catch (ApiException e) {
                    triedCount++;
                    try {
                        Thread.sleep((long) Math.pow(5, triedCount));
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            if (triedCount < RETRIES) ThreadServices.successCount.incrementAndGet();
            else ThreadServices.failureCount.incrementAndGet();
        }
        latch.countDown();
        ThreadServices.countDownLatch.countDown();
    }
}