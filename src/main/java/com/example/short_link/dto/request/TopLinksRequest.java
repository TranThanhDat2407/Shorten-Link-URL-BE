package com.example.short_link.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TopLinksRequest {
    Integer limit;
    LocalDate from;
    LocalDate to;
}
