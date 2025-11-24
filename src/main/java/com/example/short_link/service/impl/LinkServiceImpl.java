package com.example.short_link.service.impl;

import com.example.short_link.dto.request.LinkSearchRequest;
import com.example.short_link.entity.Link;
import com.example.short_link.entity.User;
import com.example.short_link.repository.LinkRepository;
import com.example.short_link.repository.spec.LinkSpecification;
import com.example.short_link.service.LinkService;
import com.example.short_link.util.AuthenticationUtil;
import com.example.short_link.util.Base62Converter;
import com.example.short_link.util.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {
    private final LinkRepository shortLinkRepository;
    private final Base62Converter base62Converter;
    private final AuthenticationUtil authenticationUtil;
    private final QrCodeService qrCodeService;

    @Transactional
    @Override
    public Link CreateShortLink(String originalUrl) throws Exception {
        User user = authenticationUtil.getCurrentAuthenticatedUser();

        Link link = Link.builder()
                .originalUrl(originalUrl)
                .clickCount(0L)
                .user(user)
                .build();

        link = shortLinkRepository.save(link);

        Long id = link.getId();

        //Chuyển ID sang Short Code (Hệ 62)
        String shortCode = base62Converter.encode(id);

        //Cập nhật Short Code vào bản ghi vừa tạo
        link.setShortCode(shortCode);

        qrCodeService.generateAndUploadQrCode(link);

        // Không cần gọi save() lần nữa nếu @Transactional đang mở, nhưng gọi
        // explicit save là an toàn và dễ đọc hơn.
        return shortLinkRepository.save(link);
    }

    @Override
    public Link getOriginalLinkByShortCode(String shortCode) {
        long id = base62Converter.decode(shortCode);

        Link link = shortLinkRepository.findById(id).orElse(null);

        if (link == null) return null;

        link.setClickCount(link.getClickCount() + 1);
        shortLinkRepository.save(link);

        return link;
    }

    @Override
    public Long totalCountByUserId(Long userId) {
        return shortLinkRepository.countByUserId(userId);
    }

    @Override
    public Page<Link> getAllLinks(LinkSearchRequest request, Pageable pageable) {
        Specification<Link> spec = Specification.unrestricted();

        Long userIdFilter = request.getUserIdAsLong();
        if (request.getUserId() != null) {
            // Param userId có mặt trong query string (dù là "null", "abc", "0", v.v.)
            if (userIdFilter == null || userIdFilter <= 0) {
                // "null", rỗng, 0, -1, hoặc chuỗi rác → chỉ lấy link guest
                spec = spec.and(LinkSpecification.hasOwner(null));
            } else {
                // Là số hợp lệ > 0 → filter theo user
                spec = spec.and(LinkSpecification.hasOwner(userIdFilter));
            }
        }

        spec = spec.and(LinkSpecification.containsShortCode(request.getShortCode()));
        spec = spec.and(LinkSpecification.containsOriginalUrl(request.getOriginalUrl()));


        return shortLinkRepository.findAll(spec, pageable);
    }
}
