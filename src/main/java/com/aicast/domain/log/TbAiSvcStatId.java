package com.aicast.domain.log;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TbAiSvcStatId implements Serializable {
    private LocalDate statDt;
    private String apiKey;
    private String svcType;
}
