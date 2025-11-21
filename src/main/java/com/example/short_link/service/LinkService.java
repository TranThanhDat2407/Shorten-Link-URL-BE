package com.example.short_link.service;

import com.example.short_link.dto.request.LinkSearchRequest;
import com.example.short_link.entity.Link;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LinkService {
    Link CreateShortLink(String originalUrl);

    Link getOriginalLinkByShortCode(String shortCode);

    Long totalCountByUserId(Long userid);

    Page<Link> getAllLinks(LinkSearchRequest request, Pageable pageable);

}
