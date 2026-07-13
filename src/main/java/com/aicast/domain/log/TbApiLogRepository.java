package com.aicast.domain.log;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TbApiLogRepository extends JpaRepository<TbApiLog, String> {
    List<TbApiLog> findByCorrId(String corrId);
}
