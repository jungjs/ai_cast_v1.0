package com.aicast.domain.monitor;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_res_log", indexes = {
    @Index(name = "idx_rl_chk_time", columnList = "chk_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbResLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "chk_time")
    private LocalDateTime chkTime;

    @Column(name = "cpu_pct", precision = 5, scale = 2)
    private BigDecimal cpuPct;

    @Column(name = "mem_mb")
    private Integer memMb;

    @Column(name = "mem_lmt_mb")
    private Integer memLmtMb;

    @Column(name = "net_rx")
    private Long netRx;

    @Column(name = "net_tx")
    private Long netTx;

    @Column(name = "disk_rd")
    private Long diskRd;

    @Column(name = "disk_wr")
    private Long diskWr;

    @PrePersist
    public void prePersist() {
        if (this.chkTime == null) {
            this.chkTime = LocalDateTime.now();
        }
    }
}
