package com.swu.bookkeeping.dto;

public class LoginResponse {
    private String token; // 令牌

    public LoginResponse() {}
    public LoginResponse(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
}
