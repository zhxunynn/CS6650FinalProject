import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ClientMain {
    private static final Logger logger = Logger.getLogger(ClientMain.class.getName());
    private static ExecutorService pool;

    public static void main(String[] args) throws InterruptedException {
        new ArgCommand().parse(args);
        SkiersApi apiInstance = new SkiersApi();
        apiInstance.getApiClient().setBasePath(ArgCommand.serverURL);
        if (ArgCommand.isTestOnly) {
            doLatency();
            return;
        }
        try {
            apiInstance.writeNewLiftRide(new LiftRide().time(1).liftID(1), 56, "2024", "1", 1);
            logger.info("Both client and server are ready!");
        } catch (Exception e) {
            logger.severe(e.toString());
            logger.info(ArgCommand.serverURL);
            System.exit(-1);
        }
        int totalNumOfReqs = ArgCommand.numOfRequests;
        int startUpPhaseNumOfThreads = ArgCommand.numOfThreads;
        int catchUpPhaseNumOfThreads = ArgCommand.numOfThreads * 3;
        pool = Executors.newFixedThreadPool(startUpPhaseNumOfThreads + catchUpPhaseNumOfThreads);
        ThreadServices.countDownLatch = new CountDownLatch(startUpPhaseNumOfThreads + catchUpPhaseNumOfThreads);
        logger.info("Ready to run phases!");
        long startTime = System.currentTimeMillis();
        doPhase("Startup", 1, startUpPhaseNumOfThreads, 1000);
        doPhase("Catchup", catchUpPhaseNumOfThreads, catchUpPhaseNumOfThreads, (totalNumOfReqs - 1000 * startUpPhaseNumOfThreads) / catchUpPhaseNumOfThreads);

        ThreadServices.countDownLatch.await();
        pool.shutdown();
        long endTime = System.currentTimeMillis();
        System.out.println(
                "===========================Results===========================" + "\n"
                        + "Successful Requests: " + ThreadServices.successCount.get() + "\n"
                        + "Failed Requests: " + ThreadServices.failureCount.get() + "\n"
                        + "Total run time: " + (endTime - startTime) + " (ms) \n"
                        + "Total Throughput in requests per second: " + 1000.0 * (ThreadServices.failureCount.get() + ThreadServices.successCount.get()) / (endTime - startTime) + "\n"
                        + "============================================================="
        );
        System.exit(0);
    }

    private static void doLatency() throws InterruptedException {
        logger.info("Ready to test latency!");
        long startTime = System.currentTimeMillis();
        CountDownLatch testLatch = new CountDownLatch(1);
        ThreadServices.countDownLatch = new CountDownLatch(1);
        new ProcessThread(ArgCommand.numOfRequests, testLatch).run();
        testLatch.await();
        long endTime = System.currentTimeMillis();
        logger.info("Total Duration is " + 1.0 * (endTime - startTime) / 1000 + "s with average latency about " + 1.0 * (endTime - startTime) / 1000 / ArgCommand.numOfRequests + " s.");
    }

    private static void doPhase(String phaseName, int threshold, int numPhaseThreads, int numOfReqs) throws InterruptedException {
        logger.info(phaseName + " is ready to start!");
        logger.info(phaseName + " phase is going to execute " + numPhaseThreads + " threads with " + numOfReqs + " requests each.");
        CountDownLatch latch = new CountDownLatch(threshold);
        for (int i = 0; i < numPhaseThreads; i++) {
            pool.execute(new ProcessThread(numOfReqs, latch));
        }
        latch.await();
        logger.info(phaseName + " has already terminated " + threshold + " thread(s).");
    }
}