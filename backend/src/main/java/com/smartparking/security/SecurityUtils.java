package com.smartparking.security;

import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return authUser;
    }
}
