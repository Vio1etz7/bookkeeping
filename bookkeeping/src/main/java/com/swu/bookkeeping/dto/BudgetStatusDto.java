package com.swu.bookkeeping.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class BudgetStatusDto {
    private BigDecimal totalBudget;  //总预算
    private BigDecimal totalSpent;  //已支出
    private BigDecimal remaining;   //剩余
    private int percentage;         //使用百分比
}
