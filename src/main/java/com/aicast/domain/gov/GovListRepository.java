package com.aicast.domain.gov;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GovListRepository extends JpaRepository<GovList, String> {

    // API Key로 지자체 정보 조회 (인증용)
    Optional<GovList> findByApiKeyAndIsActiveTrue(String apiKey);
}
