package com.kct.jcredit.core_backend.util;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.kct.jcredit.core_backend.entity.Project;

public class ProjectSpecification {

    public static Specification<Project> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("projectStatus"), status);
    }

    public static Specification<Project> hasMethodology(String methodology) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("methodName")), "%" + methodology.toLowerCase() + "%");
    }

    public static Specification<Project> createdAtFrom(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Project> createdAtTo(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), to);
    }

    public static Specification<Project> updatedAtFrom(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("updatedAt"), from);
    }

    public static Specification<Project> updatedAtTo(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThan(root.get("updatedAt"), to);
    }

    public static Specification<Project> containsKeyword(String keyword) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("projectName")), "%" + keyword.toLowerCase() + "%");
    }
}
