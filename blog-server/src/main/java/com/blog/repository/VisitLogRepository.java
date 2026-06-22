package com.blog.repository;

import com.blog.entity.VisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    @Modifying
    @Query("DELETE FROM VisitLog v WHERE v.createTime < :time")
    void deleteByCreateTimeBefore(@Param("time") LocalDateTime time);
}
