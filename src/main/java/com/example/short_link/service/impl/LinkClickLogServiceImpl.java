package com.example.short_link.service.impl;

import com.example.short_link.entity.Link;
import com.example.short_link.entity.LinkClickLog;
import com.example.short_link.exception.DataNotFoundException;
import com.example.short_link.repository.LinkClickLogRepository;
import com.example.short_link.repository.LinkRepository;
import com.example.short_link.service.LinkClickLogService;
import com.example.short_link.util.AuthenticationUtil;
import com.example.short_link.util.GeoInfo;
import com.example.short_link.util.IpGeolocationUtil;
import com.example.short_link.util.UserAgentParsingUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LinkClickLogServiceImpl implements LinkClickLogService {

    private final LinkClickLogRepository linkClickLogRepository;
    private final LinkRepository linkRepository;
    private final AuthenticationUtil authenticationUtil;

    private final UserAgentParsingUtil userAgentParsingUtil;
    private final IpGeolocationUtil ipGeolocationUtil;


    @Override
    public void logClickDetails(Link link, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        String device = userAgentParsingUtil.getDevice(userAgent);
        String browser = userAgentParsingUtil.getBrowser(userAgent);

        GeoInfo geoInfo = ipGeolocationUtil.lookup(ipAddress);

        LinkClickLog log =  LinkClickLog.builder()
                .clickedAt(Instant.now())
                .ip(ipAddress)
                .country(geoInfo.getCountry())
                .browser(browser)
                .device(device)
                .link(link)
                .build();

        linkClickLogRepository.save(log);
    }


    @Override
    public Page<LinkClickLog> getLogsByLinkId(Long linkId, Pageable pageable) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new DataNotFoundException("Link not found with id: " + linkId));

//        Long currentUserId = authenticationUtil.getCurrentAuthenticatedUser().getId();
//        if (!link.getUser().getId().equals(currentUserId)) {
//            throw new DataNotFoundException("Link not found with id: " + linkId);
//        }

        return linkClickLogRepository.findAllByLink(link, pageable);
    }


}
