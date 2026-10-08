package com.tradepilot.backtest.repository; import com.tradepilot.backtest.entity.BacktestRun; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface BacktestRunRepository extends JpaRepository<BacktestRun,Long>{ List<BacktestRun> findTop20ByUserIdOrderByCreatedAtDesc(Long userId); Optional<BacktestRun> findByIdAndUserId(Long id,Long userId); }
