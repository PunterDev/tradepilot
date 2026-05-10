package com.tradepilot.ai;
import com.tradepilot.entity.Trade;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
@Component
public class ReflectionPromptBuilder {
    public String buildWeeklyReflectionPrompt(List<Trade> trades){ StringBuilder sb = new StringBuilder("Analyze these trades as a professional trading performance coach. Focus on discipline, execution, repeated mistakes, risk management, and emotional control. Do not predict markets.\n\nTRADES:\n"); for(Trade t: trades){ sb.append("- ").append(t.getTradeDate()).append(" ").append(t.getSymbol()).append(" ").append(t.getSide()).append(" ").append(t.getSetup()).append(" outcome=").append(t.getOutcome()).append(" R=").append(num(t.getPnlR())).append(" followedPlan=").append(t.getFollowedPlan()).append(" mistake=").append(t.getMistakeTag()).append(" notes=").append(txt(t.getNotes())).append(" psychology=").append(txt(t.getPsychologyNotes())).append("\n"); } return sb.toString(); }
    private String txt(String v){ return v == null || v.isBlank() ? "N/A" : v; }
    private String num(BigDecimal v){ return v == null ? "0" : v.toPlainString(); }
}
