package com.tradepilot.dto.ai;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
@Data
public class WeeklyReflectionRequest { @NotNull private LocalDate startDate; @NotNull private LocalDate endDate; }
