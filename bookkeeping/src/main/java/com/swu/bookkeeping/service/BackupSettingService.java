package com.swu.bookkeeping.service;

import com.swu.bookkeeping.model.User;
import com.swu.bookkeeping.model.UserBackupSetting;
import com.swu.bookkeeping.repository.UserBackupSettingRepository;
import com.swu.bookkeeping.util.SecurityUtil;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BackupSettingService {

    private final UserBackupSettingRepository settingRepository;
    private final EntityManager entityManager; // 注入 EntityManager


    //获取当前用户的备份设置
    @Transactional
    public UserBackupSetting getCurrentUserSetting(){
        Long userId = SecurityUtil.getCurrentUserId();

        return settingRepository.findById(userId)
                .orElseGet(()->{
                    UserBackupSetting setting = new UserBackupSetting();
//                    User user = new User();
//                    user.setId(userId);
//                    setting.setUser(user);
                    setting.setUserId(userId);
                    return settingRepository.save(setting);
                });
    }


    //更新当前用户的备份设置
    @Transactional
    public void updateCurrentUserSetting(Boolean enabled, Integer intervalHours) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 使用原子操作更新设置
        settingRepository.findById(userId).ifPresentOrElse(
                setting -> {
                    setting.setEnabled(enabled);
                    setting.setIntervalHours(intervalHours);
                    settingRepository.save(setting);
                },
                () -> {
                    // 如果不存在则创建新的设置
                    UserBackupSetting setting = new UserBackupSetting();
                    // 使用 entityManager.getReference() 替代 new User()
                    User user = entityManager.getReference(User.class, userId);
                    setting.setUser(user);
                    setting.setEnabled(enabled);
                    setting.setIntervalHours(intervalHours);
                    settingRepository.save(setting);
                }
        );
    }


    @Transactional
    public void updateLastBackupTime(Long userId, LocalDateTime time){
        settingRepository.findById(userId).ifPresent(setting -> {
            setting.setLastBackupTime(time);
            settingRepository.save(setting);
        });
    }

}
