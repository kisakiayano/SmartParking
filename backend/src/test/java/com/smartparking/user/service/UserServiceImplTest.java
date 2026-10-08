package com.smartparking.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.user.convert.UserConvert;
import com.smartparking.user.dto.ChangePasswordRequest;
import com.smartparking.user.dto.UpdateUserStatusRequest;
import com.smartparking.user.dto.UpdateProfileRequest;
import com.smartparking.user.dto.UserPageQuery;
import com.smartparking.user.entity.User;
import com.smartparking.user.enums.UserRole;
import com.smartparking.user.enums.UserStatus;
import com.smartparking.user.mapper.UserMapper;
import com.smartparking.user.service.impl.UserServiceImpl;
import com.smartparking.user.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserConvert userConvert;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.smartparking.security.TokenStateService tokenStateService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldGetCurrentUser() {
        User user = enabledUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userConvert.toVO(user)).thenReturn(new UserVO(1L, "user01", "User One", "13800000000", "USER", "ENABLED", null, null));

        UserVO result = userService.getCurrentUser(1L);

        assertThat(result.username()).isEqualTo("user01");
    }

    @Test
    void shouldUpdateProfile() {
        User user = enabledUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userConvert.toVO(user)).thenReturn(new UserVO(1L, "user01", "Updated", "13800000001", "USER", "ENABLED", null, null));

        UserVO result = userService.updateProfile(1L, new UpdateProfileRequest("Updated", "13800000001"));

        verify(userMapper).updateById(user);
        assertThat(user.getName()).isEqualTo("Updated");
        assertThat(user.getPhone()).isEqualTo("13800000001");
        assertThat(result.name()).isEqualTo("Updated");
    }

    @Test
    void shouldChangePasswordAndBumpTokenVersion() {
        User user = enabledUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("Password@123", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("new-encoded-password");

        userService.changePassword(1L, new ChangePasswordRequest("Password@123", "NewPassword@123"));

        verify(userMapper).update(isNull(), any());
        verify(tokenStateService).evictVersionCache(1L);
    }

    @Test
    void shouldRejectWrongOldPassword() {
        User user = enabledUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("bad-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest("bad-password", "NewPassword@123")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldPageUsersForAdmin() {
        User user = enabledUser();
        IPage<User> page = new Page<User>(1, 10, 1).setRecords(java.util.List.of(user));
        when(userMapper.selectPage(any(), any())).thenReturn(page);
        when(userConvert.toVOList(java.util.List.of(user))).thenReturn(java.util.List.of(
                new UserVO(1L, "user01", "User One", "13800000000", "USER", "ENABLED", null, null)
        ));

        PageResponse<UserVO> result = userService.pageForAdmin(new UserPageQuery(1L, 10L, "user", null, null, "USER", "ENABLED"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records().get(0).username()).isEqualTo("user01");
    }

    @Test
    void shouldUpdateUserStatusForAdmin() {
        User user = enabledUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userConvert.toVO(user)).thenReturn(new UserVO(1L, "user01", "User One", "13800000000", "USER", "DISABLED", null, null));

        UserVO result = userService.updateStatus(99L, 1L, new UpdateUserStatusRequest("DISABLED"));

        verify(userMapper).update(isNull(), any());
        verify(tokenStateService).evictVersionCache(1L);
        assertThat(result.status()).isEqualTo("DISABLED");
    }

    @Test
    void shouldRejectDisablingCurrentAdmin() {
        User admin = enabledUser();
        admin.setRole(UserRole.ADMIN.name());
        when(userMapper.selectById(1L)).thenReturn(admin);

        assertThatThrownBy(() -> userService.updateStatus(1L, 1L, new UpdateUserStatusRequest("DISABLED")))
                .isInstanceOf(BusinessException.class);
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
        return user;
    }
}
