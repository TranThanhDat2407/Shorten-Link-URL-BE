package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyLinksResponse {
    LocalDate date;   // "2025-04-05"
    Long links;
}
