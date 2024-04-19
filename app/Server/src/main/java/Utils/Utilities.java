package Utils;

import java.util.Arrays;

public class Utilities {
    public static boolean isUrlValid(String[] urlPath) {
        if (urlPath.length == 8) {
            return urlPath[1].chars().allMatch(Character::isDigit) &&
                    urlPath[2].equals("seasons") && urlPath[3].chars().allMatch(Character::isDigit) &&
                    urlPath[4].equals("days") && urlPath[5].chars().allMatch(Character::isDigit) &&
                    urlPath[6].equals("skiers") && urlPath[7].chars().allMatch(Character::isDigit) &&
                    Integer.parseInt(urlPath[5]) >= 1 && Integer.parseInt(urlPath[5]) <= 365;
        }
        return false;
    }

    // Utility method to validate URL for the GET request
    // GET resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
    public static boolean isUrlValidForResortGet(String[] urlPath) {
        // Adjust this method based on your URL validation logic for GET request
        // This is just an example based on your existing isUrlValid method
        if (urlPath.length == 7) {
            if (urlPath[1].chars().allMatch(Character::isDigit)
                    && urlPath[2].equals("seasons") && urlPath[3].chars().allMatch(Character::isDigit)
                    && urlPath[4].equals("day") && urlPath[5].chars().allMatch(Character::isDigit)) {
                return Integer.parseInt(urlPath[5]) >= 1 && Integer.parseInt(urlPath[5]) <= 365;
            }
            return false;
        }
        return false;
    }

    public static boolean isUrlValidForSkiersVertical(String[] urlPath) {
        // Trim the leading slash if it exists
        if (urlPath.length == 3) {
            return
                    urlPath[1].chars().allMatch(Character::isDigit) && urlPath[2].equals("vertical");
        }
        return false;
    }

    public static boolean isUrlValidForSkierVerticalInOneDay(String[] urlPath) {
        if (urlPath.length == 8) {
            return
                    urlPath[1].chars().allMatch(Character::isDigit) &&
                    urlPath[2].equals("seasons") &&
                    urlPath[3].chars().allMatch(Character::isDigit) &&
                    urlPath[4].equals("days") &&
                    urlPath[5].chars().allMatch(Character::isDigit) &&
                    urlPath[6].equals("skiers") &&
                    urlPath[7].chars().allMatch(Character::isDigit);
        }
        return false;
    }
}