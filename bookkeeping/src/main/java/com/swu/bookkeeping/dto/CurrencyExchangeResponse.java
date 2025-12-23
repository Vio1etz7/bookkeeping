package com.swu.bookkeeping.dto;


import lombok.Data;

import java.util.List;

@Data
public class CurrencyExchangeResponse {
    String error_code;
    String reason;
    List<Result> result;

    @Data
    public static class Result {
        String currencyF;           // 转换前货币代码
        String currencyF_Name;      // 转换前货币名称
        String currencyT;           // 转换后货币代码
        String currencyT_Name;      // 转换后货币名称
        String currencyFD;          // 汇率方向
        String exchange;            // 当前汇率
        String result;              // 汇率结果
        String updateTime;          // 更新时间

    }

}
