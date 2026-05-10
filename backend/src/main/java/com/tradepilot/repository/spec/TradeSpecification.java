package com.tradepilot.repository.spec;

import com.tradepilot.dto.trade.TradeFilterRequest;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public final class TradeSpecification {
    private TradeSpecification() {}
    public static Specification<Trade> withFilters(User user, TradeFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (user != null) {
                predicates.add(cb.equal(root.get("user"), user));
            }

            if (filter.getSymbol() != null && !filter.getSymbol().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("symbol")), filter.getSymbol().trim().toUpperCase()));
            }

            if (filter.getSetup() != null) {
                predicates.add(cb.equal(root.get("setup"), filter.getSetup()));
            }

            if (filter.getSession() != null) {
                predicates.add(cb.equal(root.get("session"), filter.getSession()));
            }

            if (filter.getOutcome() != null) {
                predicates.add(cb.equal(root.get("outcome"), filter.getOutcome()));
            }

            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("tradeDate"), filter.getStartDate()));
            }

            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("tradeDate"), filter.getEndDate()));
            }

            if (filter.getAccountId() != null) {
                predicates.add(cb.equal(root.get("account").get("id"), filter.getAccountId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
