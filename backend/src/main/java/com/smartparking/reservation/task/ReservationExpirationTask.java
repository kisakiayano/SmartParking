package com.smartparking.reservation.task;

import com.smartparking.reservation.service.ReservationExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationTask {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationTask.class);

    private final ReservationExpirationService reservationExpirationService;

    public ReservationExpirationTask(ReservationExpirationService reservationExpirationService) {
        this.reservationExpirationService = reservationExpirationService;
    }

    @Scheduled(
            fixedDelayString = "${reservation.expiration-scan-interval-ms:60000}",
            initialDelayString = "${reservation.expiration-scan-initial-delay-ms:30000}")
    public void expireStaleReservations() {
        try {
            int count = reservationExpirationService.expireStaleReservations();
            if (count > 0) {
                log.info("marked {} stale reservations as EXPIRED", count);
            }
        } catch (RuntimeException ex) {
            log.error("reservation expiration scan failed", ex);
        }
    }
}
