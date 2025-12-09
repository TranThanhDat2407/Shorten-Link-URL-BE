package com.example.short_link.service.impl;

import com.example.short_link.dto.request.LinkSearchRequest;
import com.example.short_link.dto.request.UpdateLinkRequest;
import com.example.short_link.entity.Link;
import com.example.short_link.entity.User;
import com.example.short_link.exception.DataNotFoundException;
import com.example.short_link.repository.LinkRepository;
import com.example.short_link.repository.spec.LinkSpecification;
import com.example.short_link.service.LinkService;
import com.example.short_link.util.AuthenticationUtil;
import com.example.short_link.util.Base62Converter;
import com.example.short_link.util.QrCodeService;
import com.example.short_link.util.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {
    private final LinkRepository shortLinkRepository;
    private final Base62Converter base62Converter;
    private final AuthenticationUtil authenticationUtil;
    private final QrCodeService qrCodeService;
    private final RedisService redisService;
    private static final Duration TTL_LINK_CACHE = Duration.ofMinutes(30);

    @Override
    public Link createShortLinkForGuest(String originalUrl, boolean generateQrCode) throws Exception {
        return createShortLink(originalUrl, null, generateQrCode);
    }

    @Override
    public Link createShortLinkForUser(String originalUrl, User user, boolean generateQrCode)
            throws Exception {
        return createShortLink(originalUrl, user, generateQrCode);
    }

    public Link createShortLink(String originalUrl,
                                User user,
                                boolean generateQrCode) throws Exception {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Original URL cannot be empty.");
        }
        // chuẩn hóa url nếu thiếu http https
        String standardizedUrl = standardizeUrl(originalUrl);

        // Kiểm tra tính hợp lệ của URL đã chuẩn hóa
        if (!isValidUrl(standardizedUrl)) {
            throw new IllegalArgumentException("Invalid URL format: " + originalUrl);
        }

        Link link = Link.builder()
                .originalUrl(standardizedUrl)
                .clickCount(0L)
                .user(user)
                .build();

        link = shortLinkRepository.save(link);

        Long id = link.getId();

        //Chuyển ID sang Short Code (Hệ 62)
        String shortCode = base62Converter.encode(id);

        //Cập nhật Short Code vào bản ghi vừa tạo
        link.setShortCode(shortCode);

        // Chỉ tạo QR nếu người dùng yêu cầu
        if (generateQrCode) {
            qrCodeService.generateAndUploadQrCode(link);
        } else {
            link.setQrCodeUrl(null); // hoặc để trống tùy yêu cầu
        }

        // Không cần gọi save() lần nữa nếu @Transactional đang mở, nhưng gọi
        // explicit save là an toàn và dễ đọc hơn.
        return shortLinkRepository.save(link);
    }

    @Override
    public Link getOriginalLinkByShortCode(String shortCode) {
//        long id = base62Converter.decode(shortCode);
//
//        Link link = shortLinkRepository.findById(id).orElse(null);
//
//        return link;
        String cacheKey = RedisService.CACHE_SHORT + shortCode; // sl:code:abc123

        Link cachedLink = redisService.get(cacheKey, Link.class);

        if (cachedLink != null) {
            // Trả về đối tượng Detached Object từ cache
            return cachedLink;
        }

        //Cache miss: Truy vấn DB
        long id = base62Converter.decode(shortCode);
        Link link = shortLinkRepository.findById(id).orElse(null);

        // Nếu tìm thấy, lưu URL vào cache
        if (link != null) {
            redisService.set(cacheKey, link, TTL_LINK_CACHE);
        }

        // Trả về đối tượng Persistent từ DB
        return link;
    }


    @Override
    public void incrementClickCount(Link link) {
//        link.setClickCount(link.getClickCount() + 1);
//        shortLinkRepository.save(link);
        redisService.logTemporaryClick(link.getShortCode(),Duration.ofMinutes(30));

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

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            spec = spec.and(LinkSpecification.hasOwnerEmail(request.getEmail()));
        }


        if (request.getShortCode() != null && !request.getShortCode().trim().isEmpty()) {
            String keyword = request.getShortCode().trim();
            spec = spec.and(
                    LinkSpecification.containsShortCode(keyword)
                            .or(LinkSpecification.containsOriginalUrl(keyword))
            );
        }

        spec = spec.and(LinkSpecification.createdBetween(
                request.getCreatedFrom(), request.getCreatedTo()));

        return shortLinkRepository.findAll(spec, pageable);
    }

    @Override
    public void deleteById(Long id) {
        Link link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Link not found"));

        shortLinkRepository.delete(link);
    }

    @Override
    public Link replaceLinkById(UpdateLinkRequest request, Long id) {
        Link link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Link not found"));

        if(request.getOriginalUrl() != null && !request.getOriginalUrl().trim().isEmpty()){
            link.setOriginalUrl(request.getOriginalUrl() );
        }

        if(request.getTitle() != null && !request.getTitle().trim().isEmpty()){
            link.setTitle(request.getTitle());
        }

        return shortLinkRepository.save(link);
    }

    @Override
    public Link getLinkDetails(Long id){
        Link link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Link not found"));

        return link;
    }

    private boolean isValidUrl(String urlString) {
        try {
            // Kiểm tra cơ bản: URL phải có giao thức (protocol)
            // Nếu URL thiếu http:// hoặc https://, nó sẽ fail.
            // Tự động thêm http:// nếu cần trước khi kiểm tra là một tùy chọn,
            // nhưng tốt nhất nên để người dùng nhập đúng định dạng.
            new java.net.URL(urlString).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String standardizeUrl(String urlString) {
        String trimmedUrl = urlString.trim();
        // Sử dụng Regex để kiểm tra xem chuỗi có bắt đầu bằng http:
        // hoặc https:// (bỏ qua case) hay không
        if (!trimmedUrl.toLowerCase().matches("^https?://.*")) {
            // Nếu không có, thêm http:// vào đầu
            return "http://" + trimmedUrl;
        }
        return trimmedUrl;
    }

}
