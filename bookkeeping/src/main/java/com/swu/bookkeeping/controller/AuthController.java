package com.swu.bookkeeping.controller;


import com.swu.bookkeeping.dto.LoginRequest;
import com.swu.bookkeeping.dto.LoginResponse;
import com.swu.bookkeeping.dto.RegisterRequest;
import com.swu.bookkeeping.security.JwtUtil;
import com.swu.bookkeeping.service.JwtAuthService;
import com.swu.bookkeeping.service.UserService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.naming.Binding;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final JwtAuthService jwtAuthService;

    public AuthController(UserService userService, JwtUtil jwtUtil, JwtAuthService jwtAuthService){
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.jwtAuthService = jwtAuthService;
    }

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest registerRequest, BindingResult bindingResult){
        if(bindingResult.hasErrors()){
            return bindingResult.getAllErrors().get(0).getDefaultMessage();
        }
        return userService.register(registerRequest);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword());
    }

}
