package edu.northeastern.cs6650.Assignment4;

import java.util.Random;

public class RandomGenerateResortGetParam {
    private final Random random = new Random();

    /**
     * Generate random lift ride event ride.
     *
     * @return the ride
     */
    public ResortGetParam generateGETEvent() {
        ResortGetParam event = new ResortGetParam();
        event.setResortID(1);
        event.setSeasonID(2023);
        event.setDayID(random.nextInt(3)+1);
        return event;
    }
}
