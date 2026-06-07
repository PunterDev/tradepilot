export type RuleViolation = {
  id?: number;
  tradeId?: number;
  ruleCode?: string;
  ruleTitle?: string;
  category?: string;
  severity?: string;
  reason?: string;
  source?: string;
};
