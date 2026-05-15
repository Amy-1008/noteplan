package com.noteplan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetInfo {
    private Long id;
    private String type;  // "NOTE" 或 "SCHEDULE"
}