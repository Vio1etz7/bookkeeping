package com.swu.bookkeeping.service;

import com.swu.bookkeeping.dto.CurrencyExchangeResponse;
import com.swu.bookkeeping.dto.CurrencyListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExchangeService {

    @Autowired
    private WebClient currencyListWebClient;

    @Autowired
    private WebClient currencyExchangeWebClient;

    @Value("${currencylist.api.key}")
    private String listApiKey;

    @Value("${currencyexchange.api.key}")
    private String exchangeApiKey;

    /**
     * 获取支持的货币列表
     */
    public CurrencyListResponse getSupportedCurrencies() {
        return currencyListWebClient.get()
                .uri(uriBuilder -> uriBuilder.queryParam("key", listApiKey).build())
                .retrieve()
                .bodyToMono(CurrencyListResponse.class)
                .block();
    }

    /**
     * 货币转换
     */
    public CurrencyExchangeResponse convertCurrency(String from, String to, BigDecimal amount) {
        return currencyExchangeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("amount", amount)
                        .queryParam("key", exchangeApiKey)
                        .build())
                .retrieve()
                .bodyToMono(CurrencyExchangeResponse.class)
                .block();
    }

    /**
     * 将指定货币金额转换为人民币
     */
    public BigDecimal convertToCNY(String currency, BigDecimal amount) {
        // 参数校验
        if (currency == null || amount == null) {
            return BigDecimal.ZERO;
        }

        // 如果已经是人民币，直接返回
        if ("CNY".equalsIgnoreCase(currency)) {
            return amount;
        }

        try {
            // 调用货币转换API
            CurrencyExchangeResponse response = convertCurrency(currency, "CNY", amount);

            // 检查响应是否有效
            if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
                // 选择第一个结果（假设只有一个结果）
                CurrencyExchangeResponse.Result result = response.getResult().get(0);
                if (result != null && result.getExchange() != null) {
                    // 使用汇率进行计算: 原始金额 × 汇率
                    BigDecimal exchangeRate = new BigDecimal(result.getExchange());
                    return amount.multiply(exchangeRate);
                }
            }

            // 如果API响应无效，记录日志并返回原始金额
            System.err.println("Currency conversion failed for " + currency + " to CNY");
            return amount;
        } catch (Exception e) {
            // 如果转换过程中出现异常，记录错误并返回原始金额
            System.err.println("Error converting currency " + currency + " to CNY: " + e.getMessage());
            return amount;
        }
    }

    /**
     * 货币换算器
     */
    public Map<String, Object> calculateConversionForFrontend(String from, String to, BigDecimal amount) {
        Map<String, Object> finalResult = new HashMap<>();

        // 1. 调用现有的方法获取原始数据
        CurrencyExchangeResponse response = convertCurrency(from, to, amount);

        if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
            CurrencyExchangeResponse.Result apiResult = response.getResult().get(0);
            BigDecimal rate = new BigDecimal(apiResult.getExchange());
            BigDecimal convertedAmount = amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);

            finalResult.put("success", true);
            finalResult.put("type", "calculator"); // 标识这是计算结果
            finalResult.put("from", apiResult.getCurrencyF());
            finalResult.put("to", apiResult.getCurrencyT());
            finalResult.put("rate", apiResult.getExchange());
            finalResult.put("sourceAmount", amount);
            finalResult.put("convertedAmount", convertedAmount);
            finalResult.put("updateTime", apiResult.getUpdateTime());
        } else {
            finalResult.put("success", false);
            finalResult.put("message", "无法获取数据");
        }
        return finalResult;
    }

    // --- 5. 【新增：查询汇率】功能 (不需要金额，只看汇率) ---
    public Map<String, Object> queryRealTimeRate(String from, String to) {
        Map<String, Object> rateInfo = new HashMap<>();

        // 调用API (amount传1或者0都行，因为我们只关心汇率，API不强制校验amount)
        CurrencyExchangeResponse response = convertCurrency(from, to, BigDecimal.ONE);

        if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
            CurrencyExchangeResponse.Result apiResult = response.getResult().get(0);

            rateInfo.put("success", true);
            rateInfo.put("type", "rate_query"); // 标识这是查询结果
            rateInfo.put("fromCode", apiResult.getCurrencyF());
            rateInfo.put("fromName", apiResult.getCurrencyF_Name());
            rateInfo.put("toCode", apiResult.getCurrencyT());
            rateInfo.put("toName", apiResult.getCurrencyT_Name());
            rateInfo.put("exchangeRate", apiResult.getExchange()); // 核心数据：当前汇率
            rateInfo.put("updateTime", apiResult.getUpdateTime()); // 更新时间

            // 可以添加一个友好的描述，例如 "1 美元 ≈ 7.24 人民币"
            rateInfo.put("displayText", String.format("1 %s ≈ %s %s",
                    apiResult.getCurrencyF_Name(),
                    apiResult.getExchange(),
                    apiResult.getCurrencyT_Name()));

        } else {
            rateInfo.put("success", false);
            rateInfo.put("message", "查询失败，请检查货币代码");
        }
        return rateInfo;
    }

}
