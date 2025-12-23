// FileController.java
package com.swu.bookkeeping.controller;

import com.swu.bookkeeping.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 下载账单数据为Excel文件
     */
    @GetMapping("/download/bills")
    public ResponseEntity<Resource> downloadBills() throws IOException {
        return fileService.downloadBills();
    }

    /**
     * 上传Excel文件并将其内容添加为账单
     */
    @PostMapping("/upload/bills")
    public ResponseEntity<String> uploadBills(@RequestParam("file") MultipartFile file) {
        return fileService.uploadBills(file);
    }
}
