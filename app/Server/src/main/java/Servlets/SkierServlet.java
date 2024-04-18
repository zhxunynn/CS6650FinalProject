package Servlets;

import Utils.Utilities;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.swagger.client.model.LiftRide;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@WebServlet(name = "SkierServlet", urlPatterns = {"/skiers/*"})
public class SkierServlet extends HttpServlet {
    private static final String[] SKIER_POST_BODY = new String[]{"time", "liftID"};
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

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        circuitBreaker.executeRunnable(() -> {
            try {
                doPostWithCircuitBreaker(request, response);
            } catch (ServletException | IOException e) {
                logger.error(Arrays.toString(e.getStackTrace()));
            }
        });
    }

    private void doPostWithCircuitBreaker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String[] parts = request.getPathInfo().split("/");

        if (parts.length != 8) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"Message\": \"URL is not correct!\"}");
            return;
        }
        JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
        for (String param : SKIER_POST_BODY) {
            if (body == null || body.get(param) == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"Message\": \"Body param is missing!\"}");
                return;
            }
        }

        if (!Utilities.isUrlValid(parts)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"Message\": \"URL is not valid!\"}");
            return;
        }

        LiftRide liftRide = gson.fromJson(body, LiftRide.class);
        int resortID = Integer.parseInt(parts[1]);
        int seasonID = Integer.parseInt(parts[3]);
        int dayID = Integer.parseInt(parts[5]);
        int skierID = Integer.parseInt(parts[7]);
        JsonObject msg = new JsonObject();
        msg.add("resortID", new JsonPrimitive(resortID));
        msg.add("seasonID", new JsonPrimitive(seasonID));
        msg.add("dayID", new JsonPrimitive(dayID));
        msg.add("skierID", new JsonPrimitive(skierID));
        msg.add("time", new JsonPrimitive(liftRide.getTime()));
        msg.add("liftID", new JsonPrimitive(liftRide.getLiftID()));
        Channel channel = null;
        try {
            channel = channelPool.take();
            channel.basicPublish("", rabbitMQName, null, msg.toString().getBytes());
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"Message\": \"The URL is valid, message sent to Rabbit MQ successfully!\"}");
        } catch (InterruptedException e) {
            response.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
            throw new ServletException("Failed to initialize RabbitMQ connection", e);
        } finally {
            if (channel != null)
                channelPool.add(channel);
        }
    }
    private boolean isUrlValidForSkiersVertical(String[] urlPath) {
        if (urlPath.length == 3) {
            return  urlPath[0].equals("skiers") &&
                    urlPath[1].chars().allMatch(Character::isDigit) &&
                    urlPath[2].equals("vertical");
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] urlParts = req.getPathInfo().split("/");
        //for vertical count
        boolean isCountTotalVertical = isUrlValidForSkiersVertical(urlParts);
        boolean isCountTotalVerticalForOneDay = Utilities.isUrlValidForSkierVerticalInOneDay(urlParts);
        if (isCountTotalVertical || isCountTotalVerticalForOneDay) {
            Jedis jedis = jedisPool.getResource();
            String skierId = urlParts[1];
            String resort = isCountTotalVertical ? req.getParameter("resort") : urlParts[1];
            String season = isCountTotalVertical ? req.getParameter("season") : urlParts[3];
            String dayId = isCountTotalVerticalForOneDay ? urlParts[7] : null;
            Map<String, String> storedEntry = jedis.hgetAll(skierId);
            String[] resortIds = storedEntry.get("resortID").split("_");
            String[] liftIds = storedEntry.get("liftID").split("_");
            String[] seasonIds = null;
            String[] dayIds = null;
            if (season != null) {
                seasonIds = storedEntry.get("seasonID").split("_");
            }
            if (dayId != null) {
                dayIds = storedEntry.get("dayID").split("_");
            }
            int count = 0;
            for (int i = 0; i < resortIds.length; i++) {
                if (resort.equals(resortIds[i])) {
                    if (season != null && !season.equals(seasonIds[i])) continue;
                    if (dayId == null) {
                        count += Integer.parseInt(liftIds[i]) * 10;
                    } else {
                        if (dayId.equals(dayIds[i])) {
                            count += Integer.parseInt(liftIds[i]) * 10;
                        }
                    }
                }
            }

            resp.setContentType("application/json");
            if (storedEntry.keySet().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"message\": \"Data not found\"}");
            } else {
                String msg = String.format("{ \"resorts\": [{\"seasonID\": \"%s\",\"totalVert\": %s}]}", season, count);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(msg);

            }
            jedis.close();
        }
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