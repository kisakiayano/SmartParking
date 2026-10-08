package com.smartparking.reservation.convert;

import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.vo.ReservationVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationConvert {

    ReservationVO toVO(Reservation reservation);

    List<ReservationVO> toVOList(List<Reservation> reservations);
}
