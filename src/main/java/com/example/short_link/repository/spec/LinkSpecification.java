package com.example.short_link.repository.spec;

import com.example.short_link.entity.Link;
import org.springframework.data.jpa.domain.Specification;

public class LinkSpecification {
    // lấy tất cả link theo email user
    public static Specification<Link> hasOwner(String email) {
        return (root, query, cb) -> {
            if (email == null || email.trim().isEmpty()) {
                return cb.isTrue(cb.literal(true)); // Không lọc
            }

            return cb.equal(root.get("user").get("email"), email);
        };
    }
}
