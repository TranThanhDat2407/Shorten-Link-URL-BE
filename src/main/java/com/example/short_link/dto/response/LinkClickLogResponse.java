package com.example.short_link.dto.response;

import com.example.short_link.entity.Link;
import com.example.short_link.entity.LinkClickLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class LinkClickLogResponse {
    private Instant clickedAt;
    private String ip;
    private String country;
    private String device;
    private String browser;

    public static LinkClickLogResponse fromEntity(LinkClickLog entity){
        return LinkClickLogResponse.builder()
                .clickedAt(entity.getClickedAt())
                .ip(entity.getIp())
                .country(entity.getCountry())
                .device(entity.getDevice())
                .browser(entity.getBrowser())
                .build();
    }
}
