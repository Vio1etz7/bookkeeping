package com.swu.bookkeeping.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_backup_setting")
public class UserBackupSetting {
    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private Boolean enabled;

    private Integer intervalHours = 24;

    private LocalDateTime lastBackupTime;

    @Version
    private Long version;
}
