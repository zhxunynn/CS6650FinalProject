package edu.northeastern.cs6650.Assignment4;

import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties");
            }
            properties.load(input);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error loading configuration", ex);
        }
    }

    public static int getThreadCount() {
        return Integer.parseInt(properties.getProperty("initial_thread_count", "32"));  // Default to 32 if property not found
    }

    public static int getDayID(){
        return Integer.parseInt(properties.getProperty("dayID","1")); // Default to 1 if property not found
    }

    public static int getNumOfReq(){
        return Integer.parseInt(properties.getProperty("numOfReq","200000"));
    }

    public static String getBasePath(){
        return properties.getProperty("basePath", "http://localhost:8080");
    }
}

