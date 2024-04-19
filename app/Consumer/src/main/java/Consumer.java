import com.google.gson.JsonObject;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

public class Consumer {
    private Connection connection;
    private Integer numThreads;
    private String rabbitMQName;

    private static JedisPool jedisPool;
    public static final Map<Integer, List<JsonObject>> records = new ConcurrentHashMap<>();
    public static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    Consumer(String configPath) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        Path rabbitMQConfigPath = Paths.get(configPath);
        try {
            try (Stream<String> lines = Files.lines(rabbitMQConfigPath)) {
                List<String> configs = lines
                        .map(String::trim)
                        .toList();
                connectionFactory.setHost(configs.get(0));
                connectionFactory.setPort(Integer.parseInt(configs.get(1)));
                connectionFactory.setUsername(configs.get(2));
                connectionFactory.setPassword(configs.get(3));
                setNumThreads(Integer.parseInt(configs.get(4)));
                setRabbitMQName(configs.get(5));
                jedisPool = new JedisPool(configs.get(6), Integer.parseInt(configs.get(7)));
                setConnection(connectionFactory.newConnection());
                logger.info("Connect to RabbitMQ & Redis successfully");
            }
        } catch (IOException | TimeoutException e) {
            logger.error(Arrays.toString(e.getStackTrace()));
        }
    }
    public static void main(String[] args) {
        Consumer consumer = new Consumer(args[0]);
        ExecutorService pool = Executors.newFixedThreadPool(consumer.getNumThreads());
        for(int i=0; i< consumer.getNumThreads(); i++)
            pool.execute(new ConsumerThread(consumer.getRabbitMQName(), consumer.getConnection(), jedisPool));
        pool.shutdown();
    }

    public Integer getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(Integer numThreads) {
        this.numThreads = numThreads;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getRabbitMQName() {
        return rabbitMQName;
    }

    public void setRabbitMQName(String rabbitMQName) {
        this.rabbitMQName = rabbitMQName;
    }
}