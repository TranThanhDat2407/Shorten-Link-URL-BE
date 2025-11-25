package com.example.short_link.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
public class DailyClickResponse {
    LocalDate date;   // "2025-04-05"
    Long clicks;

}
