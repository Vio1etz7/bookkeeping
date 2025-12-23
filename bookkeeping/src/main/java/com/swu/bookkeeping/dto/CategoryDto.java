package com.swu.bookkeeping.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {
    private Long id;
    private String name;
    private String icon;
    private String color;
    private String type; // INCOME or EXPENSE
    private UserDto user;
}
