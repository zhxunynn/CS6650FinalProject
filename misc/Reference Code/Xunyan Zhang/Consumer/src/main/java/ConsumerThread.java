import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConsumerThread implements Runnable{
    private final String queueName;
    private final Connection conn;

    private JedisPool jedisPool;
    private final Gson gson = new Gson();
    private final static String DB_SEPARATOR = "_";
    public ConsumerThread(String queueName, Connection conn, JedisPool jedisPool){
        this.queueName = queueName;
        this.conn = conn;
        this.jedisPool = jedisPool;
    }
    @Override
    public void run() {
        try {
            Channel channel = conn.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicQos(10);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
                Consumer.logger.info(msg);
                writeToDB(msg);
                JsonObject json = gson.fromJson(msg, JsonObject.class);
                Integer skierID = json.get("skierID").getAsInt();
                if (Consumer.records.containsKey(skierID)) {
                    Consumer.records.get(skierID).add(json);
                } else {
                    List<JsonObject> newRecord = Collections.synchronizedList(new ArrayList<>());
                    newRecord.add(json);
                    Consumer.records.put(skierID, newRecord);
                }
                Consumer.logger.info("Thread" + Thread.currentThread().getName() + " received: " + json);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            channel.basicConsume(this.queueName, false ,deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            Consumer.logger.error(Arrays.toString(e.getStackTrace()));
        }
    }

    private void writeToDB(String entry) {
        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject json = gson.fromJson(entry, JsonObject.class);
            String skierId = String.valueOf(json.get("skierID"));
            String time = String.valueOf(json.get("time"));
            String liftId = String.valueOf(json.get("liftID"));
            String resortId = String.valueOf(json.get("resortID"));
            String dayId = String.valueOf(json.get("dayID"));
            Map<String, String> storedEntry = jedis.hgetAll(skierId);
            if (storedEntry.isEmpty()) {
                storedEntry = new HashMap<> (storedEntry);
            }
            storedEntry.put("time", storedEntry.getOrDefault("time", "") + DB_SEPARATOR + time);
            storedEntry.put("liftID", storedEntry.getOrDefault("liftID", "") + DB_SEPARATOR + liftId);
            storedEntry.put("resortID", storedEntry.getOrDefault("resortID", "") + DB_SEPARATOR + resortId);
            storedEntry.put("dayID", storedEntry.getOrDefault("dayID", "") + DB_SEPARATOR + dayId);
            jedis.hmset(skierId, storedEntry);
        } catch (Exception e) {
            Consumer.logger.error(Arrays.toString(e.getStackTrace()));
        }
    }
}