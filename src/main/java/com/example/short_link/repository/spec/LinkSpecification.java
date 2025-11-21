package com.example.short_link.repository.spec;

import com.example.short_link.entity.Link;
import org.springframework.data.jpa.domain.Specification;

public class LinkSpecification {
    // lấy tất cả link theo email user
    public static Specification<Link> hasOwner(Long userId) {
        return (root, query, cb) -> {
            if (userId == null ) {
                return cb.isTrue(cb.literal(true)); // Không lọc
            }

            return cb.equal(root.get("user").get("id"), userId);
        };
    }
}
