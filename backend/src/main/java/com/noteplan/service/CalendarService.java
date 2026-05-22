package com.noteplan.service;

import com.noteplan.dto.DayDetailDTO;
import com.noteplan.dto.MonthDataDTO;

public interface CalendarService {

    /**
     * 获取月份数据
     */
    MonthDataDTO getMonthData(int year, int month);

    /**
     * 获取某天详情
     */
    DayDetailDTO getDayDetail(String date);
}