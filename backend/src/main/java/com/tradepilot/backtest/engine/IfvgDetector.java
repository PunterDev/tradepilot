package com.tradepilot.backtest.engine;
import com.tradepilot.backtest.model.*; import java.math.*; import java.util.*;
/** Inversion is emitted after, never during, the violating candle close. */
public class IfvgDetector { private final FvgDetector fvg=new FvgDetector(); public Optional<PriceZone> invert(PriceZone original,BacktestBar closeBar,boolean requireDisplacement,boolean displacement){return !requireDisplacement||displacement?fvg.invert(original,closeBar,BigDecimal.ZERO):Optional.empty();} }
