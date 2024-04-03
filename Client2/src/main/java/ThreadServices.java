import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadServices {
    public static final AtomicInteger successCount = new AtomicInteger(0);
    public static final AtomicInteger failureCount = new AtomicInteger(0);
    public static CountDownLatch countDownLatch;

    private static final CopyOnWriteArrayList<Record> records = new CopyOnWriteArrayList<>();

    public static void addToRecords(Record record) {
        records.add(record);
    }

    public static void printRecords(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        FileWriter csvWriter;
        csvWriter = new FileWriter(path.toFile());
        csvWriter.append("startTime,requestType,latency,responseCode\n");

        Collections.sort(records);
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE, sum = 0;
        double median = records.get((int) (0.5 * records.size())).getLatency();
        double p99 = records.get((int) (0.99 * records.size())).getLatency();
        for (Record r : records) {
            sum += r.getLatency();
            max = Math.max(max, r.getLatency());
            min = Math.min(min, r.getLatency());
            csvWriter.append(r.toString());
        }
        double mean = sum / records.size();
        System.out.println(
                "Mean response time: " + mean + "\n"
                        + "Median response time: " + median + "\n"
                        + "99th response time: " + p99 + "\n"
                        + "min and max response time: " + "min: " + min + " , max: " + max
        );
    }
}
