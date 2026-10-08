package com.smartparking.user.service;

import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
import com.smartparking.security.JwtProperties;
import com.smartparking.security.JwtService;
import com.smartparking.security.LoginRateLimiter;
import com.smartparking.security.TokenStateService;
import com.smartparking.user.convert.UserConvert;
import com.smartparking.user.dto.LoginRequest;
import com.smartparking.user.dto.LogoutRequest;
import com.smartparking.user.dto.RefreshTokenRequest;
import com.smartparking.user.dto.RegisterRequest;
import com.smartparking.user.entity.User;
import com.smartparking.user.enums.UserRole;
import com.smartparking.user.enums.UserStatus;
import com.smartparking.user.mapper.UserMapper;
import com.smartparking.user.service.impl.AuthServiceImpl;
import com.smartparking.user.vo.LoginVO;
import com.smartparking.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserConvert userConvert;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenStateService tokenStateService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    private final JwtService jwtService = new JwtService(new JwtProperties(
            "test-smart-parking-secret-key-with-enough-length",
            3600,
            604800,
            "smart-parking-test"
    ));

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userMapper, userConvert, passwordEncoder, jwtService, tokenStateService, loginRateLimiter);
    }

    @Test
    void shouldRegisterUserWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("user01", "Password@123", "User One", "13800000000");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-password");
        when(userConvert.toVO(any())).thenReturn(new UserVO(1L, "user01", "User One", "13800000000", "USER", "ENABLED", null, null));

        UserVO result = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User inserted = captor.getValue();
        assertThat(inserted.getPassword()).isEqualTo("encoded-password");
        assertThat(inserted.getRole()).isEqualTo(UserRole.USER.name());
        assertThat(inserted.getStatus()).isEqualTo(UserStatus.ENABLED.name());
        assertThat(inserted.getTokenVersion()).isZero();
        assertThat(result.username()).isEqualTo("user01");
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        RegisterRequest request = new RegisterRequest("user01", "Password@123", "User One", "");

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldLoginAndIssueBothTokens() {
        User user = enabledUser();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("Password@123", "encoded-password")).thenReturn(true);
        when(userConvert.toVO(user)).thenReturn(new UserVO(1L, "user01", "User One", "13800000000", "USER", "ENABLED", null, null));

        LoginVO result = authService.login(new LoginRequest("user01", "Password@123"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        Claims accessClaims = jwtService.parseClaims(result.token());
        assertThat(accessClaims.getSubject()).isEqualTo("user01");
        assertThat(accessClaims.get(JwtService.CLAIM_TOKEN_TYPE, String.class)).isEqualTo(JwtService.TYPE_ACCESS);
        Claims refreshClaims = jwtService.parseClaims(result.refreshToken());
        assertThat(refreshClaims.get(JwtService.CLAIM_TOKEN_TYPE, String.class)).isEqualTo(JwtService.TYPE_REFRESH);
        assertThat(result.user().role()).isEqualTo("USER");
        verify(loginRateLimiter).onSuccess("user01");
    }

    @Test
    void shouldCountFailedLoginOnBadCredentials() {
        when(userMapper.selectOne(any())).thenReturn(enabledUser());
        when(passwordEncoder.matches("bad-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user01", "bad-password")))
                .isInstanceOf(BusinessException.class);

        verify(loginRateLimiter).onFailure("user01");
    }

    @Test
    void shouldRejectLoginWhenRateLimited() {
        org.mockito.Mockito.doThrow(
                        new BusinessException(ResultCode.TOO_MANY_REQUESTS, "too many failed login attempts"))
                .when(loginRateLimiter).checkAllowed("user01");

        assertThatThrownBy(() -> authService.login(new LoginRequest("user01", "Password@123")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getResultCode()).isEqualTo(ResultCode.TOO_MANY_REQUESTS));
    }

    @Test
    void shouldRejectDisabledUserLogin() {
        User user = enabledUser();
        user.setStatus(UserStatus.DISABLED.name());
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("Password@123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user01", "Password@123")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRotateRefreshToken() {
        String oldRefreshToken = jwtService.generateRefreshToken(1L, "user01", 0L);
        User user = enabledUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(tokenStateService.isTokenDenied(any())).thenReturn(false);
        when(tokenStateService.currentVersion(1L)).thenReturn(0L);
        when(userConvert.toVO(user)).thenReturn(new UserVO(1L, "user01", "User One", "13800000000", "USER", "ENABLED", null, null));

        LoginVO result = authService.refresh(new RefreshTokenRequest(oldRefreshToken));

        assertThat(result.token()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotEqualTo(oldRefreshToken);
        String oldJti = jwtService.parseClaims(oldRefreshToken).getId();
        verify(tokenStateService).denyToken(eq(oldJti), any());
    }

    @Test
    void shouldRejectAccessTokenAsRefreshToken() {
        String accessToken = jwtService.generateAccessToken(1L, "user01", "USER", 0L);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(accessToken)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getResultCode()).isEqualTo(ResultCode.UNAUTHORIZED));
    }

    @Test
    void shouldRejectRevokedRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(1L, "user01", 0L);
        when(tokenStateService.isTokenDenied(any())).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(refreshToken)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getResultCode()).isEqualTo(ResultCode.UNAUTHORIZED));
    }

    @Test
    void shouldRejectRefreshWhenTokenVersionChanged() {
        String refreshToken = jwtService.generateRefreshToken(1L, "user01", 0L);
        when(userMapper.selectById(1L)).thenReturn(enabledUser());
        when(tokenStateService.isTokenDenied(any())).thenReturn(false);
        when(tokenStateService.currentVersion(1L)).thenReturn(1L);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(refreshToken)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getResultCode()).isEqualTo(ResultCode.UNAUTHORIZED));
    }

    @Test
    void shouldLogoutRevokeBothTokens() {
        String accessToken = jwtService.generateAccessToken(1L, "user01", "USER", 0L);
        String refreshToken = jwtService.generateRefreshToken(1L, "user01", 0L);

        authService.logout("Bearer " + accessToken, new LogoutRequest(refreshToken));

        verify(tokenStateService).denyToken(eq(jwtService.parseClaims(accessToken).getId()), any());
        verify(tokenStateService).denyToken(eq(jwtService.parseClaims(refreshToken).getId()), any());
    }

    @Test
    void shouldRejectLogoutWithoutTokens() {
        assertThatThrownBy(() -> authService.logout(null, new LogoutRequest(null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getResultCode()).isEqualTo(ResultCode.BAD_REQUEST));
    }

    private User enabledUser() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("user01");
        user.setPassword("encoded-password");
        user.setName("User One");
        user.setPhone("13800000000");
        user.setRole(UserRole.USER.name());
        user.setStatus(UserStatus.ENABLED.name());
        user.setTokenVersion(0L);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }
}
