package com.swu.bookkeeping.controller;

import com.swu.bookkeeping.dto.CategoryDto;
import com.swu.bookkeeping.dto.UserDto;
import com.swu.bookkeeping.model.Category;
import com.swu.bookkeeping.model.User;
import com.swu.bookkeeping.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取用户可用的分类（系统分类 + 用户自定义分类）
     */
    @GetMapping
    public List<CategoryDto> getAvailableCategories(@RequestParam Category.BillType type) {
        // 添加调试日志
        System.out.println("接收到的分类类型参数: " + type);
        System.out.println("参数类型: " + type.getClass().getName());


        List<Category> categories = categoryService.getAvailableCategories(type);
        System.out.println("查询到的分类数量: " + categories.size());

        return categories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    /**
     * 获取系统默认分类
     */
    @GetMapping("/system")
    public List<CategoryDto> getSystemCategories(@RequestParam Category.BillType type) {
        List<Category> categories = categoryService.getSystemCategories(type);
        return categories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    /**
     * 获取用户自定义分类
     */
    @GetMapping("/user")
    public List<CategoryDto> getUserCategories(@RequestParam Category.BillType type) {
        List<Category> categories = categoryService.getUserCategories(type);
        return categories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }



    /**
     * 根据ID获取分类
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        if (category != null) {
            CategoryDto dto = convertToDto(category);
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }


    /**
     * 创建用户自定义分类
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryDto categoryDto) {
        try {
            // 转换DTO到实体类
            Category category = convertToEntity(categoryDto);
            Category savedCategory = categoryService.createUserCategory(category);

            // 转换实体类到DTO返回
            CategoryDto responseDto = convertToDto(savedCategory);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryDto categoryDto) {
        try {
            // 转换DTO到实体类
            Category category = convertToEntity(categoryDto);
            Category updatedCategory = categoryService.updateCategory(id, category);

            if (updatedCategory != null) {
                CategoryDto responseDto = convertToDto(updatedCategory);
                return ResponseEntity.ok(responseDto);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        boolean deleted = categoryService.deleteCategory(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body(Map.of("error", "只能删除用户自定义分类"));
    }

    /**
     * DTO到实体类转换
     */
    private Category convertToEntity(CategoryDto dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setColor(dto.getColor());
        // 确保类型转换正确
        try {
            category.setType(Category.BillType.valueOf(dto.getType()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的分类类型: " + dto.getType());
        }

        if (dto.getUser() != null) {
            User user = new User();
            user.setId(dto.getUser().getId());
            user.setEmail(dto.getUser().getEmail());
            user.setUsername(dto.getUser().getUsername());
            user.setEnabled(dto.getUser().isEnabled());
            category.setUser(user);
        }

        return category;
    }


    /**
     * 实体类到DTO转换
     */
    private CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setType(category.getType().toString());
        dto.setColor(category.getColor());

        if (category.getUser() != null) {
            UserDto userDto = new UserDto();
            userDto.setId(category.getUser().getId());
            userDto.setEmail(category.getUser().getEmail());
            userDto.setUsername(category.getUser().getUsername());
            userDto.setEnabled(category.getUser().isEnabled());
            dto.setUser(userDto);
        }

        return dto;
    }


}