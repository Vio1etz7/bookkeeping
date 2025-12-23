package com.swu.bookkeeping.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)//非空且唯一
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 11)
    private String phone;

    @Column
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(columnDefinition = "TEXT")
    private String signature;

    @Column(name = "monthly_budget",precision = 12, scale = 2)
    private BigDecimal monthlyBudget; //月预算

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",  //表名
          joinColumns = @JoinColumn(name = "user_id"), //当前实体（user）在中间表的外键
          inverseJoinColumns = @JoinColumn(name = "role_id")) //关联的实体（role）在中间表外键

    private Set<Role> roles = new HashSet<>();



}
