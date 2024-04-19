package Servlets;

import Utils.Utilities;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.swagger.client.model.LiftRide;
import io.swagger.client.model.SkierVertical;
import io.swagger.client.model.SkierVerticalResorts;
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

import static Utils.Utilities.isUrlValidForSkierVerticalInOneDay;
import static Utils.Utilities.isUrlValidForSkiersVertical;
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
            throw new ServletException("Failed to initialize SkierServlet due to critical system error.", e);
        }
        if (channelPool == null || channelPool.isEmpty()) {
            throw new ServletException("Channel pool did not initialize correctly.");
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String[] urlParts = req.getPathInfo().split("/");
        if (isUrlValidForSkiersVertical(urlParts)) {
            Jedis jedis = jedisPool.getResource();
            String skierId = urlParts[1];
            String skiersKey = String.format("skiers/%s", skierId);
            String resortId = "1";
//            String resortId = req.getParameter("resort");
//            if (resortId == null) {
//                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//                resp.getWriter().write("{\"message\": \"Invalid inouts supplied\"}");
//                return;
//            }
            String seasonId = req.getParameter("season");
            // if season == null, season range is (2024, 2024) by Data Generation
            if (seasonId == null) seasonId = "2024";
            String totalField = String.format("liftID/resort%s_season%s_total", resortId, seasonId);
            String total = jedis.hget(skiersKey, totalField);
            if (total == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"message\": \"Data not found\"}");
            } else {
                // Create the SkierVerticalResorts object
                SkierVerticalResorts skierResorts = new SkierVerticalResorts();
                skierResorts.setSeasonID(seasonId);
                skierResorts.setTotalVert(Integer.parseInt(total));

// Create the SkierVertical object and add the resorts item
                SkierVertical skierVertical = new SkierVertical();
                skierVertical.addResortsItem(skierResorts);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(skierVertical));
            }
            jedis.close();
        } else if (isUrlValidForSkierVerticalInOneDay(urlParts)) {
            Jedis jedis = jedisPool.getResource();
            String resortId = urlParts[1];
            String seasonId = urlParts[3];
            String dayId = urlParts[5];
            String skierId = urlParts[7];
            String skiersKey = String.format("skiers/%s", skierId);
            String cacheKey = String.format("cache/resort%s_season%s_day%s", resortId, seasonId, dayId);
            String cacheData = jedis.hget(skiersKey, cacheKey);
            if (cacheData != null) {
                String msg = String.format("{ \"resorts\": [{\"seasonID\": \"%s\",\"totalVert\": %s}]}", seasonId, cacheData);
                resp.getWriter().write(msg);
            } else {
                int count = 0;
                String skiersFieldLiftID = String.format("liftID/resort%s_season%s_day%s", resortId, seasonId, dayId);
                String currentLiftID = jedis.hget(skiersKey, skiersFieldLiftID);
                if (currentLiftID.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"message\": \"Data not found\"}");
                } else {
                    String[] LiftIds = currentLiftID.split("_");
                    for (String liftId: LiftIds) {
                        count += Integer.parseInt(liftId) * 10;
                    }
                    resp.setStatus(HttpServletResponse.SC_OK);
                    // jedis set cache
                    jedis.set(cacheKey, String.valueOf(count));
                    resp.getWriter().write(String.valueOf(count));
                }
            }
            jedis.close();
        } else {
            resp.getWriter().write("{\"message\": \"Invalid inouts supplied\"}");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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