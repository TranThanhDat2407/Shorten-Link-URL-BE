package com.example.short_link.repository.spec;

import com.example.short_link.entity.User;
import com.example.short_link.enums.AuthProvider;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    public static Specification<User> containsEmail(String email) {
        return (root, query, cb) -> {
            if (email == null) return null;
            return cb.like(root.get("email"), "%" + email + "%");
        };
    }

    public static Specification<User> containsFullName(String fullName) {
        return (root, query, cb) -> {
            if (fullName == null) return null;
            return cb.like(cb.lower(root.get("fullName")), "%" + fullName.toLowerCase() + "%");
        };
    }

    public static Specification<User> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return null;
            return isActive ? cb.isTrue(root.get("isActive"))
                    : cb.isFalse(root.get("isActive"));
        };
    }

    public static Specification<User> hasProvider(AuthProvider provider) {
        return (root, query, cb) -> {
            if (provider == null) return null;
            return cb.equal(root.get("provider"), provider);
        };
    }


}
