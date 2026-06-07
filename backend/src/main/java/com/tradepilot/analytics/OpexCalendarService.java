package com.tradepilot.analytics;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class OpexCalendarService {

    public boolean isMonthlyOpex(LocalDate date) {
        if (date == null) {
            return false;
        }

        return date.getDayOfWeek() == DayOfWeek.FRIDAY
                && date.getDayOfMonth() >= 15
                && date.getDayOfMonth() <= 21;
    }
}