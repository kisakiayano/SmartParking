package com.smartparking.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenStateService tokenStateService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenStateService tokenStateService) {
        this.jwtService = jwtService;
        this.tokenStateService = tokenStateService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parseClaims(token);
                if (isUsableAccessToken(claims)) {
                    Long userId = claims.get(JwtService.CLAIM_USER_ID, Long.class);
                    String username = claims.getSubject();
                    String role = claims.get(JwtService.CLAIM_ROLE, String.class);
                    AuthUser principal = new AuthUser(userId, username, role);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isUsableAccessToken(Claims claims) {
        String tokenType = claims.get(JwtService.CLAIM_TOKEN_TYPE, String.class);
        if (tokenType != null && !JwtService.TYPE_ACCESS.equals(tokenType)) {
            return false;
        }
        String tokenId = claims.getId();
        if (tokenId != null && tokenStateService.isTokenDenied(tokenId)) {
            return false;
        }
        Long userId = claims.get(JwtService.CLAIM_USER_ID, Long.class);
        if (userId == null) {
            return false;
        }
        Long tokenVersion = claims.get(JwtService.CLAIM_TOKEN_VERSION, Long.class);
        if (tokenVersion == null) {
            // 旧版本签发的 token 无版本 claim：签名已校验通过，按原逻辑放行
            return true;
        }
        return tokenStateService.currentVersion(userId) == tokenVersion;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
