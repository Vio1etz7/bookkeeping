package com.swu.bookkeeping.repository;

import com.swu.bookkeeping.model.UserBackupSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBackupSettingRepository extends JpaRepository<UserBackupSetting, Long> {
}
