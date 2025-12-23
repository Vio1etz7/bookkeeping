package com.swu.bookkeeping.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiDto {
    //=====请求参数=====
    private String text; //用户输入的文本


    //=====响应结果=====
    private String action;   //“RECORD（记账）”或“CHAT（聊天）”
    private AiBillData data;  //记账数据
    private String reply;       //聊天回复

    @Data
    public static class AiBillData {
        private BigDecimal amount;
        private String billType;     // "INCOME" 或 "EXPENSE"
        private String categoryName; // 匹配到的分类名
        private String date;         // yyyy-MM-dd
        private String remark;
    }

}
