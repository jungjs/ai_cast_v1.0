package com.aicast.domain.log;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_api_log", indexes = {
    @Index(name = "idx_al_req_time", columnList = "req_time"),
    @Index(name = "idx_al_is_ok", columnList = "is_ok")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbApiLog {

    @Id
    @Column(name = "corr_id", length = 36)
    private String corrId;

    @Column(name = "api_key", length = 100)
    private String apiKey;

    @Column(name = "gov_name", length = 100)
    private String govName;

    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "req_time")
    private LocalDateTime reqTime;

    @Column(name = "res_time")
    private LocalDateTime resTime;

    @Column(name = "proc_ms")
    private Integer procMs;

    @Column(name = "is_ok", nullable = false)
    private Boolean isOk = true;

    @Column(name = "err_code", length = 50)
    private String errCode;

    @Column(name = "err_msg", columnDefinition = "TEXT")
    private String errMsg;

    @Column(name = "step_logs", columnDefinition = "json")
    private String stepLogs;

    @PrePersist
    public void prePersist() {
        if (this.reqTime == null) {
            this.reqTime = LocalDateTime.now();
        }
    }
}
