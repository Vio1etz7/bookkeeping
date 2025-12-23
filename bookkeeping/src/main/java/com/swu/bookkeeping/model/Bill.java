package com.swu.bookkeeping.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 核心账单字段
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // 转换后的人民币金额

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BillType billtype;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // 货币转换相关字段
    @Column(length = 10)
    private String currency = "CNY"; // 用户选择的货币类型

    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount; // 原始货币金额

    @Column(name = "original_currency", length = 10)
    private String originalCurrency; // 原始货币类型

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate; // 使用的汇率

    @Column(name = "exchange_time")
    private LocalDateTime exchangeTime; // 汇率获取时间

    @Column(name = "target_currency",length = 10)
    private String targetCurrency = "CNY";

    @Column(name = "is_converted")
    private Boolean isConverted = false; // 是否经过货币转换

    @Enumerated(EnumType.STRING)
    @Column(name = "conversion_status")
    private ConversionStatus conversionStatus = ConversionStatus.NOT_NEEDED;


    @Column(name = "error_message")
    private String errorMessage;

    @Version
    private Long version;

    @Column(length = 50)
    private String tags;

    public enum BillType {
        INCOME, EXPENSE
    }

    public enum ConversionStatus {
        NOT_NEEDED, SUCCESS, FAILED
    }

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }

        // 如果用户选择的是人民币，则不需要转换
        if ("CNY".equals(currency)) {
            this.originalAmount = this.amount;
            this.originalCurrency = "CNY";
            this.exchangeRate = BigDecimal.ONE;
            this.exchangeTime = LocalDateTime.now();
            this.isConverted = false;
        }
    }
}
