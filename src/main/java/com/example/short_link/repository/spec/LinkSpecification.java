package com.example.short_link.repository.spec;

import com.example.short_link.entity.Link;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class LinkSpecification {

    public static Specification<Link> hasOwner(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.isNull(root.get("user"));           // chỉ lấy link guest
            }
            if (userId <= 0) {
                return cb.conjunction();                      // <= 0 → không filter (lấy tất cả)
            }
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Link> containsOriginalUrl(String originalUrl){
        return (root, query, cb) ->{
            if (originalUrl == null || originalUrl.isBlank()) return null;
            return cb.like(cb.lower(root.get("originalUrl"))
                    , "%" + originalUrl.toLowerCase() + "%");
        };
    }

    public static Specification<Link> containsShortCode(String shortCode) {
        return (root, query, cb) -> {
            if (shortCode == null || shortCode.isBlank()) return null;
            return cb.like(cb.lower(root.get("shortCode"))
                    , "%" + shortCode.toLowerCase() + "%");
        };
    }

    public static Specification<Link> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;

            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            } else {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
        };
    }
}
