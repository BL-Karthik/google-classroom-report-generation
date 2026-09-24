package com.bridgelabz.config;

public class ReportConfig {

    private static int maxWeek = 6;

    public static void setMaxWeek(int week) {
        if (week < 1) {
            throw new IllegalArgumentException(
                    "Week number must be greater than 0"
            );
        }

        maxWeek = week;
    }

    public static int getMaxWeek() {
        return maxWeek;
    }
}
