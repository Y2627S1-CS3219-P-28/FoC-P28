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

    public AccountDeletionRecoveryJob(
            UserRepository userRepository,
            FirebaseAuthService firebaseAuthService) {
        this.userRepository = userRepository;
        this.firebaseAuthService = firebaseAuthService;
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