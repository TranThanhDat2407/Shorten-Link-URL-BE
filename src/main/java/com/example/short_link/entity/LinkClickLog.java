package com.example.short_link.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "link_click_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkClickLog extends BaseEntity{
    @Column(name = "clicked_at")
    private Instant clicked_at;

    @Column(name = "ip")
    private String ip;

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Column(name = "device")
    private String device;

    @Column(name = "browser")
    private String browser;

    @ManyToOne
    @JoinColumn(name = "short_link_id")
    private Link link;
}
