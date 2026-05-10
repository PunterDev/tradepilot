package com.tradepilot.repository;
import com.tradepilot.entity.AiReflection;
import com.tradepilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AiReflectionRepository extends JpaRepository<AiReflection, Long> { List<AiReflection> findByUserOrderByCreatedAtDesc(User user); }
