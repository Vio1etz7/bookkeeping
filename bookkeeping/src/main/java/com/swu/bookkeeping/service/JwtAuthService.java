package com.swu.bookkeeping.service;

import com.swu.bookkeeping.model.Role;
import com.swu.bookkeeping.model.User;
import com.swu.bookkeeping.repository.UserRepository;
import com.swu.bookkeeping.security.CustomUserDetails;
import com.swu.bookkeeping.security.JwtUtil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtAuthService {

    final private JwtUtil jwtUtil;
    final private UserRepository userRepository;

    public JwtAuthService(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // 基于JWT鉴权并封装用户详情
    public CustomUserDetails loadUserByToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsernameWithRoles(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority( role.getName()))
                .collect(Collectors.toList());

        return new CustomUserDetails(user.getId(), username, "", authorities);
    }

    // 获取用户权限
    private List<GrantedAuthority> getAuthoritiesByUserName(String username) {
        return findRolesByUsername(username).stream()
                .map(role -> new SimpleGrantedAuthority(role.trim()))
                .collect(Collectors.toList());
    }

    // 事务内访问懒加载属性
    private List<String> findRolesByUsername(String username) {
        return userRepository.findByUsernameWithRoles(username)
                .map(user -> user.getRoles().stream()
                        .map(Role::getName)
                        .toList())
                .orElse(List.of());
    }

}