package com.example.short_link.controller;

import com.example.short_link.dto.request.CreateShortCodeRequest;
import com.example.short_link.dto.request.LinkSearchRequest;
import com.example.short_link.dto.request.UpdateLinkRequest;
import com.example.short_link.dto.response.CreateShortCodeResponse;
import com.example.short_link.dto.response.LinkResponse;
import com.example.short_link.dto.response.SimpleResponse;
import com.example.short_link.entity.Link;
import com.example.short_link.service.LinkClickLogService;
import com.example.short_link.service.LinkService;
import com.example.short_link.util.AuthenticationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/short-link")
public class LinkController {
    private final LinkService linkService;
    private final LinkClickLogService linkClickLogService;
    private final AuthenticationUtil authenticationUtil;

    @Value("${spring.application.frontend-domain}")
    private String frontEndDomain;

    @PostMapping("/create")
    public ResponseEntity<CreateShortCodeResponse> createShortLink(
            @RequestBody CreateShortCodeRequest request) throws Exception {

        Link link = linkService.CreateShortLink(request.getOriginalUrl());

        CreateShortCodeResponse response = CreateShortCodeResponse.builder()
                .code(link.getShortCode())
                .shortUrl(frontEndDomain + "/" + link.getShortCode())
                .qrCodeUrl(link.getQrCodeUrl())
                .originalUrl(link.getOriginalUrl())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // redirect
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirectToOriginal(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        Link link = linkService.getOriginalLinkByShortCode(shortCode);

        if (link == null) {
            // Trả 404 + đẩy về trang 404 của frontend
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/not-found"))
                    .build();
        }

        linkClickLogService.logClickDetails(link, request);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.getOriginalUrl()))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<LinkResponse>> getAllLinks(
            @ModelAttribute LinkSearchRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<Link> result = linkService.getAllLinks(request, pageable);

        Page<LinkResponse> responses = result.map(
                link -> LinkResponse.fromEntity(link)
        );

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my-links")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<LinkResponse>> getMyLinks(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        String currentUserId = String.valueOf(authenticationUtil.getCurrentAuthenticatedUser().getId());

        LinkSearchRequest request = new LinkSearchRequest();
        request.setUserId(currentUserId);

        Page<Link> page = linkService.getAllLinks(request, pageable);
        return ResponseEntity.ok(page.map(LinkResponse::fromEntity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SimpleResponse> deleteMyLink(@PathVariable Long id) {

        linkService.deleteById(id);

        return ResponseEntity.ok(SimpleResponse.builder()
                .success(true)
                .message("Link deleted")
                .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateMyLink(
            @PathVariable Long id,
            @RequestBody UpdateLinkRequest request) {

        Link updated = linkService.replaceLinkById(request.getOriginalUrl(), id);
        return ResponseEntity.ok(LinkResponse.fromEntity(updated));
    }

}
