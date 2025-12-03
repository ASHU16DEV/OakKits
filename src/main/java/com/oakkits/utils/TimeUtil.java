package com.oakkits.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {
    
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([dhms])");
    
    private TimeUtil() {}
    
    public static long parseTime(String input) {
        if (input == null || input.isEmpty()) return 0;
        
        input = input.toLowerCase().replace(" ", "");
        Matcher matcher = TIME_PATTERN.matcher(input);
        long totalSeconds = 0;
        
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "d":
                    totalSeconds += TimeUnit.DAYS.toSeconds(value);
                    break;
                case "h":
                    totalSeconds += TimeUnit.HOURS.toSeconds(value);
                    break;
                case "m":
                    totalSeconds += TimeUnit.MINUTES.toSeconds(value);
                    break;
                case "s":
                    totalSeconds += value;
                    break;
            }
        }
        
        return totalSeconds * 1000;
    }
    
    public static String formatTime(long milliseconds) {
        if (milliseconds <= 0) return "0s";
        
        long seconds = milliseconds / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    public static String formatTimeShort(long milliseconds) {
        if (milliseconds <= 0) return "Ready";
        
        long seconds = milliseconds / 1000;
        
        if (seconds >= 86400) {
            return (seconds / 86400) + "d";
        } else if (seconds >= 3600) {
            return (seconds / 3600) + "h";
        } else if (seconds >= 60) {
            return (seconds / 60) + "m";
        } else {
            return seconds + "s";
        }
    }
}
