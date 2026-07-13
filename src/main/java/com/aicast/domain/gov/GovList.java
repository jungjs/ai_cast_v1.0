package com.aicast.domain.gov;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "gov_list")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GovList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "api_key", columnDefinition = "char", nullable = false, unique = true)
    private String apiKey;

    @Column(name = "name", length = 100, nullable = false)
    private String govName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 기존 String govId 참조용 호환 메서드
    public String getGovId() {
        return id != null ? String.valueOf(id) : null;
    }
}
