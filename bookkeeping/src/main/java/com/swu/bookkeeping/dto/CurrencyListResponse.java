package com.swu.bookkeeping.dto;


import lombok.Data;

import java.util.List;

@Data
public class CurrencyListResponse {
    int error_code;
    String reason;
    Result result;

    @Data
    public static class Result {
        List<CurrencyItem> list;

        @Data
        public static class CurrencyItem {
            String name;
            String code;
        }
    }
}
