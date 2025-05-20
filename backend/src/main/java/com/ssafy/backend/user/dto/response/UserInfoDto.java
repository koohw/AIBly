package com.ssafy.backend.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserInfoDto {
    private String name;
    private String email;
    private String createdAt;
}
