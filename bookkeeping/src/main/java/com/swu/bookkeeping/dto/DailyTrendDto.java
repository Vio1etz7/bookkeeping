package com.swu.bookkeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyTrendDto {
    private String date;    // 日期，格式如 "06-15"
    private Double amount;  // 当日总支出
}