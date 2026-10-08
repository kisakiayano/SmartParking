package com.smartparking.reservation.service;

import com.smartparking.common.response.PageResponse;
import com.smartparking.reservation.dto.CreateReservationRequest;
import com.smartparking.reservation.dto.ReservationPageQuery;
import com.smartparking.reservation.vo.ReservationVO;

public interface ReservationService {

    ReservationVO create(Long currentUserId, CreateReservationRequest request);

    ReservationVO getById(Long currentUserId, Long reservationId);

    void cancel(Long currentUserId, Long reservationId);

    PageResponse<ReservationVO> pageMine(Long currentUserId, ReservationPageQuery query);

    PageResponse<ReservationVO> pageForAdmin(ReservationPageQuery query);
}
