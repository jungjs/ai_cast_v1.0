package com.aicast.domain.log;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_ai_svc_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbAiSvcLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "corr_id", length = 36, nullable = false)
    private String corrId;

    @Column(name = "svc_type", length = 20, nullable = false)
    private String svcType;

    @Column(name = "api_key", length = 100, nullable = false)
    private String apiKey;

    @Column(name = "req_time", updatable = false, nullable = false)
    private LocalDateTime reqTime;

    @Column(name = "proc_ms")
    private Integer procMs;

    @Column(name = "is_ok", nullable = false)
    private Boolean isOk = true;

    @Column(name = "req_size")
    private Integer reqSize;

    @Column(name = "res_size")
    private Integer resSize;

    @Column(name = "err_msg", columnDefinition = "TEXT")
    private String errMsg;

    @PrePersist
    public void prePersist() {
        if (this.reqTime == null) {
            this.reqTime = LocalDateTime.now();
        }
    }
}
