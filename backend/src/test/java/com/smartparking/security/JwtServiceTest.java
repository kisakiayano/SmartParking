package com.smartparking.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(new JwtProperties(
            "test-smart-parking-secret-key-with-enough-length",
            3600,
            604800,
            "smart-parking-test"
    ));

    @Test
    void shouldGenerateAndParseAccessToken() {
        String token = jwtService.generateAccessToken(1L, "admin", "ADMIN", 3L);

        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.get(JwtService.CLAIM_USER_ID, Long.class)).isEqualTo(1L);
        assertThat(claims.get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("ADMIN");
        assertThat(claims.get(JwtService.CLAIM_TOKEN_TYPE, String.class)).isEqualTo(JwtService.TYPE_ACCESS);
        assertThat(claims.get(JwtService.CLAIM_TOKEN_VERSION, Long.class)).isEqualTo(3L);
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void shouldGenerateRefreshTokenWithTypeClaim() {
        String token = jwtService.generateRefreshToken(1L, "user01", 5L);

        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user01");
        assertThat(claims.get(JwtService.CLAIM_TOKEN_TYPE, String.class)).isEqualTo(JwtService.TYPE_REFRESH);
        assertThat(claims.get(JwtService.CLAIM_USER_ID, Long.class)).isEqualTo(1L);
        assertThat(claims.get(JwtService.CLAIM_TOKEN_VERSION, Long.class)).isEqualTo(5L);
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateAccessToken(1L, "admin", "ADMIN", 0L);
        // 篡改 payload 中间字符（尾部追加可能被 base64 容错截断）
        int middle = token.length() / 2;
        char flipped = token.charAt(middle) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, middle) + flipped + token.substring(middle + 1);

        assertThatThrownBy(() -> jwtService.parseClaims(tampered)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectShortSecret() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short-secret", 3600, 604800, "issuer")))
                .isInstanceOf(IllegalStateException.class);
    }
}
