package com.swu.bookkeeping.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // 建议通过配置文件读取密钥，并确保长度满足HS256要求（≥ 32 字节）
    private final SecretKey key = Keys.hmacShaKeyFor("mySecretKey123456789012345678901234".getBytes());
    /**
     * 生成JWT令牌，设置主题、签发时间、过期时间、密钥
     */
    public String generateToken(String username) {
        long expiration = 3600000; // 一个小时
        return Jwts.builder()
                .subject(username) // 主题
                .issuedAt(new Date()) // 签发时间
                .expiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(key)  // 用于签名的密钥
                .compact();
    }

    // 提取token中的主题，一般是用户名
    public String extractUsername(String token) {
        return Jwts.parser() // 创建解析器构建器
                .verifyWith(key) // 设置用于签名验证的密钥
                .build() // 构建解析器实例
                .parseSignedClaims(token) // 解析并验证这个含签名的令牌
                .getPayload() // 获取令牌负载，例如主题、签发时间、过期时间等
                .getSubject(); // 获取主题，如用户名
    }


    // 验证令牌是否有效（签名正确、未被篡改）
    public boolean validateToken(String token) {
        try {
            Jwts.parser() // 创建解析器构建器
                    .verifyWith(key) // 设置签名验证使用的密钥
                    .build() // 构建解析器实例
                    .parseSignedClaims(token);  // 若验证失败，这里会抛异常
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
