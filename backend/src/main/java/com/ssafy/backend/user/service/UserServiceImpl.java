package com.ssafy.backend.user.service;

import com.ssafy.backend.user.repository.UserRepository;
import com.ssafy.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public void deleteUserById(Long userId) {
        userRepository.deleteById(userId);
    }
}
