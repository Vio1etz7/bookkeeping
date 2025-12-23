package com.swu.bookkeeping.service;

import com.swu.bookkeeping.dto.CurrencyExchangeResponse;
import com.swu.bookkeeping.dto.DailyTrendDto;
import com.swu.bookkeeping.model.Bill;
import com.swu.bookkeeping.model.Category;
import com.swu.bookkeeping.model.User;
import com.swu.bookkeeping.repository.BillRepository;
import com.swu.bookkeeping.repository.CategoryRepository;
import com.swu.bookkeeping.repository.UserRepository;
import com.swu.bookkeeping.util.SecurityUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BillService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private ChatModel chatModel;


    /**
     * 添加新账单
     */
    public Bill addBill(Bill bill) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 获取当前用户
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证分类是否存在且属于当前用户
        if (bill.getCategory() == null || bill.getCategory().getId() == null) {
            throw new IllegalArgumentException("分类不能为空");
        }

        Optional<Category> categoryOpt = categoryRepository.findById(bill.getCategory().getId());
        if (!categoryOpt.isPresent()) {
            throw new IllegalArgumentException("分类不存在");
        }

        Category category = categoryOpt.get();
        // 检查分类权限（系统分类或用户自己的分类）
        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("无权使用此分类");
        }

        // 设置用户
        bill.setUser(currentUser);

        // 设置分类
        bill.setCategory(category);

        // 设置创建时间
        if (bill.getCreateTime() == null) {
            bill.setCreateTime(LocalDateTime.now());
        }

        // 货币转换逻辑
        if (!"CNY".equals(bill.getCurrency())) {
            bill.setOriginalAmount(bill.getAmount());
            bill.setOriginalCurrency(bill.getCurrency());

            //调用汇率服务获取汇率和时间
            CurrencyExchangeResponse response = exchangeService.convertCurrency(
                    bill.getCurrency(), "CNY", bill.getAmount());

            if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
                CurrencyExchangeResponse.Result result = response.getResult().get(0);
                if (result != null && result.getExchange() != null) {
                    BigDecimal exchangeRate = new BigDecimal(result.getExchange());
                    // 正确解析 updateTime
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime exchangeTime = LocalDateTime.parse(result.getUpdateTime(), formatter);


                    bill.setExchangeRate(exchangeRate);
                    bill.setExchangeTime(exchangeTime);
                }
            }

            // 转换为人民币存储
            BigDecimal cnyAmount = exchangeService.convertToCNY(bill.getCurrency(), bill.getAmount());
            bill.setAmount(cnyAmount);
            bill.setIsConverted(true);
            bill.setConversionStatus(Bill.ConversionStatus.SUCCESS);
        } else {
            bill.setOriginalAmount(bill.getAmount());
            bill.setOriginalCurrency("CNY");
            bill.setIsConverted(false);
            bill.setConversionStatus(Bill.ConversionStatus.NOT_NEEDED);
        }

        // ★★★ 新增：AI 消费情感/必要性分析 ★★★
        // 只分析“支出”且有“备注”的账单，避免浪费 Token
        if (bill.getBilltype() == Bill.BillType.EXPENSE
                && bill.getRemark() != null
                && !bill.getRemark().trim().isEmpty()) {
            try {
                String tag = generateExpenseTag(bill);
                bill.setTags(tag);
            } catch (Exception e) {
                // AI 分析失败不应阻止账单保存，打印日志即可
                System.err.println("AI 标签生成失败: " + e.getMessage());
            }
        }

        return billRepository.save(bill);
    }

    // ★★★ 辅助方法：调用 AI 生成标签 ★★★
    private String generateExpenseTag(Bill bill) {
        String prompt = String.format("""
            你是一个记账标签助手。
            请根据以下支出信息，判断消费性质：
            - 金额：%.2f 元
            - 分类：%s
            - 备注：%s
            
            请从以下 4 个标签中选择最合适的一个返回（只返回标签名，不要带 # 号，也不要解释）：
            [必要支出, 冲动消费, 社交娱乐, 自我提升]
            
            如果无法判断，返回 "普通支出"。
            """,
                bill.getAmount(),
                bill.getCategory().getName(),
                bill.getRemark()
        );

        String response = chatModel.call(prompt);

        // 清理可能返回的 <think> 标签
        response = response.replaceAll("(?s)<think>.*?</think>", "").trim();
        // 清理可能存在的标点符号
        response = response.replace("#", "").replace("。", "").replace(".", "").trim();

        // 简单校验返回结果是否在预期范围内
        if (response.length() > 10) return "普通支出"; // 避免 AI 发疯返回长句子

        return response;
    }

    /**
     * 获取当前用户的所有账单
     */
    public List<Bill> getAllBills() {
        Long userId = SecurityUtil.getCurrentUserId();
        return billRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 根据时间段获取当前用户的账单
     */
    public List<Bill> getAllBills(String period, String keyword) {
        // 1. 先获取该用户的所有账单
        List<Bill> allBills = getAllBills();

        // 2. 如果列表为空，直接返回
        if (allBills.isEmpty()) {
            return allBills;
        }

        // 3. 准备结果流
        Stream<Bill> stream = allBills.stream();

        // 4. 应用时间筛选 (逻辑保持不变)
        if (period != null && !period.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            if ("today".equalsIgnoreCase(period)) {
                stream = stream.filter(bill -> isSameDay(bill.getCreateTime(), now));
            } else if ("month".equalsIgnoreCase(period)) {
                stream = stream.filter(bill -> isSameMonth(bill.getCreateTime(), now));
            }
        }

        // 5. 应用关键词搜索 (新增逻辑)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String key = keyword.trim().toLowerCase(); // 转小写实现不区分大小写
            stream = stream.filter(bill -> {
                // 匹配备注
                boolean matchRemark = bill.getRemark() != null && bill.getRemark().toLowerCase().contains(key);

                // 匹配分类名称
                boolean matchCategory = bill.getCategory() != null && bill.getCategory().getName().toLowerCase().contains(key);

                // 匹配金额 (将金额转为字符串后匹配，例如搜 "12" 可以匹配 120, 12.5)
                boolean matchAmount = bill.getAmount() != null && bill.getAmount().toString().contains(key);

                return matchRemark || matchCategory || matchAmount;
            });
        }

        return stream.collect(Collectors.toList());
    }

    public List<Bill> getAllBills(String period) {
        return getAllBills(period, null);
    }

    /**
     * 根据ID获取账单（确保属于当前用户）
     */
    public Bill getBillById(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Optional<Bill> bill = billRepository.findById(id);
        if (bill.isPresent() && bill.get().getUser().getId().equals(userId)) {
            return bill.get();
        }
        return null;
    }

    /**
     * 更新账单（确保属于当前用户）
     */
    public Bill updateBill(Long id, Bill updatedBill) {
        Long userId = SecurityUtil.getCurrentUserId();
        Optional<Bill> existingBillOpt = billRepository.findById(id);

        if (existingBillOpt.isPresent() && existingBillOpt.get().getUser().getId().equals(userId)) {
            Bill existingBill = existingBillOpt.get();

            // 更新字段
            existingBill.setAmount(updatedBill.getAmount());
            existingBill.setBilltype(updatedBill.getBilltype());
            existingBill.setRemark(updatedBill.getRemark());

            // 更新分类
            if (updatedBill.getCategory() != null && updatedBill.getCategory().getId() != null) {
                Optional<Category> categoryOpt = categoryRepository.findById(updatedBill.getCategory().getId());
                if (categoryOpt.isPresent()) {
                    Category category = categoryOpt.get();
                    // 检查分类权限
                    if (category.getUser() == null || category.getUser().getId().equals(userId)) {
                        existingBill.setCategory(category);
                    } else {
                        throw new IllegalArgumentException("无权使用此分类");
                    }
                } else {
                    throw new IllegalArgumentException("分类不存在");
                }
            }

            // 货币转换逻辑
            if (!"CNY".equals(updatedBill.getCurrency())) {
                existingBill.setOriginalAmount(updatedBill.getAmount());
                existingBill.setOriginalCurrency(updatedBill.getCurrency());

                // 调用汇率服务获取汇率和时间
                CurrencyExchangeResponse response = exchangeService.convertCurrency(
                        updatedBill.getCurrency(), "CNY", updatedBill.getAmount());

                // 解析响应并设置汇率和时间
                if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
                    CurrencyExchangeResponse.Result result = response.getResult().get(0);
                    if (result != null && result.getExchange() != null) {
                        BigDecimal exchangeRate = new BigDecimal(result.getExchange());
                        // 正确解析 updateTime
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDateTime exchangeTime = LocalDateTime.parse(result.getUpdateTime(), formatter);


                        existingBill.setExchangeRate(exchangeRate);
                        existingBill.setExchangeTime(exchangeTime);
                    }
                }

                // 转换为人民币存储
                BigDecimal cnyAmount = exchangeService.convertToCNY(updatedBill.getCurrency(), updatedBill.getAmount());
                existingBill.setAmount(cnyAmount);
                existingBill.setIsConverted(true);
                existingBill.setConversionStatus(Bill.ConversionStatus.SUCCESS);
            } else {
                existingBill.setOriginalAmount(updatedBill.getAmount());
                existingBill.setOriginalCurrency("CNY");
                existingBill.setIsConverted(false);
                existingBill.setConversionStatus(Bill.ConversionStatus.NOT_NEEDED);
            }
            existingBill.setCurrency(updatedBill.getCurrency());

            return billRepository.save(existingBill);
        }
        return null;
    }

    /**
     * 删除账单（确保属于当前用户）
     */
    public void deleteBill(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Optional<Bill> bill = billRepository.findById(id);
        if (bill.isPresent() && bill.get().getUser().getId().equals(userId)) {
            billRepository.deleteById(id);
        } else {
            throw new RuntimeException("账单不存在或无权删除");
        }
    }

    /**
     * 获取当前用户的账单统计信息
     */
    public BillStatistics getStatistics(String period) {
        List<Bill> bills = getAllBills(period);

        // 计算总收入和总支出（以人民币计算）
        double totalIncome = bills.stream()
                .filter(bill -> bill.getBilltype() == Bill.BillType.INCOME)
                .mapToDouble(bill -> bill.getAmount().doubleValue())
                .sum();

        double totalExpense = bills.stream()
                .filter(bill -> bill.getBilltype() == Bill.BillType.EXPENSE)
                .mapToDouble(bill -> bill.getAmount().doubleValue())
                .sum();

        // 按类型统计
        List<BillTypeStatistics> typeStatistics = bills.stream()
                .collect(Collectors.groupingBy(Bill::getBilltype))
                .entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey().toString();
                    List<Bill> typeBills = entry.getValue();
                    double totalAmount = typeBills.stream()
                            .mapToDouble(bill -> bill.getAmount().doubleValue())
                            .sum();
                    return new BillTypeStatistics(type, typeBills.size(), totalAmount);
                })
                .collect(Collectors.toList());

        return new BillStatistics(bills.size(), totalIncome, totalExpense, typeStatistics);
    }

    // 判断两个日期是否为同一天
    private boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
        return date1.toLocalDate().equals(date2.toLocalDate());
    }

    // 判断两个日期是否为同月
    private boolean isSameMonth(LocalDateTime date1, LocalDateTime date2) {
        return date1.getYear() == date2.getYear() &&
                date1.getMonth() == date2.getMonth();
    }

    /**
     * 账单统计信息类
     */
    @Data
    @NoArgsConstructor
    public static class BillStatistics {
        private int totalCount;         // 总账单数
        private double totalIncome;     // 总收入
        private double totalExpense;    // 总支出
        private List<BillTypeStatistics> typeStatistics; // 按类型统计

        public BillStatistics(int totalCount, double totalIncome, double totalExpense,
                              List<BillTypeStatistics> typeStatistics) {
            this.totalCount = totalCount;
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.typeStatistics = typeStatistics;
        }
    }

    /**
     * 按类型统计信息类
     */
    @Data
    @NoArgsConstructor
    public static class BillTypeStatistics {
        private String type;        // 类型（INCOME/EXPENSE）
        private int count;          // 该类型账单数量
        private double totalAmount; // 该类型总金额

        public BillTypeStatistics(String type, int count, double totalAmount) {
            this.type = type;
            this.count = count;
            this.totalAmount = totalAmount;
        }
    }

    /**
     * 获取消费趋势数据
     */
    public List<DailyTrendDto> getTrendData (int days){
        Long userId = SecurityUtil.getCurrentUserId();

        //确定时间范围(今天结束 - 过去N天开始)
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime =  endTime.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0);

        //获取该时间段内的所有账单
        List<Bill> bills = billRepository.findByUserIdAndCreateTimeBetweenOrderByCreateTimeDesc(
                                         userId, startTime, endTime);

        Map<String , Double> trendMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 4. 先预填充最近 days 天的数据为 0.0 (关键步骤，否则图表会断裂)
        for (int i = 0; i < days; i++) {
            String dateKey = startTime.plusDays(i).format(formatter);
            trendMap.put(dateKey, 0.0);
        }

        // 5. 遍历账单，累加支出金额
        for (Bill bill : bills) {
            // 只统计支出
            if (bill.getBilltype() == Bill.BillType.EXPENSE) {
                String dateKey = bill.getCreateTime().format(formatter);
                // 如果 map 中包含这个日期（理论上都包含），则累加
                if (trendMap.containsKey(dateKey)) {
                    trendMap.put(dateKey, trendMap.get(dateKey) + bill.getAmount().doubleValue());
                }
            }
        }

        // 6. 转换为 DTO 列表返回
        return trendMap.entrySet().stream()
                .map(entry -> new com.swu.bookkeeping.dto.DailyTrendDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }





}