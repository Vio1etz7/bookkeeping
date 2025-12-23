package com.swu.bookkeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Lombok 注解组合：
// @Data: 生成 getter、setter、toString、equals 和 hashCode 方法
// @AllArgsConstructor: 生成包含所有字段的构造函数
// @NoArgsConstructor: 生成无参构造函数
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    private String username;
    private String password;
}