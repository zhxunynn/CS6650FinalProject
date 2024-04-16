package Servlets;

import Utils.Utilities;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@WebServlet(name = "ResortServlet", urlPatterns = {"/resorts/*"})
public class ResortServlet extends HttpServlet {
    private static final String[] RESORT_GET_BODY = new String[]{"time","numSkiers"};
    private final Gson gson = new Gson();
    private final ConnectionFactory connectionFactory = new ConnectionFactory();
    private BlockingQueue<Channel> channelPool;
    private String rabbitMQName;
    private static final int NUM_CHANNELS_DEFAULT = 20; // Default number of channels
    private CircuitBreaker circuitBreaker;
    public final String RABBIT_CONFIG_FILE = "rabbitmq.conf";
    public static final Logger logger = LoggerFactory.getLogger(SkierServlet.class);

    // Redis configuration will be implemented here
    private static JedisPool jedisPool;

    @Override
    public void init() throws ServletException {
        super.init();

        // Create Circuit Breaker configuration
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Set the failure rate threshold to 50%
                .waitDurationInOpenState(Duration.ofSeconds(10)) // Wait for 10 seconds in open state before transitioning to half-open
                .permittedNumberOfCallsInHalfOpenState(100) // Allow 100 calls in half-open state
                .build();

        // Create Circuit Breaker instance
        circuitBreaker = CircuitBreaker.of("skierCircuitBreaker", config);

        Path rabbitMQConfigPath = Paths.get(Objects.requireNonNull(this.getClass().getClassLoader().getResource(RABBIT_CONFIG_FILE)).getPath());
        try {
            int numChannel;
            try (Stream<String> lines = Files.lines(rabbitMQConfigPath)) {
                List<String> configs = lines
                        .map(String::trim)
                        .toList();
                connectionFactory.setHost(configs.get(0));
                connectionFactory.setPort(Integer.parseInt(configs.get(1)));
                connectionFactory.setUsername(configs.get(2));
                connectionFactory.setPassword(configs.get(3));
                numChannel = configs.size() > 4 ? Integer.parseInt(configs.get(4)) : NUM_CHANNELS_DEFAULT; // Use default if not specified
                rabbitMQName = configs.get(5);
                jedisPool = new JedisPool(configs.get(6), Integer.parseInt(configs.get(7)));
                logger.info("The config file is {}", configs);
            }
            Connection connection = connectionFactory.newConnection();
            channelPool = new LinkedBlockingDeque<>();
            for (int i = 0; i < numChannel; i++) {
                Channel channel = connection.createChannel();
                channel.queueDeclare(rabbitMQName, false, false, false, null);
                channelPool.add(channel);
            }
        } catch (IOException | TimeoutException e) {
            logger.error(Arrays.toString(e.getStackTrace()));
        }
    }

    // For now, let's assume you're checking the URL pattern and extracting parameters
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] urlParts = req.getPathInfo().split("/");

        // Since your URL pattern for GET request is expected to be:
        // /resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
        // The length should be 7 (including the empty first element due to leading slash)
        if (urlParts.length != 7) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"Message\": \"URL format is incorrect!\"}");
            return;
        }

        // Validate the URL
        if (!Utilities.isUrlValidForResortGet(urlParts)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"Message\": \"URL parameters are invalid!\"}");
            return;
        }

        int resortID = Integer.parseInt(urlParts[1]);
        int seasonID = Integer.parseInt(urlParts[3]);
        int dayID = Integer.parseInt(urlParts[5]);
        int uniqueSkiers = 0;
        try (Jedis jedis = jedisPool.getResource()) {
            String cacheKey = String.format("cache/resort%s_season%s", resortID, seasonID);
            String cacheField = String.format("resorts/day%s", dayID);
            if (jedis.hexists(cacheKey, cacheField)) {
                uniqueSkiers = Integer.parseInt(jedis.hget(cacheKey, cacheField));
            } else {
                String resortKey = String.format("resorts/resort%s_season%s_day%s", resortID, seasonID, dayID);
                uniqueSkiers = jedis.smembers(resortKey).size();
                jedis.hset(cacheKey, cacheField, String.valueOf(uniqueSkiers));
            }
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
        }

        // Placeholder for Redis query logic
        String redisRespContent = String.format("The unique skiers for Resort %d, Season %d at Day %d is %d",
                resortID, seasonID, dayID, uniqueSkiers);
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(redisRespContent));
    }


    @Override
    public void destroy() {
        super.destroy();
        try {
            if (channelPool != null) {
                for (Channel channel : channelPool) {
                    channel.close();
                }
                channelPool.clear();
            }
            if (jedisPool != null) {
                jedisPool.close();
            }
            circuitBreaker = null;
        } catch (IOException | TimeoutException e) {
            logger.error(Arrays.toString(e.getStackTrace()));
        }
    }
}