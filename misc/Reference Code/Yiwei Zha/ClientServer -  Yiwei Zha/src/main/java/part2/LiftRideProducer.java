package part2;

import java.util.concurrent.BlockingQueue;

/**
 * The type Lift ride producer.
 */
public class LiftRideProducer implements Runnable{

    private final BlockingQueue<Ride> queue;
    private final int totalEvents;

  /**
   * Instantiates a new Lift ride producer.
   *
   * @param queue       the queue
   * @param totalEvents the total events
   */
  public LiftRideProducer(BlockingQueue<Ride> queue, int totalEvents) {
      this.queue = queue;
      this.totalEvents = totalEvents;
    }

  @Override
    public void run() {
      DataGenerator generator = new DataGenerator();
      for (int i = 0; i < totalEvents; i++) {
        try {
          Ride event = generator.generateRandomLiftRideEvent();
          queue.put(event);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
}

