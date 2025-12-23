package com.swu.bookkeeping.controller;

import com.swu.bookkeeping.dto.UserBackupSettingDto;
import com.swu.bookkeeping.model.UserBackupSetting;
import com.swu.bookkeeping.service.BackupSettingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/backupsetting")
@RequiredArgsConstructor
public class BackupSettingController {

    private final BackupSettingService settingService;

    @GetMapping("/get")
    public UserBackupSettingDto getSetting(){
        UserBackupSetting setting = settingService.getCurrentUserSetting();
        UserBackupSettingDto dto = new UserBackupSettingDto();
        dto.setUserId(setting.getUserId());
        dto.setEnabled(setting.getEnabled());
        dto.setIntervalHours(setting.getIntervalHours());
        dto.setLastBackupTime(setting.getLastBackupTime());
        return dto;
    }


    @PostMapping("/update")
    public String updateSetting(@RequestBody SettingRequest request){
        settingService.updateCurrentUserSetting(request.enabled, request.intervalHours);
        return "设置已保存";
    }

    @Data
    public static class SettingRequest{
        private Boolean enabled;
        private Integer intervalHours;
    }

}
