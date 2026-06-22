package com.blog.service;

import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.repository.UserRepository;
import com.blog.security.JwtUtils;
import com.blog.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_shouldReturnToken_whenCredentialsValid() {
        // Arrange
        var request = new LoginRequest("admin", "password123");
        User user = User.builder()
                .id(1L)
                .username("admin")
                .nickname("Admin")
                .avatar("https://example.com/avatar.png")
                .role("ADMIN")
                .build();

        given(userRepository.findByUsername("admin")).willReturn(Optional.of(user));
        given(jwtUtils.generateToken(1L, "admin", "ADMIN")).willReturn("test-jwt-token");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("test-jwt-token");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().nickname()).isEqualTo("Admin");
        assertThat(response.user().role()).isEqualTo("ADMIN");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldThrow401_whenCredentialsInvalid() {
        // Arrange
        var request = new LoginRequest("admin", "wrong-password");

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 401)
                .hasMessage("Invalid username or password");
    }

    @Test
    void login_shouldCheckAdminRole() {
        // Arrange
        var request = new LoginRequest("user", "password");
        User nonAdminUser = User.builder()
                .id(2L)
                .username("user")
                .nickname("User")
                .role("USER")
                .build();

        given(userRepository.findByUsername("user")).willReturn(Optional.of(nonAdminUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 403)
                .hasMessage("Access denied: admin only");
    }

    @Test
    void login_shouldThrow401_whenUserNotFound() {
        // Arrange
        var request = new LoginRequest("ghost", "password");

        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 401)
                .hasMessage("Invalid username or password");
    }

    @Test
    void getCurrentUser_shouldReturnUserInfo_whenUserExists() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .username("admin")
                .nickname("Admin")
                .avatar("https://example.com/avatar.png")
                .role("ADMIN")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // Act
        var userInfo = authService.getCurrentUser(1L);

        // Assert
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.id()).isEqualTo(1L);
        assertThat(userInfo.username()).isEqualTo("admin");
        assertThat(userInfo.role()).isEqualTo("ADMIN");
    }

    @Test
    void getCurrentUser_shouldThrow404_whenUserNotFound() {
        // Arrange
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.getCurrentUser(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404)
                .hasMessage("User not found");
    }

    @Test
    void logout_shouldNotThrow() {
        // Act & Assert
        authService.logout();
        // No-op for JWT stateless, just verify no exception
    }
}
