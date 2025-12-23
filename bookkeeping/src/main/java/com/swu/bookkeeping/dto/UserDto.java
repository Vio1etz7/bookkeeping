package com.swu.bookkeeping.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;

    private String username;

    private String email;

    private boolean enabled;

    private String avatarUrl;//用于头像

    private String phone;

    private LocalDate birthday;

    private String gender;

    private String signature;

}
