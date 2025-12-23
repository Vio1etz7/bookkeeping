package com.swu.bookkeeping.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
public class SecurityConfig {
    /**
     * 核心安全过滤链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // 禁用CSRF跨站请求伪造防护

                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers( "/css/**", "/js/**", "/images/**","/html/**",
                                "/api/login","/api/register","/uploads/**").permitAll()
                        .requestMatchers("/api/currency/**","/api/bill/**",
                                "/api/files/**","/api/categories/**","/api/users/**",
                                "/api/backup/**","/api/backupsetting/**","/api/ai/**").hasRole("USER")
                        .anyRequest().authenticated() // 所有其他请求都必须已认证
                ) // 请求路径的访问控制配置
                // 设置会话（Session）创建策略为无状态，符合JWT的认证方式
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // 将jwtFilter插入过滤器链，确保它在默认登录认证逻辑之前执行
                .build(); // 构建整个过滤链并返回，Spring自动使用它
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }/**/

}

