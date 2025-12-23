package com.swu.bookkeeping.service;


import com.swu.bookkeeping.model.Bill;
import com.swu.bookkeeping.model.Category;
import com.swu.bookkeeping.model.User;
import com.swu.bookkeeping.repository.BillRepository;
import com.swu.bookkeeping.util.ExcelFileUtil;
import com.swu.bookkeeping.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final BillRepository billRepository;

    private static final String BACKUP_DIR = "backup-files";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 创建备份文件
     */
    public String createBackup(){
        Long userId = SecurityUtil.getCurrentUserId();

        //1.查询账单
        List<Bill> bills = billRepository.findByUserIdOrderByCreateTimeDesc(userId);

        //2.确保备份目录存在
        File dir = new File(BACKUP_DIR);
        if(!dir.exists()) dir.mkdirs();

        //3.创建文件名
        String fileName = "user_" + userId + "_" +
                LocalDateTime.now().format(FORMATTER) + ".xlsx";

        File file = new File(dir, fileName);

        //4.用已有工具类写excel
        ExcelFileUtil.writeBillsToFile(bills,file);

        return fileName;

    }

    /**
     * 列出当前用户所有备份文件
     */
    public List<Map<String, Object>> listBackups(){
        Long userId = SecurityUtil.getCurrentUserId();

        File dir = new File(BACKUP_DIR);
        if(!dir.exists())return Collections.emptyList();

        File[] files = dir.listFiles((d,name) -> name.startsWith("user_" + userId));
        List<Map<String,Object>> list = new ArrayList<>();

        if(files != null){
            for(File f : files){
                Map<String,Object> info = new HashMap<>();
                info.put("fileName",f.getName());
                info.put("fileSize",f.length());
                info.put("createTime",
                        LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(f.lastModified()),
                                ZoneId.systemDefault()
                        ).toString()
                );
                list.add(info);
            }
        }

        return list;
    }

    /**
     * 下载备份文件
     */
    public ResponseEntity<Resource> downloadBackup(String fileName) {
        try {
            File file = new File(BACKUP_DIR, fileName);
            if (!file.exists()) {
                return ResponseEntity.status(404).body(null);
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * 删除备份文件
     */
    public void deleteBackup(String fileName) {
        File file = new File(BACKUP_DIR, fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 恢复备份文件
     */
    public void restoreBackup(String fileName){
        Long userId = SecurityUtil.getCurrentUserId();
        File file = new File(BACKUP_DIR, fileName);

        if(!file.exists()) throw new RuntimeException("备份文件不存在");

        //1.读取excel数据
        List<Bill> bills = ExcelFileUtil.readBillsFromFile(file);

        //2.清空用户原来的账单
        billRepository.deleteByUserId(userId);

        //3.逐条写入数据库
        for(Bill bill:bills){
            bill.setId(null);
            User user = new User();
            user.setId(userId);
            bill.setUser(user);

            //默认分类处理
            Category defaultCategory = new Category();
            defaultCategory.setId(1L);
            bill.setCategory(defaultCategory);

            billRepository.save(bill);
        }
    }

    /**
     * 自动备份使用
     */
    public String createBackupForUser(Long userId){
        List<Bill> bills = billRepository.findByUserIdOrderByCreateTimeDesc(userId);

        File dir = new File(BACKUP_DIR);
        if(!dir.exists()) dir.mkdirs();

        String fileName = "user_" + userId + "_" + LocalDateTime.now().format(FORMATTER) + "_auto.xlsx";

        File file = new File(dir, fileName);

        ExcelFileUtil.writeBillsToFile(bills,file);

        return fileName;
    }


}
