package com.noteplan.dto;

import lombok.Data;
import java.util.List;

@Data
public class DayDetailDTO {
    /**
     * 日程列表
     */
    private List<ScheduleDetailDTO> schedules;

    /**
     * 备忘录列表
     */
    private List<NoteDetailDTO> notes;

    @Data
    public static class ScheduleDetailDTO {
        private Long id;
        private String title;
        private String time;      // 格式化后的时间
       // private String location;
        private String remark;
        private String status;    // pending/completed
    }

    @Data
    public static class NoteDetailDTO {
        private Long id;
        private String title;
        private String content;
        private String createTime; // 格式化后的时间
        private List<String> tags;
    }
}