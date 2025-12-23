package com.swu.bookkeeping.service;

import com.swu.bookkeeping.model.Category;
import com.swu.bookkeeping.model.User;
import com.swu.bookkeeping.repository.CategoryRepository;
import com.swu.bookkeeping.repository.UserRepository;
import com.swu.bookkeeping.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取用户可用的分类（系统分类 + 用户自定义分类）
     */
    public List<Category> getAvailableCategories(Category.BillType type) {
        Long userId = SecurityUtil.getCurrentUserId();
        return categoryRepository.findAvailableCategoriesByTypeAndUser(type, userId);
    }

    /**
     * 获取系统默认分类
     */
    public List<Category> getSystemCategories(Category.BillType type) {
        return categoryRepository.findByUserIsNullAndType(type);
    }

    /**
     * 获取用户自定义分类
     */
    public List<Category> getUserCategories(Category.BillType type) {
        Long userId = SecurityUtil.getCurrentUserId();
        return categoryRepository.findByUserIdAndType(userId, type);
    }

    /**
     * 根据ID获取分类
     */
    public Category getCategoryById(Long id) {
        Optional<Category> category = categoryRepository.findById(id);
        return category.orElse(null);
    }

    /**
     * 创建用户自定义分类
     */
    public Category createUserCategory(Category category) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 获取当前用户
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查名称是否已存在
        if (categoryRepository.existsByNameAndTypeAndUserId(category.getName(), category.getType(), userId)) {
            throw new RuntimeException("分类名称已存在");
        }

        category.setUser(currentUser);
        return categoryRepository.save(category);
    }

    /**
     * 更新分类
     */
    public Category updateCategory(Long id, Category category) {
        Category existingCategory = getCategoryById(id);
        if (existingCategory != null && existingCategory.getUser() != null) {
            // 只能更新用户自定义分类
            Long userId = SecurityUtil.getCurrentUserId();
            if (!existingCategory.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权更新此分类");
            }

            existingCategory.setName(category.getName());
            existingCategory.setIcon(category.getIcon());
            existingCategory.setColor(category.getColor());
            existingCategory.setSortOrder(category.getSortOrder());
            return categoryRepository.save(existingCategory);
        }
        return null;
    }

    /**
     * 删除分类
     */
    public boolean deleteCategory(Long id) {
        Category category = getCategoryById(id);
        if (category != null && category.getUser() != null) {
            // 只能删除用户自定义分类
            Long userId = SecurityUtil.getCurrentUserId();
            if (category.getUser().getId().equals(userId)) {
                categoryRepository.delete(category);
                return true;
            }
        }
        return false;
    }
}