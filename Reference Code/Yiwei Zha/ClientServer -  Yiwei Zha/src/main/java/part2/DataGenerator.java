package part2;

import java.util.Random;

/**
 * The type Data generator.
 */
public class DataGenerator {


    private final Random random = new Random();

  /**
   * Generate random lift ride event ride.
   *
   * @return the ride
   */
  public Ride generateRandomLiftRideEvent() {
      Ride event = new Ride();
      event.setSkierID(random.nextInt(100000) + 1); // Generates a number between 1 and 100000
      event.setResortID(random.nextInt(10) + 1); // Generates a number between 1 and 10
      event.setLiftID(random.nextInt(40) + 1); // Generates a number between 1 and 40
      event.setSeasonID("2024");
      event.setDayID("1");
      event.setTime(random.nextInt(360) + 1); // Generates a number between 1 and 360

      return event;
    }
  }

