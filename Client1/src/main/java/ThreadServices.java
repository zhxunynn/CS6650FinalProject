import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadServices {
    public static final AtomicInteger successCount = new AtomicInteger(0);
    public static final AtomicInteger failureCount = new AtomicInteger(0);
    public static CountDownLatch countDownLatch;
}
