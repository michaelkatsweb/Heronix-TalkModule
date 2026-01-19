package com.heronix.talkmodule.repository;

import com.heronix.talkmodule.model.domain.UsageStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageStatisticRepository extends JpaRepository<UsageStatistic, Long> {

    Optional<UsageStatistic> findByStatisticDateAndMetricType(LocalDate date, String metricType);

    List<UsageStatistic> findByStatisticDate(LocalDate date);

    @Query("SELECT s FROM UsageStatistic s WHERE s.statisticDate >= :startDate AND s.statisticDate <= :endDate ORDER BY s.statisticDate ASC")
    List<UsageStatistic> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM UsageStatistic s WHERE s.metricType = :metricType ORDER BY s.statisticDate DESC")
    List<UsageStatistic> findByMetricTypeOrderByDateDesc(@Param("metricType") String metricType);

    @Query("SELECT SUM(s.metricValue) FROM UsageStatistic s WHERE s.metricType = :metricType AND s.statisticDate >= :since")
    Long sumMetricSince(@Param("metricType") String metricType, @Param("since") LocalDate since);

    @Query("SELECT AVG(s.activeUsers) FROM UsageStatistic s WHERE s.statisticDate >= :since")
    Double averageActiveUsersSince(@Param("since") LocalDate since);

    @Query("SELECT MAX(s.peakConcurrentUsers) FROM UsageStatistic s WHERE s.statisticDate >= :since")
    Long maxConcurrentUsersSince(@Param("since") LocalDate since);
}
