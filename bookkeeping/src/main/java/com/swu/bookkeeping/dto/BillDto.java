package com.swu.bookkeeping.dto;

import com.swu.bookkeeping.model.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillDto {
    private Long id;
    private BigDecimal amount;
    private String billtype;
    private String currency;
    private String remark;
    private LocalDateTime createTime;
    private Category category;
    private String username;
    private String originalCurrency;
    private BigDecimal originalAmount;
    private Boolean isConverted;
    private String conversionStatus;
    private BigDecimal exchangeRate;
    private LocalDateTime exchangeTime;
    //版本控制
    private Long version;
    private String tags;
}
