package com.commercehub.user.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercehub.user.api.dto.CreateUserRequest;
import com.commercehub.user.api.dto.UserResponse;
import com.commercehub.user.application.PasswordHasher;
import com.commercehub.user.application.UserService;
import com.commercehub.user.domain.entity.UserEntity;
import com.commercehub.user.domain.enumtype.UserRole;
import com.commercehub.user.exception.ConflictException;
import com.commercehub.user.exception.InvalidCredentialsException;
import com.commercehub.user.exception.UserNotFoundException;
import com.commercehub.user.infrastructure.persistence.UserRepository;
import com.commercehub.user.mapper.UserMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;

    UserService service;
    PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new PasswordHasher();
        service = new UserService(repository, new UserMapper(), hasher);
    }

    @Test
    void createNormalizesEmailAndStoresAHash() {
        when(repository.findByEmail("ana@commercehub.local")).thenReturn(Optional.empty());

        UserResponse response = service.create(new CreateUserRequest(
                "  Ana@CommerceHub.local ", "s3cret-pass", " Ana Customer ", "CUSTOMER"));

        assertThat(response.email()).isEqualTo("ana@commercehub.local");
        assertThat(response.fullName()).isEqualTo("Ana Customer");
        assertThat(response.roleCode()).isEqualTo("CUSTOMER");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).persistAndFlush(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("s3cret-pass");
        assertThat(hasher.matches("s3cret-pass", captor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(repository.findByEmail("ana@commercehub.local")).thenReturn(Optional.of(UserEntity.newUser()));

        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                "ana@commercehub.local", "s3cret-pass", "Ana", "CUSTOMER")))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).persistAndFlush(any());
    }

    @Test
    void authenticateAcceptsMatchingPassword() {
        UserEntity user = UserEntity.newUser();
        user.setEmail("ana@commercehub.local");
        user.setPasswordHash(hasher.hash("s3cret-pass"));
        user.setFullName("Ana");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        when(repository.findByEmail("ana@commercehub.local")).thenReturn(Optional.of(user));

        assertThat(service.authenticate("Ana@CommerceHub.local", "s3cret-pass").getId()).isEqualTo(user.getId());
    }

    @Test
    void authenticateRejectsWrongPasswordUnknownUserAndInactive() {
        when(repository.findByEmail("missing@commercehub.local")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.authenticate("missing@commercehub.local", "x"))
                .isInstanceOf(InvalidCredentialsException.class);

        UserEntity user = UserEntity.newUser();
        user.setEmail("ana@commercehub.local");
        user.setPasswordHash(hasher.hash("s3cret-pass"));
        user.setFullName("Ana");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        when(repository.findByEmail("ana@commercehub.local")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.authenticate("ana@commercehub.local", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);

        user.setActive(false);
        assertThatThrownBy(() -> service.authenticate("ana@commercehub.local", "s3cret-pass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void findByIdUnknownThrows() {
        when(repository.findByIdOptional("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById("missing")).isInstanceOf(UserNotFoundException.class);
    }
}
