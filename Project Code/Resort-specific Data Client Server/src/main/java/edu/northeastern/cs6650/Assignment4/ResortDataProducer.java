package edu.northeastern.cs6650.Assignment4;

import java.util.concurrent.BlockingQueue;


public class ResortDataProducer implements Runnable{
    private final BlockingQueue<ResortGetParam> queue;
    private final int totalEvents;

    /**
     * Instantiates a new Lift ride producer.
     *
     * @param queue       the queue
     * @param totalEvents the total events
     */
    public ResortDataProducer(BlockingQueue<ResortGetParam> queue, int totalEvents) {
        this.queue = queue;
        this.totalEvents = totalEvents;
    }
    @Override
    public void run() {
        RandomGenerateResortGetParam generator = new RandomGenerateResortGetParam();
        for (int i = 0; i < totalEvents; i++) {
            try {
                ResortGetParam event = generator.generateGETEvent();
                queue.put(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
