package com.swu.bookkeeping.controller;

import com.swu.bookkeeping.dto.BillDto;
import com.swu.bookkeeping.dto.CategoryDto;
import com.swu.bookkeeping.dto.DailyTrendDto;
import com.swu.bookkeeping.model.Bill;
import com.swu.bookkeeping.model.Category;
import com.swu.bookkeeping.service.BillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bill")
@CrossOrigin(origins = "*")
public class BillController {

    @Autowired
    private BillService billService;

    @GetMapping("/test")
    public String test() {
        return "OK";
    }


    /**
     * 添加新账单
     */
    @PostMapping
    public ResponseEntity<?> addBill(@RequestBody BillDto billDto) {
        try {
            //转换Dto到实体类
            Bill bill = convertToEntity(billDto);
            Bill savedBill = billService.addBill(bill);

            //转换实体类到Dto返回
            BillDto responseDto = convertToDto(savedBill);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取所有账单
     */

    @GetMapping
    public List<BillDto> getAllBills(@RequestParam( name = "period", required = false) String period,
                                     @RequestParam(name = "keyword" , required = false) String keyword) {
        List<Bill> bills = billService.getAllBills(period,keyword);

        return bills.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取账单
     */
    @GetMapping("/{id}")
    public ResponseEntity<BillDto> getBillById(@PathVariable Long id) {
        Bill bill = billService.getBillById(id);
        if (bill != null) {
            BillDto dto = convertToDto(bill);
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }


    /**
     * 更新账单
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBill(@PathVariable Long id, @RequestBody BillDto updatedBillDto) {
        try {
            // 转换DTO到实体类
            Bill updatedBill = convertToEntity(updatedBillDto);
            Bill bill = billService.updateBill(id, updatedBill);

            if (bill != null) {
                BillDto responseDto = convertToDto(bill);
                return ResponseEntity.ok(responseDto);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除账单
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBill(@PathVariable Long id) {
        try {
            billService.deleteBill(id);
            return ResponseEntity.ok(Map.of("message", "账单删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "删除失败"));
        }
    }

    /**
     * 获取账单统计信息
     */
    @GetMapping("/statistics")
    public BillService.BillStatistics getStatistics(@RequestParam(required = false) String period) {
        return billService.getStatistics(period);
    }


    // 转换方法

    /**
     * DTO到实体类转换
     */
    private Bill convertToEntity(BillDto dto) {
        Bill bill = new Bill();
        bill.setAmount(dto.getAmount());
        bill.setBilltype(Bill.BillType.valueOf(dto.getBilltype()));
        bill.setCurrency(dto.getCurrency());
        bill.setRemark(dto.getRemark());
        bill.setCreateTime(dto.getCreateTime());
        bill.setOriginalCurrency(dto.getOriginalCurrency());
        bill.setOriginalAmount(dto.getOriginalAmount());
        bill.setIsConverted(dto.getIsConverted());

        bill.setExchangeRate(dto.getExchangeRate());
        bill.setExchangeTime(dto.getExchangeTime());

        if (dto.getConversionStatus() != null) {
            bill.setConversionStatus(Bill.ConversionStatus.valueOf(dto.getConversionStatus()));
        } else {
            bill.setConversionStatus(Bill.ConversionStatus.NOT_NEEDED); // 设置默认值
        }

        if (dto.getCategory() != null) {
            Category category = new Category();
            category.setId(dto.getCategory().getId());
            category.setName(dto.getCategory().getName());
            category.setIcon(dto.getCategory().getIcon());
            category.setType(dto.getCategory().getType());
            bill.setCategory(category);
        }

        return bill;
    }


    private BillDto convertToDto(Bill bill) {
        BillDto dto = new BillDto();
        dto.setId(bill.getId());
        dto.setAmount(bill.getAmount());
        dto.setBilltype(bill.getBilltype().toString());
        dto.setCurrency(bill.getCurrency());
        dto.setRemark(bill.getRemark());
        dto.setCreateTime(bill.getCreateTime());
        dto.setOriginalCurrency(bill.getOriginalCurrency());
        dto.setOriginalAmount(bill.getOriginalAmount());
        dto.setIsConverted(bill.getIsConverted());
        dto.setConversionStatus(bill.getConversionStatus().toString());
        dto.setVersion(bill.getVersion());
        dto.setExchangeRate(bill.getExchangeRate());
        dto.setExchangeTime(bill.getExchangeTime());
        dto.setTags(bill.getTags());

        if (bill.getCategory() != null) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(bill.getCategory().getId());
            categoryDto.setName(bill.getCategory().getName());
            categoryDto.setIcon(bill.getCategory().getIcon());
            categoryDto.setType(bill.getCategory().getType().toString());
            dto.setCategory(bill.getCategory());
        }

        if (bill.getUser() != null) {
            dto.setUsername(bill.getUser().getUsername());
        }

        return dto;
    }

    /**
     * 获取消费趋势数据图表
     */
    @GetMapping("/trend")
    public ResponseEntity<List<DailyTrendDto>> getTrendData( @RequestParam(defaultValue = "7") int days) {
        List<DailyTrendDto> trendData = billService.getTrendData(days);
        return ResponseEntity.ok(trendData);
    }

}








