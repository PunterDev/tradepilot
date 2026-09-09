package com.tradepilot.activation;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessException;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserFeaturePreviewService {
    public static final String WEEKLY_COACH_PREVIEW = "WEEKLY_COACH_PRO_PREVIEW";

    private final UserFeaturePreviewRepository repository;
    private final FeatureUsageService featureUsageService;
    private final boolean previewEnabled;

    public UserFeaturePreviewService(
            UserFeaturePreviewRepository repository,
            FeatureUsageService featureUsageService,
            @Value("${app.activation.pro-preview-enabled:true}") boolean previewEnabled
    ) {
        this.repository = repository;
        this.featureUsageService = featureUsageService;
        this.previewEnabled = previewEnabled;
    }

    @Transactional
    public UserFeaturePreview ensureWeeklyCoachPreviewUnlocked(User user, boolean firstInsightGenerated) {
        if (!isEligibleFreeUser(user) || !firstInsightGenerated || !previewEnabled) {
            return null;
        }
        Optional<UserFeaturePreview> existing = repository.findByUserIdAndFeatureKey(user.getId(), WEEKLY_COACH_PREVIEW);
        if (existing.isPresent()) {
            return existing.get();
        }
        UserFeaturePreview preview = new UserFeaturePreview();
        preview.setUserId(user.getId());
        preview.setFeatureKey(WEEKLY_COACH_PREVIEW);
        preview.setStatus(UserFeaturePreviewStatus.AVAILABLE);
        try {
            UserFeaturePreview saved = repository.saveAndFlush(preview);
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.PRO_PREVIEW_UNLOCKED, FeatureUsageEventType.USER_ACTION, null, "USER_FEATURE_PREVIEW", saved.getId().toString(), "activation", "{\"feature\":\"WEEKLY_COACH\"}");
            return saved;
        } catch (DataIntegrityViolationException duplicate) {
            return repository.findByUserIdAndFeatureKey(user.getId(), WEEKLY_COACH_PREVIEW).orElse(null);
        }
    }

    @Transactional
    public UserFeaturePreview reserveWeeklyCoachPreview(User user) {
        UserFeaturePreview preview = repository.findByUserIdAndFeatureKeyAndStatus(user.getId(), WEEKLY_COACH_PREVIEW, UserFeaturePreviewStatus.AVAILABLE)
                .orElseThrow(() -> new FeatureAccessException(Feature.WEEKLY_COACH, "Weekly Coach preview is not available."));
        preview.setStatus(UserFeaturePreviewStatus.RESERVED);
        preview.setReservedAt(LocalDateTime.now());
        return repository.save(preview);
    }

    @Transactional
    public void attachReservedResource(Long previewId, Long resourceId) {
        repository.findById(previewId).ifPresent(preview -> {
            preview.setReservedResourceId(resourceId);
            repository.save(preview);
        });
    }

    @Transactional
    public void releasePreview(Long previewId) {
        repository.findById(previewId)
                .filter(preview -> preview.getStatus() == UserFeaturePreviewStatus.RESERVED)
                .ifPresent(preview -> {
                    preview.setStatus(UserFeaturePreviewStatus.AVAILABLE);
                    preview.setReservedAt(null);
                    preview.setReservedResourceId(null);
                    repository.save(preview);
                });
    }

    @Transactional
    public void consumeReservedWeeklyCoachPreview(Long userId, Long snapshotId) {
        repository.findByUserIdAndFeatureKeyAndStatus(userId, WEEKLY_COACH_PREVIEW, UserFeaturePreviewStatus.RESERVED)
                .filter(preview -> snapshotId.equals(preview.getReservedResourceId()))
                .ifPresent(preview -> {
                    preview.setStatus(UserFeaturePreviewStatus.CONSUMED);
                    preview.setConsumedAt(LocalDateTime.now());
                    repository.save(preview);
                });
    }

    @Transactional
    public void releaseReservedWeeklyCoachPreview(Long userId, Long snapshotId) {
        repository.findByUserIdAndFeatureKeyAndStatus(userId, WEEKLY_COACH_PREVIEW, UserFeaturePreviewStatus.RESERVED)
                .filter(preview -> snapshotId.equals(preview.getReservedResourceId()))
                .ifPresent(preview -> {
                    preview.setStatus(UserFeaturePreviewStatus.AVAILABLE);
                    preview.setReservedAt(null);
                    preview.setReservedResourceId(null);
                    repository.save(preview);
                });
    }

    public boolean isEligibleFreeUser(User user) {
        return user != null && user.getId() != null && (user.getPlan() == null || user.getPlan() == PlanType.FREE);
    }
}
