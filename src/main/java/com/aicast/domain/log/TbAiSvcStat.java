package com.aicast.domain.log;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tb_ai_svc_stat")
@IdClass(TbAiSvcStatId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbAiSvcStat {

    @Id
    @Column(name = "stat_dt")
    private LocalDate statDt;

    @Id
    @Column(name = "api_key", length = 100)
    private String apiKey;

    @Id
    @Column(name = "svc_type", length = 20)
    private String svcType;

    @Column(name = "gov_name", length = 100)
    private String govName;

    @Column(name = "tot_cnt", nullable = false)
    private Integer totCnt = 0;

    @Column(name = "ok_cnt", nullable = false)
    private Integer okCnt = 0;

    @Column(name = "fail_cnt", nullable = false)
    private Integer failCnt = 0;

    @Column(name = "avg_ms", nullable = false)
    private Integer avgMs = 0;

    @Column(name = "tot_tokens")
    private Integer totTokens = 0;

    @Column(name = "prompt_tokens")
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens")
    private Integer completionTokens = 0;
}
