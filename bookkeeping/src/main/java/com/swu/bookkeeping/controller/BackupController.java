package com.swu.bookkeeping.controller;

import com.swu.bookkeeping.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    /**
     * 创建备份
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBackup() {
        String fileName = backupService.createBackup();
        return ResponseEntity.ok("备份创建成功: " + fileName);
    }

    /**
     * 列出当前用户所有备份
     */
    @GetMapping("/list")
    public List<Map<String, Object>> listBackups() {
        return backupService.listBackups();
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String fileName) {
        return backupService.downloadBackup(fileName);
    }

    /**
     * 删除备份文件
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String fileName) {
        backupService.deleteBackup(fileName);
        return ResponseEntity.ok("删除成功");
    }

    /**
     * 恢复备份
     */
    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestParam String fileName) {
        backupService.restoreBackup(fileName);
        return ResponseEntity.ok("恢复成功");
    }


}
