package com.commercehub.user.application;

import com.commercehub.user.api.dto.CreateUserRequest;
import com.commercehub.user.api.dto.UserResponse;
import com.commercehub.user.domain.entity.UserEntity;
import com.commercehub.user.domain.enumtype.UserRole;
import com.commercehub.user.exception.ConflictException;
import com.commercehub.user.exception.InvalidCredentialsException;
import com.commercehub.user.exception.UserNotFoundException;
import com.commercehub.user.infrastructure.persistence.UserRepository;
import com.commercehub.user.mapper.UserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository repository, UserMapper mapper, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.mapper = mapper;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.findByEmail(email).isPresent()) {
            throw ConflictException.duplicateEmail(email);
        }
        UserEntity entity = UserEntity.newUser();
        entity.setEmail(email);
        entity.setPasswordHash(passwordHasher.hash(request.password()));
        entity.setFullName(request.fullName().trim());
        entity.setRole(UserRole.valueOf(request.roleCode()));
        entity.setActive(true);
        try {
            repository.persistAndFlush(entity);
        } catch (ConstraintViolationException e) {
            throw ConflictException.duplicateEmail(email);
        }
        return mapper.toResponse(entity);
    }

    public UserResponse findById(String id) {
        return mapper.toResponse(load(id));
    }

    public UserEntity authenticate(String email, String password) {
        UserEntity user = repository.findByEmail(normalizeEmail(email)).orElse(null);
        if (user == null || !user.isActive() || !passwordHasher.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    private UserEntity load(String id) {
        return repository.findByIdOptional(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
