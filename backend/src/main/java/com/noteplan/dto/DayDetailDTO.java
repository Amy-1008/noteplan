package com.noteplan.dto;

import lombok.Data;
import java.util.List;

@Data
public class DayDetailDTO {
    private List<ScheduleDetailDTO> schedules;
    private List<NoteDetailDTO> notes;

    @Data
    public static class ScheduleDetailDTO {
        private Long id;
        private String title;
        private String time;
        private String remark;
        private String status;
    }

    @Data
    public static class NoteDetailDTO {
        private Long id;
        private String title;
        private String content;
        private String createTime;
        private List<String> tags;
    }
}