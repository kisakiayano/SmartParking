package com.smartparking.reservation.service;

public interface ReservationExpirationService {

    /**
     * 将超过宽限期仍未入场的 ACTIVE 预约推进为 EXPIRED 并释放车位。
     *
     * @return 本次实际过期的预约数量
     */
    int expireStaleReservations();
}
