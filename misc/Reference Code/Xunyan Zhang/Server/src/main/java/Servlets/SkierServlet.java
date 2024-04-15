package Servlets;

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
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

@WebServlet(name = "SkierServlet")
public class SkierServlet extends HttpServlet {
    private static final String[] SKIER_POST_BODY = new String[]{"time", "liftID"};
    private final Gson gson = new Gson();
    private final ConnectionFactory connectionFactory = new ConnectionFactory();
    private BlockingQueue<Channel> channelPool;
    private String rabbitMQName;
    private static final int NUM_CHANNELS_DEFAULT = 20; // Default number of channels
    private CircuitBreaker circuitBreaker;
    public final String RABBIT_CONFIG_FILE = "rabbitmq.conf";
    public static final Logger logger = LoggerFactory.getLogger(SkierServlet.class);

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
                logger.info("The config file is " + configs);
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

    private boolean isUrlValid(String[] urlPath) {
        if (urlPath.length == 8) {
            return urlPath[1].chars().allMatch(Character::isDigit) &&
                    urlPath[2].equals("seasons") && urlPath[3].chars().allMatch(Character::isDigit) &&
                    urlPath[4].equals("days") && urlPath[5].chars().allMatch(Character::isDigit) &&
                    urlPath[6].equals("skiers") && urlPath[7].chars().allMatch(Character::isDigit) &&
                    Integer.parseInt(urlPath[5]) >= 1 && Integer.parseInt(urlPath[5]) <= 365;
        }
        return false;
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

        if (!isUrlValid(parts)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"Message\": \"URL is not valid!\"}");
            return;
        }

        LiftRide liftRide = gson.fromJson(body, LiftRide.class);
        int skierID = Integer.parseInt(parts[7]);
        int resortID = Integer.parseInt(parts[1]);
        int dayID = Integer.parseInt(parts[5]);
        JsonObject msg = new JsonObject();
        msg.add("skierID", new JsonPrimitive(skierID));
        msg.add("time", new JsonPrimitive(liftRide.getTime()));
        msg.add("liftID", new JsonPrimitive(liftRide.getLiftID()));
        msg.add("resortID", new JsonPrimitive(resortID));
        msg.add("dayID", new JsonPrimitive(dayID));
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
    public void destroy() {
        super.destroy();
        try {
            if (channelPool != null) {
                for (Channel channel : channelPool) {
                    channel.close();
                }
                channelPool.clear();
            }
            circuitBreaker = null;
        } catch (IOException | TimeoutException e) {
            logger.error(Arrays.toString(e.getStackTrace()));
        }
    }
}