package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CountryResponse {
    String country;  // "Vietnam", "US", ...
    Long clicks;
}
