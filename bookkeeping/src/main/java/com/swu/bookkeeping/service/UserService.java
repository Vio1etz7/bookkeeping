package com.swu.bookkeeping.service;

import com.swu.bookkeeping.dto.BudgetStatusDto;
import com.swu.bookkeeping.dto.LoginResponse;
import com.swu.bookkeeping.dto.RegisterRequest;
import com.swu.bookkeeping.dto.UserDto;
import com.swu.bookkeeping.model.Gender;
import com.swu.bookkeeping.model.Role;
import com.swu.bookkeeping.model.User;
import com.swu.bookkeeping.repository.BillRepository;
import com.swu.bookkeeping.repository.RoleRepository;
import com.swu.bookkeeping.repository.UserRepository;
import com.swu.bookkeeping.security.CustomUserDetails;
import com.swu.bookkeeping.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final BillRepository billRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, BillRepository billRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.billRepository = billRepository;
    }

    //用户注册业务的实现
    @Transactional
    public String register(RegisterRequest registerRequest) {

        System.out.println("Registering user: " + registerRequest.getUsername());
        System.out.println("Available roles: " + roleRepository.findAll());

        //邮箱唯一
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            return "邮箱已存在";
        }

        //用户名唯一
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            return "用户名已存在";
        }

        //验证密码一致性
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            return "两次输入的密码不一致";
        }

        //限制密码长度
        if (registerRequest.getPassword().length() < 8) {
            return "密码长度不能小于8位";
        }

        //限制密码格式 要求包含大写字母，小写字母，数字，特殊符号
        String password = registerRequest.getPassword();
        if (!password.matches(".*[A-Z].*")) {
            return "密码必须包含大写字母";
        }
        if (!password.matches(".*[a-z].*")) {
            return "密码必须包含小写字母";
        }
        if (!password.matches(".*[0-9].*")) {
            return "密码必须包含数字";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "密码必须包含特殊符号";
        }

        User user = new User();
        //创建新用户
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setEnabled(true);
        // 查找角色，这里注意检查数据库中的角色名是否包含隐藏的"\n"符号，避免查询失败
        Role role = roleRepository.findByName("ROLE_USER").orElseThrow(() -> new RuntimeException("角色不存在"));
        user.getRoles().add(role);
        userRepository.save(user);
        return "注册成功";
    }

    //基于用户名返回带jwt令牌的登陆响应
    public LoginResponse login(String username, String password) {
        return userRepository.findByUsernameWithRoles(username)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(u -> {
                    Collection<GrantedAuthority> authorities = u.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                            .collect(Collectors.toList());

                    CustomUserDetails userDetails = new CustomUserDetails(
                            u.getId(), u.getUsername(), "", authorities);

                    return new LoginResponse(jwtUtil.generateToken(u.getUsername()));
                })
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
    }

    // 获取当前用户信息的DTO
    public UserDto getCurrentUserDto(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 获取枚举的字符串名称
        String genderStr = (user.getGender() != null) ? user.getGender().name() : null;

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getAvatarUrl(),
                user.getPhone(),
                user.getBirthday(),
                genderStr, // 注意这里
                user.getSignature()
        );
    }

    // 更新用户基本信息
    @Transactional
    public UserDto updateUserProfile(Long userId, UserDto userDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setPhone(userDto.getPhone());
        user.setBirthday(userDto.getBirthday());
        user.setSignature(userDto.getSignature());

        // --- 修改开始：性别转换逻辑 ---
        if (userDto.getGender() != null && !userDto.getGender().isEmpty()) {
            try {
                // 将前端传来的 "MALE" 字符串转换为 Gender.MALE 枚举
                // 如果前端传 "男"，这里会报错，所以下面第三步必须改前端
                user.setGender(Gender.valueOf(userDto.getGender()));
            } catch (IllegalArgumentException e) {
                // 如果传了不合法的值，可以选择忽略或抛出异常
                throw new RuntimeException("性别类型错误，只能是 MALE, FEMALE 或 OTHER");
            }
        } else {
            user.setGender(null);
        }
        // --- 修改结束 ---

        User savedUser = userRepository.save(user);

        // 返回 DTO 时，如果 gender 是 null 要小心处理，否则 toString 会空指针
        String genderStr = (savedUser.getGender() != null) ? savedUser.getGender().name() : null;

        return new UserDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.isEnabled(),
                savedUser.getAvatarUrl(),
                savedUser.getPhone(),
                savedUser.getBirthday(),
                genderStr, // 这里传回字符串
                savedUser.getSignature()
        );
    }

    // 更新头像
    @Transactional
    public String updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return avatarUrl;
    }

    // 校验密码强度
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new RuntimeException("密码长度不能小于8位");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("密码必须包含大写字母");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("密码必须包含小写字母");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException("密码必须包含数字");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new RuntimeException("密码必须包含特殊符号");
        }
    }

    //修改密码
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        //验证旧密码是否正确
        if(!passwordEncoder.matches(oldPassword, user.getPassword())){
            throw new RuntimeException("当前密码输入错误");
        }

        //验证新密码是否与旧密码相同
        if(passwordEncoder.matches(newPassword, user.getPassword())){
            throw new RuntimeException("新密码不能与旧密码相同");
        }

        //验证新密码强度
        validatePasswordStrength(newPassword);

        //加密并保存
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void setMonthlyBudget(Long userId, BigDecimal amount){
        if(amount.compareTo(BigDecimal.ZERO) < 0){
            throw new RuntimeException("预算金额不能小于0");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setMonthlyBudget( amount);
        userRepository.save(user);
    }


    //获取预算监控状态
    // --- 新增：获取预算监控状态 ---
    public BudgetStatusDto getBudgetStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 1. 获取预算限额 (如果没设置，默认为0)
        BigDecimal limit = user.getMonthlyBudget();
        if (limit == null) limit = BigDecimal.ZERO;

        // 2. 计算本月时间范围
        LocalDateTime startOfMonth = LocalDateTime.of(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()), LocalTime.MIN);
        LocalDateTime endOfMonth = LocalDateTime.of(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()), LocalTime.MAX);

        // 3. 查询本月已支出金额
        BigDecimal spent = billRepository.sumExpenseByUserIdAndDateRange(userId, startOfMonth, endOfMonth);
        if (spent == null) spent = BigDecimal.ZERO;

        // 4. 计算剩余和百分比
        BigDecimal remaining = limit.subtract(spent);
        int percentage = 0;

        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            // (spent / limit) * 100
            percentage = spent.divide(limit, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValue();
        } else if (spent.compareTo(BigDecimal.ZERO) > 0) {
            // 如果没设置预算但花了钱，视为 100% (或者你可以定义为 0，看需求)
            percentage = 100;
        }

        return new BudgetStatusDto(limit, spent, remaining, Math.min(percentage, 100));
    }

}

