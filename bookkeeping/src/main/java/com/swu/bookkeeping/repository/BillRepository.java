package com.swu.bookkeeping.repository;

import com.swu.bookkeeping.model.Bill;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    // 根据用户ID查找所有账单，按时间倒序
    List<Bill> findByUserIdOrderByCreateTimeDesc(Long userId);

    // 根据用户和类型查找
    List<Bill> findByUserIdAndBilltypeOrderByCreateTimeDesc(Long userId, Bill.BillType billtype);

    // 根据时间范围查找
    List<Bill> findByUserIdAndCreateTimeBetweenOrderByCreateTimeDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    // 根据分类查找
    List<Bill> findByUserIdAndCategoryIdOrderByCreateTimeDesc(Long userId, Long categoryId);

    //根据用户ID删除所有账单
    @Modifying
    @Transactional
    @Query("DELETE FROM Bill b WHERE b.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 统计收入总额
    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Bill b WHERE b.user.id = :userId AND b.billtype = 'INCOME' AND b.createTime BETWEEN :start AND :end")
    BigDecimal sumIncomeByUserAndDateRange(@Param("userId") Long userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    // 统计支出总额
    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Bill b WHERE b.user.id = :userId AND b.billtype = 'EXPENSE' AND b.createTime BETWEEN :start AND :end")
    BigDecimal sumExpenseByUserAndDateRange(@Param("userId") Long userId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    //计算用户在指定时间范围内的总支出
    @Query("SELECT SUM(b.amount) FROM Bill b WHERE b.user.id = :userId AND b.billtype = 'EXPENSE' AND b.createTime BETWEEN :startTime AND :endTime")
    BigDecimal sumExpenseByUserIdAndDateRange(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);


}