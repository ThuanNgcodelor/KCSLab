package com.example.app.intake.repository;

import com.example.app.intake.dto.ReceiptSearchCriteria;
import com.example.app.intake.entity.Receipt;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

public final class ReceiptSpecifications {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private ReceiptSpecifications() {}

    public static Specification<Receipt> byCriteria(ReceiptSearchCriteria criteria) {
        Specification<Receipt> specification = (root, query, builder) -> builder.conjunction();
        if (criteria == null) return specification;

        if (criteria.from() != null) {
            Instant from = criteria.from().atStartOfDay(BUSINESS_ZONE).toInstant();
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("receivedAt"), from));
        }
        if (criteria.to() != null) {
            Instant toExclusive = criteria.to().plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
            specification = specification.and((root, query, builder) ->
                    builder.lessThan(root.get("receivedAt"), toExclusive));
        }
        if (criteria.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), criteria.status()));
        }
        if (criteria.decision() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("acceptanceDecision"), criteria.decision()));
        }
        if (criteria.receivedBy() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("receivedBy").get("id"), criteria.receivedBy()));
        }
        if (hasText(criteria.productType())) {
            String pattern = containsPattern(criteria.productType());
            specification = specification.and((root, query, builder) -> {
                query.distinct(true);
                var samples = root.join("samples", JoinType.INNER);
                return builder.like(builder.lower(samples.get("productType")), pattern, '\\');
            });
        }
        if (hasText(criteria.query())) {
            String pattern = containsPattern(criteria.query());
            specification = specification.and((root, query, builder) -> {
                query.distinct(true);
                var customer = root.join("customer", JoinType.INNER);
                var samples = root.join("samples", JoinType.LEFT);
                return builder.or(
                        builder.like(builder.lower(root.get("receiptCode")), pattern, '\\'),
                        builder.like(builder.lower(root.get("sampleName")), pattern, '\\'),
                        builder.like(builder.lower(customer.get("name")), pattern, '\\'),
                        builder.like(builder.lower(customer.get("phone")), pattern, '\\'),
                        builder.like(builder.lower(samples.get("sampleCode")), pattern, '\\'),
                        builder.like(builder.lower(samples.get("name")), pattern, '\\')
                );
            });
        }
        return specification;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String containsPattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }
}
