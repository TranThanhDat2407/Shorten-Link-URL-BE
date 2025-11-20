package com.example.short_link.service.impl;

import com.example.short_link.entity.Link;
import com.example.short_link.entity.User;
import com.example.short_link.repository.LinkRepository;
import com.example.short_link.service.LinkService;
import com.example.short_link.util.AuthenticationUtil;
import com.example.short_link.util.Base62Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {
    private final LinkRepository shortLinkRepository;
    private final Base62Converter base62Converter;
    private final AuthenticationUtil authenticationUtil;

    @Transactional
    @Override
    public Link CreateShortLink(String originalUrl) {
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
}
