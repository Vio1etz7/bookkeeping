package com.swu.bookkeeping.security;

import com.swu.bookkeeping.service.JwtAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {


    @Autowired
    private JwtAuthService jwtAuthService;


    /**
     * 重写过滤方法，在每次请求到达控制器之前被执行
     * 从请求中提取JWT，验证，并设置Spring的认证上下文
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try{
                CustomUserDetails userDetails = jwtAuthService.loadUserByToken(token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));//额外信息
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            catch (Exception e) {
                logger.error("JWT解析失败", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            }
        // 继续过滤链
        filterChain.doFilter(request, response);
    }
}