package part2;

/**
 * The type Performance record.
 */
public class PerformanceRecord {
  private long startTime;
  private String requestType;
  private long latency; // in milliseconds
  private int responseCode;

  /**
   * Instantiates a new Performance record.
   *
   * @param startTime    the start time
   * @param requestType  the request type
   * @param latency      the latency
   * @param responseCode the response code
   */
  public PerformanceRecord(long startTime, String requestType, long latency, int responseCode) {
    this.startTime = startTime;
    this.requestType = requestType;
    this.latency = latency;
    this.responseCode = responseCode;
  }

  @Override
  public String toString() {
    return startTime + "," + requestType + "," + latency + "," + responseCode;
  }

  /**
   * Gets start time.
   *
   * @return the start time
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Gets request type.
   *
   * @return the request type
   */
  public String getRequestType() {
    return requestType;
  }

  /**
   * Gets latency.
   *
   * @return the latency
   */
  public long getLatency() {
    return latency;
  }

  /**
   * Gets response code.
   *
   * @return the response code
   */
  public int getResponseCode() {
    return responseCode;
  }
}

