package com.swu.bookkeeping.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserBackupSettingDto {
    private long userId;
    private boolean enabled;
    private Integer intervalHours;
    private LocalDateTime lastBackupTime;
}
