package com.example.short_link.controller;

import com.example.short_link.dto.request.CreateShortCodeRequest;
import com.example.short_link.dto.response.CreateShortCodeResponse;
import com.example.short_link.dto.response.LinkResponse;
import com.example.short_link.entity.Link;
import com.example.short_link.service.LinkClickLogService;
import com.example.short_link.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/short-link")
public class LinkController {
    private final LinkService linkService;
    private final LinkClickLogService linkClickLogService;

    @Value("${spring.application.frontend-domain}")
    private String frontEndDomain;

    @PostMapping("/create")
    public ResponseEntity<CreateShortCodeResponse> createShortLink(
            @RequestBody CreateShortCodeRequest request) {

        Link link = linkService.CreateShortLink(request.getOriginalUrl());

        CreateShortCodeResponse response = CreateShortCodeResponse.builder()
                .code(link.getShortCode())
                .shortUrl(frontEndDomain + "/" + link.getShortCode())
                .originalUrl(link.getOriginalUrl())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/redirect/{shortCode}")
    public ResponseEntity<?> redirectToOriginal(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        Link link = linkService.getOriginalLinkByShortCode(shortCode);

        if (link == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Short link not found");
        }

        linkClickLogService.logClickDetails(link, request);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.getOriginalUrl()))
                .build();
    }

    @GetMapping
    public ResponseEntity<Page<LinkResponse>> getAllLinks(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable){
        Page<Link> result = linkService.getAllLinks(pageable);

        Page<LinkResponse> responses = result.map(
                link -> LinkResponse.fromEntity(link)
        );

        return ResponseEntity.ok(responses);
    }

}
