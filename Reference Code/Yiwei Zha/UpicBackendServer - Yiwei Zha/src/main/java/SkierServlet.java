import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The type Skier servlet.
 */
@WebServlet(name = "SkierServlet", value = "/skiers/*")
public class SkierServlet extends HttpServlet {
  private final Gson gson = new Gson();
  private final static String QUEUE_NAME = "lift_rides_queue";
  private static Connection connection;
  private static Channel channel;
  @Override
  public void init() throws ServletException {
    super.init();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("54.203.51.76");
    factory.setUsername("admin");
    factory.setPassword("admin");
    factory.setVirtualHost("/");
    // Consider setting other connection parameters as needed

    try {
      connection = factory.newConnection();
      channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    } catch (Exception e) {
      throw new ServletException("Failed to establish connection to RabbitMQ", e);
    }
  }
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // Extracting path parameters
    String pathInfo = request.getPathInfo(); // Expected format: /{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
    String[] pathParts = pathInfo.split("/");
    if (pathParts.length != 8) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid path parameters\"}");
      return;
    }

    // Assuming pathParts[0] is empty since paths start with a "/"
    int resortID, skierID;
    String seasonID, dayID;
    try {
      resortID = Integer.parseInt(pathParts[1]);
      seasonID = pathParts[3];
      dayID = pathParts[5];
      skierID = Integer.parseInt(pathParts[7]);
    } catch (NumberFormatException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid parameter format\"}");
      return;
    }

    // Basic validation for dayID as an example
    int day;
    try {
      day = Integer.parseInt(dayID);
      if (day < 1 || day > 366) {
        throw new IllegalArgumentException("Invalid dayID");
      }
    } catch (IllegalArgumentException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid dayID\"}");
      return;
    }

    // Parse JSON body
    StringBuilder jb = new StringBuilder();
    String line;
    try (BufferedReader reader = request.getReader()) {
      while ((line = reader.readLine()) != null)
        jb.append(line);
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Error reading request body\"}");
      return;
    }

    Ride liftRide;
    try {
      liftRide = gson.fromJson(jb.toString(), Ride.class);
      // Set path parameters into the liftRide object
      liftRide.setResortID(resortID);
      liftRide.setSkierID(skierID);
      liftRide.setSeasonID(seasonID);
      liftRide.setDayID(dayID);
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Error parsing request body\"}");
      return;
    }

    // At this point, you have parsed all the parameters and the JSON body. You can process them accordingly.
    // For this example, we just return a success message with dummy data
    try {
      // Convert the liftRide object to JSON string
      String message = gson.toJson(liftRide);
      // Send the message to RabbitMQ queue
      sendToRabbitMQ(message);

      // Respond to client with success message
      response.setStatus(HttpServletResponse.SC_CREATED);
      PrintWriter out = response.getWriter();
      response.setContentType("application/json");
      out.print("{\"message\": \"Lift ride for skier " + skierID + " added successfully to the queue.\"}");
      out.flush();
    } catch (Exception e) {
      // Handle errors related to RabbitMQ connection or message sending
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("{\"message\": \"Failed to add lift ride to the queue.\"}");
    }
  }
  private void sendToRabbitMQ(String message) throws IOException {
    channel.basicPublish("", QUEUE_NAME, null, message.getBytes("UTF-8"));
    System.out.println("Message sent: " + message);
  }

  @Override
  public void destroy() {
    super.destroy();
    try {
      if (channel != null) channel.close();
      if (connection != null) connection.close();
    } catch (Exception e) {
      System.err.println("Failed to close RabbitMQ connection/channel: " + e.getMessage());
    }
  }

}

