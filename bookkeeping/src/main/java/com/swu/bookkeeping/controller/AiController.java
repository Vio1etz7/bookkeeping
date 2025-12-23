package com.swu.bookkeeping.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.swu.bookkeeping.dto.AiDto;
import com.swu.bookkeeping.model.Bill;
import com.swu.bookkeeping.model.Category;
import com.swu.bookkeeping.service.BillService;
import com.swu.bookkeeping.service.CategoryService;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") //确保跨域允许
public class AiController {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BillService billService;

    @Autowired
    public AiController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody AiDto request){
        try{
            // 1. 获取当前用户可用的所有分类 (调用你的 Service，它会自动获取 CurrentUserId)
            List<Category> expenseCats = categoryService.getAvailableCategories(Category.BillType.EXPENSE);
            List<Category> incomeCats = categoryService.getAvailableCategories(Category.BillType.INCOME);
            // 2. 提取分类名称并拼接
            List<String> allCategoryNames = new ArrayList<>();
            expenseCats.forEach(c -> allCategoryNames.add(c.getName()));
            incomeCats.forEach(c -> allCategoryNames.add(c.getName()));

            //去重（防止系统分类和自定义分类重名）
            String categoriesStr = allCategoryNames.stream().distinct().collect(Collectors.joining(","));

            String today = LocalDate.now().toString();

            // 3. 构造 Prompt
            String systemText = String.format("""
                你是一个专业的记账助手。今天是 %s。
                用户的可用分类有：%s。
                
                请分析用户输入：
                1. 如果用户意图是记账，提取信息并返回JSON。
                   - "billType" 必须是 "EXPENSE" 或 "INCOME"。
                   - "categoryName" 必须从用户分类列表中找一个最相似的。
                   - 格式：{"action":"RECORD", "data": {"amount": 10.0, "billType": "EXPENSE", "categoryName": "餐饮", "date": "2023-01-01", "remark": "备注"}}
                
                2. 如果无法提取金额或只是闲聊，返回JSON。
                   - 格式：{"action":"CHAT", "reply": "你的回复"}
                
                注意：只返回纯JSON字符串，不要包含 Markdown 格式，不要输出 <think> 标签。
                """, today, categoriesStr);

            SystemMessage systemMessage = new SystemMessage(systemText);
            UserMessage userMessage = new UserMessage(request.getText());
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            // 4. 调用 AI
            String aiResponse = chatModel.call(prompt).getResult().getOutput().getText();

            // 5. 清洗数据 (LLM可能包含思考过程)
            String cleanJson = cleanAiResponse(aiResponse);

            // 6. 解析为对象
            AiDto result = objectMapper.readValue(cleanJson, AiDto.class);

            return ResponseEntity.ok(result);
        }catch(Exception e){
            e.printStackTrace();
            // 如果是 SecurityUtil 抛出的“用户未登录”，这里会捕获到
            return ResponseEntity.badRequest().body(Map.of("error", "AI 服务异常: " + e.getMessage()));
        }

    }

    // 清洗工具方法
    private String cleanAiResponse(String response) {
        // 去除 <think> 标签
        response = response.replaceAll("(?s)<think>.*?</think>", "").trim();
        // 尝试提取 ```json 代码块
        Pattern pattern = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) return matcher.group(1);
        // 尝试提取纯 JSON
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start != -1 && end != -1) return response.substring(start, end + 1);
        return response;
    }

    /**
      生成月度报告
     */
    @GetMapping("/report")
    public ResponseEntity<?> generateMonthlyReport() {
        try {
            // 1. 获取本月所有账单 (复用现有的 Service 逻辑)
            List<Bill> bills = billService.getAllBills("month", null);

            if (bills.isEmpty()) {
                return ResponseEntity.ok(Map.of("content", "本月还没有账单哦，快去记一笔吧，那样我才能帮你分析！"));
            }

            // 2. 数据预处理：计算总支出、总收入、Top3 支出分类
            BigDecimal totalExpense = BigDecimal.ZERO;
            BigDecimal totalIncome = BigDecimal.ZERO;
            // 分类 -> 金额 映射
            Map<String, BigDecimal> categoryMap = new HashMap<>();

            for (Bill bill : bills) {
                if (bill.getBilltype() == Bill.BillType.EXPENSE) {
                    totalExpense = totalExpense.add(bill.getAmount());
                    String catName = bill.getCategory() != null ? bill.getCategory().getName() : "其他";
                    categoryMap.merge(catName, bill.getAmount(), BigDecimal::add);
                } else {
                    totalIncome = totalIncome.add(bill.getAmount());
                }
            }

            // 如果没花钱
            if (totalExpense.compareTo(BigDecimal.ZERO) == 0) {
                return ResponseEntity.ok(Map.of("content", "本月竟然没有支出？你是传说中的‘不食人间烟火’吗？"));
            }

            // 找出支出最高的 3 个分类
            String top3Categories = categoryMap.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(3)
                    .map(entry -> entry.getKey() + "(" + entry.getValue() + "元)")
                    .collect(Collectors.joining(", "));

            // 3. 构造 Prompt
            String promptText = String.format("""
                    你是一位幽默犀利、这就话有点损但很有道理的财务理财专家。
                    用户本月财务数据如下：
                    - 总支出：%.2f 元
                    - 总收入：%.2f 元
                    - 支出最高的分类：[%s]
                    
                    请对用户的消费情况进行点评。
                    要求：
                    1. 风格幽默，稍微带点“吐槽”或“夸奖”。
                    2. 分析用户的消费痛点（看Top分类）。
                    3. 给出一条简短的下月建议。
                    4. 总字数控制在 150 字以内。
                    5. 不要使用 Markdown 格式，直接返回纯文本。
                    """, totalExpense, totalIncome, top3Categories);

            // 4. 调用 AI
            String aiAnalysis = chatModel.call(promptText);

            // 5. 清理可能存在的 <think> 标签
            aiAnalysis = cleanAiResponse(aiAnalysis);

            return ResponseEntity.ok(Map.of("content", aiAnalysis));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "AI 分析生成失败: " + e.getMessage()));
        }
    }





}
