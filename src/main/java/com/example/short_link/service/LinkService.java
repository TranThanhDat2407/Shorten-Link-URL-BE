package com.example.short_link.service;

import com.example.short_link.entity.Link;

public interface LinkService {
    Link CreateShortLink(String originalUrl);

    Link getOriginalLinkByShortCode(String shortCode);
}
