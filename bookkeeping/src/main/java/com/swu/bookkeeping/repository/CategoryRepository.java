package com.swu.bookkeeping.repository;

import com.swu.bookkeeping.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 根据类型查找分类
    List<Category> findByType(Category.BillType type);

    // 查找系统分类（user为null）
    List<Category> findByUserIsNullAndType(Category.BillType type);

    // 查找用户自定义分类（user不为null）
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.type = :type")
    List<Category> findByUserIdAndType(@Param("userId") Long userId, @Param("type") Category.BillType type);

    // 查找用户可用的所有分类（系统分类+用户自定义分类）
    @Query("SELECT c FROM Category c WHERE c.type = :type AND (c.user IS NULL OR c.user.id = :userId) ORDER BY c.sortOrder")
    List<Category> findAvailableCategoriesByTypeAndUser(
            @Param("type") Category.BillType type,
            @Param("userId") Long userId);

    // 检查分类名称是否已经存在（系统分类）
    boolean existsByNameAndTypeAndUserIsNull(String name, Category.BillType type);

    // 检查分类名称是否已经存在（用户自定义分类）
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.type = :type AND c.user.id = :userId")
    boolean existsByNameAndTypeAndUserId(@Param("name") String name,
                                         @Param("type") Category.BillType type,
                                         @Param("userId") Long userId);
}