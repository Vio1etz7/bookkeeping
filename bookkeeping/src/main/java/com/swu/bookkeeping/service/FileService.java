// FileService.java
package com.swu.bookkeeping.service;

import com.swu.bookkeeping.model.Bill;
import com.swu.bookkeeping.model.User;
import com.swu.bookkeeping.repository.BillRepository;
import com.swu.bookkeeping.util.ExcelFileUtil;
import com.swu.bookkeeping.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BillService billService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 下载账单数据为Excel文件
     */
    public ResponseEntity<Resource> downloadBills() throws IOException {
        List<Bill> bills = billService.getAllBills();

        File tempFile = File.createTempFile("bills-", ".xlsx");
        ExcelFileUtil.writeBillsToFile(bills, tempFile);

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bills.xlsx");
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(tempFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * 上传Excel文件并将其内容添加为账单
     */
    public ResponseEntity<String> uploadBills(MultipartFile file) {
        try {
            File tempFile = File.createTempFile("upload-", ".xlsx");
            file.transferTo(tempFile);

            billRepository.deleteByUserId(SecurityUtil.getCurrentUserId());

            List<Bill> uploadedBills = ExcelFileUtil.readBillsFromFile(tempFile);

            Long userId = SecurityUtil.getCurrentUserId();
            uploadedBills.forEach(bill -> {
                bill.setId(null);
                bill.setVersion(0L);
                User user = new User();
                user.setId(userId);
                bill.setUser(user);
                if (bill.getCreateTime() == null) {
                    bill.setCreateTime(LocalDateTime.now());
                }
            });

            billRepository.saveAll(uploadedBills);
            tempFile.delete();

            return ResponseEntity.ok("成功上传并添加了 " + uploadedBills.size() + " 条账单记录");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("上传失败: " + e.getMessage());
        }
    }
}
