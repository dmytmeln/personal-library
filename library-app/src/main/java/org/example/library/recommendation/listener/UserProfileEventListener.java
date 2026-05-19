package org.example.library.recommendation.listener;

import lombok.RequiredArgsConstructor;
import org.example.library.common.service.DebounceService;
import org.example.library.recommendation.event.UserProfileUpdatedEvent;
import org.example.library.recommendation.service.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class UserProfileEventListener {

    private final DebounceService debounceService;
    private final UserProfileService userProfileService;

    @Value("${library.recommendation.profile-rebuild-debounce:PT5M}")
    private Duration debounceDelay;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileUpdated(UserProfileUpdatedEvent event) {
        var userId = event.userId();
        debounceService.debounce(
                "user-profile-vector:" + userId,
                () -> userProfileService.rebuildUserProfileVector(userId),
                debounceDelay);
    }

}
