package com.tradepilot.analytics;

import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class SessionClassifier {

    public String classify(LocalTime time) {
        if (time == null) {
            return "UNKNOWN";
        }

        if (!time.isBefore(LocalTime.of(9, 30)) && time.isBefore(LocalTime.of(9, 45))) {
            return "NY_OPEN_FIRST_15";
        }

        if (!time.isBefore(LocalTime.of(9, 45)) && time.isBefore(LocalTime.of(11, 0))) {
            return "NY_AM_EDGE_WINDOW";
        }

        if (!time.isBefore(LocalTime.of(11, 0)) && time.isBefore(LocalTime.of(12, 30))) {
            return "LATE_MORNING";
        }

        if (!time.isBefore(LocalTime.of(12, 30)) && time.isBefore(LocalTime.of(13, 30))) {
            return "LUNCH";
        }

        if (!time.isBefore(LocalTime.of(13, 30)) && time.isBefore(LocalTime.of(16, 0))) {
            return "PM_SESSION";
        }

        return "OTHER";
    }
}
