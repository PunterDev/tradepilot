package com.tradepilot.backtest.engine;
import com.tradepilot.backtest.model.*;
import java.math.*;
import java.util.*;
public class BacktestMetricsCalculator {
    public BacktestMetrics calculate(List<BacktestTradeResult> trades) {
        int wins=(int)trades.stream().filter(t->t.outcome()==BacktestTradeResult.Outcome.WIN).count();
        int losses=(int)trades.stream().filter(t->t.outcome()==BacktestTradeResult.Outcome.LOSS).count();
        int be=trades.size()-wins-losses;
        BigDecimal raw=sum(trades, false), weighted=sum(trades,true);
        BigDecimal positive=trades.stream().map(BacktestTradeResult::rawR).filter(r->r.signum()>0).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal negative=trades.stream().map(BacktestTradeResult::rawR).filter(r->r.signum()<0).map(BigDecimal::abs).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal winners=divide(positive,wins), losers=divide(negative.negate(),losses), average=divide(raw,trades.size());
        BigDecimal cumulativeRaw=BigDecimal.ZERO,cumulativeWeighted=BigDecimal.ZERO,peak=BigDecimal.ZERO,drawdown=BigDecimal.ZERO; int streak=0,maxStreak=0; List<BacktestMetrics.Point> curve=new ArrayList<>();
        for(int index=0;index<trades.size();index++){BacktestTradeResult t=trades.get(index); cumulativeRaw=cumulativeRaw.add(t.rawR()); cumulativeWeighted=cumulativeWeighted.add(t.weightedR()); peak=peak.max(cumulativeWeighted); drawdown=drawdown.max(peak.subtract(cumulativeWeighted)); streak=t.outcome()==BacktestTradeResult.Outcome.LOSS?streak+1:0; maxStreak=Math.max(maxStreak,streak); curve.add(new BacktestMetrics.Point(index+1,t.entryTime(),t.tradeDate(),t.rawR(),t.weightedR(),cumulativeRaw,cumulativeWeighted)); }
        return new BacktestMetrics(trades.size(),wins,losses,be,divide(BigDecimal.valueOf(wins),trades.size()),divide(BigDecimal.valueOf(wins),wins+losses),raw,weighted,average,average,negative.signum()==0?null:positive.divide(negative,6,RoundingMode.HALF_UP),drawdown,maxStreak,winners,losers,curve);
    }
    private BigDecimal sum(List<BacktestTradeResult> ts,boolean weighted){return ts.stream().map(weighted?BacktestTradeResult::weightedR:BacktestTradeResult::rawR).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private BigDecimal divide(BigDecimal a,int b){return b==0?BigDecimal.ZERO:a.divide(BigDecimal.valueOf(b),6,RoundingMode.HALF_UP);}
}
