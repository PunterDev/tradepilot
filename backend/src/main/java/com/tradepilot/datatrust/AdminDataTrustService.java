package com.tradepilot.datatrust;

import com.tradepilot.datatrust.dto.DataTrustQualityResponse;
import com.tradepilot.enums.UserRole;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDataTrustService {
  private final ImportReconciliationRepository reconciliations; private final InsightFeedbackRepository feedback;
  public AdminDataTrustService(ImportReconciliationRepository reconciliations, InsightFeedbackRepository feedback){this.reconciliations=reconciliations;this.feedback=feedback;}
  @PreAuthorize("hasRole('ADMIN')") @Transactional(readOnly=true) public DataTrustQualityResponse quality(){
    var imports=reconciliations.findAll().stream().filter(r -> included(r.getUser())).toList(); long total=imports.size(), matched=imports.stream().filter(r->r.getStatus()==ImportReconciliationStatus.MATCHED).count(), mismatch=imports.stream().filter(r->r.getStatus()==ImportReconciliationStatus.MISMATCH_REPORTED).count(), reviewed=matched+mismatch;
    var importRows=groupImport(imports, ImportReconciliation::getSource);
    var mismatchReasons=new TreeMap<String,Long>(); imports.forEach(r->{if(r.getMismatchReasons()!=null) Arrays.stream(r.getMismatchReasons().split(",")).filter(s->!s.isBlank()).forEach(reason->mismatchReasons.merge(reason,1L,Long::sum));});
    var ratings=feedback.findAll().stream().filter(f -> included(f.getUser())).toList(); long positive=ratings.stream().filter(InsightFeedback::isHelpful).count();
    return new DataTrustQualityResponse(new DataTrustQualityResponse.ImportTrust(total,reviewed,matched,mismatch,total-reviewed,rate(matched,reviewed),importRows,mismatchReasons),new DataTrustQualityResponse.InsightTrust(ratings.size(),positive,ratings.size()-positive,rate(positive,ratings.size()),groupFeedback(ratings,InsightFeedback::getFindingType),groupFeedback(ratings,InsightFeedback::getEvidenceStrength),groupFeedback(ratings,r->r.getEngineVersion()+" / "+r.getRuleVersion()),negativeReasons(ratings)));
  }
  private List<DataTrustQualityResponse.RateRow> groupImport(List<ImportReconciliation> rows, Function<ImportReconciliation,String> key){return rows.stream().collect(Collectors.groupingBy(r->Optional.ofNullable(key.apply(r)).orElse("UNKNOWN"),TreeMap::new,Collectors.toList())).entrySet().stream().map(e->{long yes=e.getValue().stream().filter(r->r.getStatus()==ImportReconciliationStatus.MATCHED).count(), no=e.getValue().stream().filter(r->r.getStatus()==ImportReconciliationStatus.MISMATCH_REPORTED).count();return new DataTrustQualityResponse.RateRow(e.getKey(),yes+no,yes,no,rate(yes,yes+no));}).toList();}
  private List<DataTrustQualityResponse.RateRow> groupFeedback(List<InsightFeedback> rows, Function<InsightFeedback,String> key){return rows.stream().collect(Collectors.groupingBy(key,TreeMap::new,Collectors.toList())).entrySet().stream().map(e->{long yes=e.getValue().stream().filter(InsightFeedback::isHelpful).count();return new DataTrustQualityResponse.RateRow(e.getKey(),e.getValue().size(),yes,e.getValue().size()-yes,rate(yes,e.getValue().size()));}).toList();}
  private Map<String,Long> negativeReasons(List<InsightFeedback> rows){var result=new TreeMap<String,Long>();rows.stream().filter(f->!f.isHelpful()&&f.getReason()!=null).forEach(f->result.merge(f.getReason().name(),1L,Long::sum));return result;}
  private Double rate(long numerator,long denominator){return denominator==0?null:Math.round((numerator*10000.0/denominator))/100.0;}
  private boolean included(com.tradepilot.entity.User user){return user != null && user.getRole()!= UserRole.ADMIN && !Boolean.TRUE.equals(user.getAnalyticsExcluded());}
}
