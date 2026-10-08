package com.smartparking.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
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
import com.smartparking.user.service.AuthService;
import com.smartparking.user.vo.LoginVO;
import com.smartparking.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserMapper userMapper;
    private final UserConvert userConvert;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenStateService tokenStateService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthServiceImpl(
            UserMapper userMapper,
            UserConvert userConvert,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenStateService tokenStateService,
            LoginRateLimiter loginRateLimiter
    ) {
        this.userMapper = userMapper;
        this.userConvert = userConvert;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenStateService = tokenStateService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        if (existsByUsername(request.username())) {
            throw new BusinessException(ResultCode.CONFLICT, "username already exists");
        }
        if (StringUtils.hasText(request.phone()) && existsByPhone(request.phone(), null)) {
            throw new BusinessException(ResultCode.CONFLICT, "phone already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setPhone(StringUtils.hasText(request.phone()) ? request.phone() : null);
        user.setRole(UserRole.USER.name());
        user.setStatus(UserStatus.ENABLED.name());
        user.setTokenVersion(0L);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);
        return userConvert.toVO(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        loginRateLimiter.checkAllowed(request.username());
        User user = selectByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginRateLimiter.onFailure(request.username());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "invalid username or password");
        }
        ensureEnabled(user);
        loginRateLimiter.onSuccess(request.username());
        return buildLoginVO(user);
    }

    @Override
    public LoginVO refresh(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(request.token());
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "invalid refresh token");
        }
        if (!JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TOKEN_TYPE, String.class))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refresh token required");
        }
        if (tokenStateService.isTokenDenied(claims.getId())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refresh token revoked");
        }
        Long userId = claims.get(JwtService.CLAIM_USER_ID, Long.class);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "invalid token user");
        }
        ensureEnabled(user);
        Long tokenVersion = claims.get(JwtService.CLAIM_TOKEN_VERSION, Long.class);
        if (tokenVersion == null || tokenStateService.currentVersion(userId) != tokenVersion) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "token expired, please login again");
        }
        // rotate: the presented refresh token must never be usable a second time
        tokenStateService.denyToken(claims.getId(), claims.getExpiration().toInstant());
        return buildLoginVO(user);
    }

    @Override
    public void logout(String authorizationHeader, LogoutRequest request) {
        boolean revoked = false;
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            revoked |= revokeToken(authorizationHeader.substring(BEARER_PREFIX.length()));
        }
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            revoked |= revokeToken(request.refreshToken());
        }
        if (!revoked) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "no valid token to revoke");
        }
    }

    private boolean revokeToken(String token) {
        try {
            Claims claims = jwtService.parseClaims(token);
            tokenStateService.denyToken(claims.getId(), claims.getExpiration().toInstant());
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private LoginVO buildLoginVO(User user) {
        long tokenVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
        String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getUsername(), user.getRole(), tokenVersion);
        String refreshToken = jwtService.generateRefreshToken(user.getUserId(), user.getUsername(), tokenVersion);
        return new LoginVO(accessToken, refreshToken, userConvert.toVO(user));
    }

    private User selectByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    private boolean existsByUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0;
    }

    private boolean existsByPhone(String phone, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getPhone, phone);
        if (excludeUserId != null) {
            wrapper.ne(User::getUserId, excludeUserId);
        }
        return userMapper.selectCount(wrapper) > 0;
    }

    private void ensureEnabled(User user) {
        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "user is disabled");
        }
    }
}
