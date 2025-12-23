package com.swu.bookkeeping.task;

import com.swu.bookkeeping.model.UserBackupSetting;
import com.swu.bookkeeping.repository.UserBackupSettingRepository;
import com.swu.bookkeeping.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AutoBackupTask {

    private final UserBackupSettingRepository settingRepository;
    private final BackupService backupService;

    //每天凌晨三点检查一次
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoBackupCheck(){

        LocalDateTime now = LocalDateTime.now();

        for (UserBackupSetting setting : settingRepository.findAll()){

            if(!setting.getEnabled()) continue;

            LocalDateTime last = setting.getLastBackupTime();

            boolean needBackup = (last == null) || last.plusHours(setting.getIntervalHours()).isBefore(now);

            if(needBackup){
                backupService.createBackupForUser(setting.getUserId());

                setting.setLastBackupTime( now);
                settingRepository.save(setting);

                System.out.println("自动备份完成 → userId = " + setting.getUserId());
            }
            else{
                System.out.println("自动备份未完成 → userId = " + setting.getUserId());
            }

        }
    }
}
