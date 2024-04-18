package edu.northeastern.cs6650.Assignment4;

import java.util.HashMap;
import java.util.Map;

public class ArgCommandParser {
    public static Map<String, String> parseArguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();
        for (String arg : args) {
            String[] parts = arg.split("=");
            if (parts.length == 2) {
                arguments.put(parts[0], parts[1]);
            }
        }
        return arguments;
    }
}