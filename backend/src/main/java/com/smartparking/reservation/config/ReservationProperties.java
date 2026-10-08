package com.smartparking.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reservation")
public record ReservationProperties(
        long expirationGraceMinutes,
        long expirationScanIntervalMs,
        long expirationScanInitialDelayMs
) {
}
