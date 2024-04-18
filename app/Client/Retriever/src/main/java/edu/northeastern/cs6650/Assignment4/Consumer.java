package edu.northeastern.cs6650.Assignment4;

import java.util.concurrent.BlockingQueue;

public abstract class Consumer implements Runnable{
    protected final BlockingQueue<GetParam> queue;
    protected int numOfReq;

    public Consumer(BlockingQueue<GetParam> queue, int numOfReq) {
        this.queue = queue;
        this.numOfReq = numOfReq;
    }

    @Override
    public abstract void run();
}
