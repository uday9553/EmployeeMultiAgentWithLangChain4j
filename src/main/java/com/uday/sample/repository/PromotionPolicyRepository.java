package com.uday.sample.repository;

import com.uday.sample.model.PromotionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromotionPolicyRepository extends JpaRepository<PromotionPolicy, Long> {
    Optional<PromotionPolicy> findByDesignation(String value);
}
