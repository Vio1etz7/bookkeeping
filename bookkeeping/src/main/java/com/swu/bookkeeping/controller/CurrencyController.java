package com.swu.bookkeeping.controller;

import com.swu.bookkeeping.dto.CurrencyListResponse;
import com.swu.bookkeeping.service.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    @Autowired
    private ExchangeService exchangeService;

    /**
     * 1. 获取支持的货币列表 (用于下拉菜单)
     */
    @GetMapping("/list")
    public CurrencyListResponse getSupportedCurrencies() {
        return exchangeService.getSupportedCurrencies();
    }


    /**
     * 2. 【货币换算】接口
     */
    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertCurrency(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "金额无效"));
        }
        return ResponseEntity.ok(exchangeService.calculateConversionForFrontend(from, to, amount));
    }

    /**
     * 3. 【新增：查询汇率】接口
     */
    @GetMapping("/rate")
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to) {

        // 调用专门的查询服务
        return ResponseEntity.ok(exchangeService.queryRealTimeRate(from, to));
    }

}

