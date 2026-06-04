package com.noteplan.service;

import com.noteplan.dto.DayDetailDTO;
import com.noteplan.dto.MonthDataDTO;

public interface CalendarService {
    MonthDataDTO getMonthData(int year, int month);
    DayDetailDTO getDayDetail(String date);
}