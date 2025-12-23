package com.swu.bookkeeping.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "type", "name"})
})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillType type;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;  // null = 系统分类, not null = 用户自定义分类

    private String icon;
    private String color;
    private Integer sortOrder = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum BillType{
        INCOME, EXPENSE
    }

    public void onCreate(){
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
        if(sortOrder == null) {
            sortOrder = 0;
        }
    }

}
