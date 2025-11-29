package com.example.short_link.service;

import com.example.short_link.dto.request.LinkSearchRequest;
import com.example.short_link.entity.Link;
import com.example.short_link.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LinkService {
    Link createShortLinkForGuest(String originalUrl, boolean generateQrCode) throws Exception;

    Link createShortLinkForUser(String originalUrl, User user, boolean generateQrCode) throws Exception;

    Link getOriginalLinkByShortCode(String shortCode);

    Long totalCountByUserId(Long userid);

    Page<Link> getAllLinks(LinkSearchRequest request, Pageable pageable);

    void deleteById (Long id);

    Link  replaceLinkById (String replaceLink, Long id);

}
