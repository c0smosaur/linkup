package com.core.linkup.reservation.reservation.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
@Getter
public class ReservationResponse {

    private String type;
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private Long price;

    private String seatType;
    private String seatCode;

}
