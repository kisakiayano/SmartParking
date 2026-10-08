package com.smartparking.user.service;

import com.smartparking.common.response.PageResponse;
import com.smartparking.user.dto.ChangePasswordRequest;
import com.smartparking.user.dto.UpdateUserStatusRequest;
import com.smartparking.user.dto.UpdateProfileRequest;
import com.smartparking.user.dto.UserPageQuery;
import com.smartparking.user.vo.UserVO;

public interface UserService {

    UserVO getCurrentUser(Long userId);

    UserVO updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    PageResponse<UserVO> pageForAdmin(UserPageQuery query);

    UserVO updateStatus(Long currentUserId, Long targetUserId, UpdateUserStatusRequest request);
}
