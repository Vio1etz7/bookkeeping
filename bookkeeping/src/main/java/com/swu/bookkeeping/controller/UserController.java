package com.swu.bookkeeping.controller;

import com.swu.bookkeeping.dto.BudgetStatusDto;
import com.swu.bookkeeping.dto.ChangePasswordRequest;
import com.swu.bookkeeping.dto.UserDto;
import com.swu.bookkeeping.service.UserService;
import com.swu.bookkeeping.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    // 定义头像上传目录，与 WebConfig 中的配置对应
    private static final String UPLOAD_DIR = "uploads/";

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 获取当前用户信息
    @GetMapping("/current")
    public ResponseEntity<UserDto> getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(userService.getCurrentUserDto(userId));
    }

    // 更新个人资料
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(@RequestBody UserDto userDto) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(userService.updateUserProfile(userId, userDto));
    }

    // 上传头像
    @PostMapping("/avatar")
    public ResponseEntity<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }

        try {
            Long userId = SecurityUtil.getCurrentUserId();

            // 确保目录存在
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成文件名: UUID + 原始后缀
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.write(path, file.getBytes());

            // 生成访问URL (对应 WebConfig 中的 addResourceHandler)
            String avatarUrl = "/uploads/" + fileName;

            // 更新数据库
            userService.updateAvatar(userId, avatarUrl);

            return ResponseEntity.ok(avatarUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("头像上传失败: " + e.getMessage());
        }
    }

    // 修改密码
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());

        return ResponseEntity.ok("密码修改成功");
    }

    // 设置预算
    @GetMapping("/budget")
    public ResponseEntity<BudgetStatusDto> getBudgetStatus() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(userService.getBudgetStatus(userId));
    }


    // 设置预算
    @PostMapping("/budget")
    public ResponseEntity<String> setBudget(@RequestBody Map<String, BigDecimal> payload){
        Long userId = SecurityUtil.getCurrentUserId();
        BigDecimal amount = payload.get("amount");

        if(amount == null){
            return ResponseEntity.badRequest().body("预算金额不能为空");
        }

        userService.setMonthlyBudget(userId, amount);
        return ResponseEntity.ok("预算设置成功");
    }
}