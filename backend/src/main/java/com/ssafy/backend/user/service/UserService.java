package com.ssafy.backend.user.service;

public interface UserService {
    /**
     * @param userId DB에 저장된 User의 PK
     */
    void deleteUserById(Long userId);
}
