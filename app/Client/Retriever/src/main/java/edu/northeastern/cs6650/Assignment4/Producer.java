package edu.northeastern.cs6650.Assignment4;

import java.util.concurrent.BlockingQueue;

public abstract class Producer implements Runnable{
    protected final BlockingQueue<GetParam> queue;
    protected final int totalEvents;

    public Producer(BlockingQueue<GetParam> queue, int totalEvents) {
        this.queue = queue;
        this.totalEvents = totalEvents;
    }

    public abstract void run();
}
