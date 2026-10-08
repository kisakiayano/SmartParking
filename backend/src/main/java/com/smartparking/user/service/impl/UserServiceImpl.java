package com.smartparking.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import com.smartparking.security.TokenStateService;
import com.smartparking.user.convert.UserConvert;
import com.smartparking.user.dto.ChangePasswordRequest;
import com.smartparking.user.dto.UpdateUserStatusRequest;
import com.smartparking.user.dto.UpdateProfileRequest;
import com.smartparking.user.dto.UserPageQuery;
import com.smartparking.user.entity.User;
import com.smartparking.user.enums.UserStatus;
import com.smartparking.user.mapper.UserMapper;
import com.smartparking.user.service.UserService;
import com.smartparking.user.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserConvert userConvert;
    private final PasswordEncoder passwordEncoder;
    private final TokenStateService tokenStateService;

    public UserServiceImpl(
            UserMapper userMapper,
            UserConvert userConvert,
            PasswordEncoder passwordEncoder,
            TokenStateService tokenStateService
    ) {
        this.userMapper = userMapper;
        this.userConvert = userConvert;
        this.passwordEncoder = passwordEncoder;
        this.tokenStateService = tokenStateService;
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = requireEnabledUser(userId);
        return userConvert.toVO(user);
    }

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireEnabledUser(userId);
        if (StringUtils.hasText(request.phone()) && existsByPhone(request.phone(), userId)) {
            throw new BusinessException(ResultCode.CONFLICT, "phone already exists");
        }
        user.setName(request.name());
        user.setPhone(StringUtils.hasText(request.phone()) ? request.phone() : null);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return userConvert.toVO(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireEnabledUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "old password is incorrect");
        }
        // token_version 随密码一起原子递增，使该用户已签发的所有 token 立即失效
        userMapper.update(null, new UpdateWrapper<User>()
                .eq("user_id", userId)
                .set("password", passwordEncoder.encode(request.newPassword()))
                .set("update_time", LocalDateTime.now())
                .setSql("token_version = token_version + 1"));
        tokenStateService.evictVersionCache(userId);
    }

    @Override
    public PageResponse<UserVO> pageForAdmin(UserPageQuery query) {
        UserPageQuery normalizedQuery = query == null
                ? new UserPageQuery(null, null, null, null, null, null, null)
                : query;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(normalizedQuery.username())) {
            wrapper.like(User::getUsername, normalizedQuery.username().trim());
        }
        if (StringUtils.hasText(normalizedQuery.name())) {
            wrapper.like(User::getName, normalizedQuery.name().trim());
        }
        if (StringUtils.hasText(normalizedQuery.phone())) {
            wrapper.like(User::getPhone, normalizedQuery.phone().trim());
        }
        if (StringUtils.hasText(normalizedQuery.role())) {
            wrapper.eq(User::getRole, normalizedQuery.role());
        }
        if (StringUtils.hasText(normalizedQuery.status())) {
            wrapper.eq(User::getStatus, normalizedQuery.status());
        }
        wrapper.orderByDesc(User::getCreateTime);

        IPage<User> page = userMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        );
        return new PageResponse<>(
                userConvert.toVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Override
    @Transactional
    public UserVO updateStatus(Long currentUserId, Long targetUserId, UpdateUserStatusRequest request) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "user not found");
        }
        if (currentUserId.equals(targetUserId) && UserStatus.DISABLED.name().equals(request.status())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "cannot disable current admin");
        }
        // 禁用用户同时递增 token_version，使其已登录会话立即失效
        userMapper.update(null, new UpdateWrapper<User>()
                .eq("user_id", targetUserId)
                .set("status", request.status())
                .set("update_time", LocalDateTime.now())
                .setSql("token_version = token_version + 1"));
        tokenStateService.evictVersionCache(targetUserId);
        return userConvert.toVO(userMapper.selectById(targetUserId));
    }

    private User requireEnabledUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "user not found");
        }
        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "user is disabled");
        }
        return user;
    }

    private boolean existsByPhone(String phone, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .ne(User::getUserId, excludeUserId);
        return userMapper.selectCount(wrapper) > 0;
    }
}
