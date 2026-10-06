package com.tradepilot.manualbacktest.repository;

import com.tradepilot.manualbacktest.entity.ManualBacktestTradeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ManualBacktestTradeEventRepository extends JpaRepository<ManualBacktestTradeEvent, Long> { }
