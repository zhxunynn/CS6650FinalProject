package part1;

/**
 * The type Ride.
 */
public class Ride {
  private int skierID;
  private int resortID;
  private int liftID;
  private String seasonID;
  private String dayID;
  private int time;

  /**
   * Instantiates a new Ride.
   *
   * @param skierID  the skier id
   * @param resortID the resort id
   * @param liftID   the lift id
   * @param seasonID the season id
   * @param dayID    the day id
   * @param time     the time
   */
  public Ride(int skierID, int resortID, int liftID, String seasonID, String dayID,
      int time) {
    this.skierID = skierID;
    this.resortID = resortID;
    this.liftID = liftID;
    this.seasonID = seasonID;
    this.dayID = dayID;
    this.time = time;
  }

  /**
   * Instantiates a new Ride.
   */
  public Ride() {

  }

  /**
   * Gets skier id.
   *
   * @return the skier id
   */
  public int getSkierID() {
    return skierID;
  }

  /**
   * Sets skier id.
   *
   * @param skierID the skier id
   */
  public void setSkierID(int skierID) {
    this.skierID = skierID;
  }

  /**
   * Gets resort id.
   *
   * @return the resort id
   */
  public int getResortID() {
    return resortID;
  }

  /**
   * Sets resort id.
   *
   * @param resortID the resort id
   */
  public void setResortID(int resortID) {
    this.resortID = resortID;
  }

  /**
   * Gets lift id.
   *
   * @return the lift id
   */
  public int getLiftID() {
    return liftID;
  }

  /**
   * Sets lift id.
   *
   * @param liftID the lift id
   */
  public void setLiftID(int liftID) {
    this.liftID = liftID;
  }

  /**
   * Gets season id.
   *
   * @return the season id
   */
  public String getSeasonID() {
    return seasonID;
  }

  /**
   * Sets season id.
   *
   * @param seasonID the season id
   */
  public void setSeasonID(String seasonID) {
    this.seasonID = seasonID;
  }

  /**
   * Gets day id.
   *
   * @return the day id
   */
  public String getDayID() {
    return dayID;
  }

  /**
   * Sets day id.
   *
   * @param dayID the day id
   */
  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  /**
   * Gets time.
   *
   * @return the time
   */
  public int getTime() {
    return time;
  }

  /**
   * Sets time.
   *
   * @param time the time
   */
  public void setTime(int time) {
    this.time = time;
  }
}
