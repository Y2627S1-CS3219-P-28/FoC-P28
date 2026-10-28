package sg.edu.nus.foc.user.logic;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sg.edu.nus.foc.user.model.AccountStatus;
import sg.edu.nus.foc.user.model.User;
import sg.edu.nus.foc.user.repository.UserRepository;
import sg.edu.nus.foc.user.authentication.FirebaseAuthService;

@Component
public class AccountDeletionRecoveryJob {

    private static final Logger log =
            LoggerFactory.getLogger(AccountDeletionRecoveryJob.class);

    private final UserRepository userRepository;
    private final FirebaseAuthService firebaseAuthService;
    private final OrderServiceClient orderServiceClient;

    public AccountDeletionRecoveryJob(
            UserRepository userRepository,
            FirebaseAuthService firebaseAuthService,
            OrderServiceClient orderServiceClient) {
        this.userRepository = userRepository;
        this.firebaseAuthService = firebaseAuthService;
        this.orderServiceClient = orderServiceClient;
    }

    @Scheduled(
        fixedDelayString =
            "${user.account-deletion.recovery-delay-ms:60000}"
    )
    public void recoverPendingDeletions() {
        List<User> pendingUsers =
                userRepository.findByStatus(
                        AccountStatus.DEACTIVATION_PENDING);

        for (User user : pendingUsers) {
            try {
                // Never let retries bypass the same eligibility rule as a new deletion.
                if (!orderServiceClient.canDeleteUser(user.getUserId())) {
                    log.warn("Account deactivation remains pending: active orders for userId={}", user.getUserId());
                    continue;
                }
                // Delete the Firebase account, or succeed if it
                // has already been deleted.
                firebaseAuthService.deleteUserIfExists(
                        user.getUserId());

                // Firebase deletion is now confirmed.
                user.setStatus(AccountStatus.DEACTIVATED);
                userRepository.save(user);

                log.info(
                        "Completed account deactivation for userId={}",
                        user.getUserId());

            } catch (Exception e) {
                // Leave the account pending so it can be retried.
                log.error(
                        "Could not complete account deactivation for userId={}. "
                        + "Will retry later.",
                        user.getUserId(),
                        e);
            }
        }
    }
}
