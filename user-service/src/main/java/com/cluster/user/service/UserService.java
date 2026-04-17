package com.cluster.user.service;

import com.cluster.user.domain.User;
import com.cluster.user.repo.UserRepository;
import com.cluster.user.web.dto.CreateUserRequest;
import com.cluster.user.web.dto.UserResponse;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @CachePut(cacheNames = "users", key = "#result.id()")
    public UserResponse create(CreateUserRequest request) {
        User u = new User();
        u.setUsername(request.username());
        u.setEmail(request.email());
        User saved = userRepository.save(u);
        return toResponse(saved);
    }

    @Cacheable(cacheNames = "users", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id).map(UserService::toResponse).orElse(null);
    }

    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }

    private static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getCreatedAt());
    }
}
