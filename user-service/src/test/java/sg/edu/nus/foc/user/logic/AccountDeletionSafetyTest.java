package sg.edu.nus.foc.user.logic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.nus.foc.user.authentication.FirebaseAuthService;
import sg.edu.nus.foc.user.model.AccountStatus;
import sg.edu.nus.foc.user.model.User;
import sg.edu.nus.foc.user.repository.OutboxEventRepository;
import sg.edu.nus.foc.user.repository.UserRepository;
import tools.jackson.databind.json.JsonMapper;

class AccountDeletionSafetyTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final FirebaseAuthService firebase = mock(FirebaseAuthService.class);
    private final OrderServiceClient orders = mock(OrderServiceClient.class);
    private final UserService service = new UserService(repository, firebase, orders,
            mock(OutboxEventRepository.class), JsonMapper.builder().build());
    private User user;

    @BeforeEach
    void account() {
        user = new User();
        user.setUserId("student");
        user.setRoles(List.of("requester", "courier"));
        when(repository.findByUserId("student")).thenReturn(Optional.of(user));
    }

    @Test
    void anyActiveOrderBlocksBeforeMongoOrFirebaseMutation() {
        when(orders.canDeleteUser("student")).thenReturn(false);
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.deactivateUser("caller-token", "student"));
        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        verify(repository, never()).save(any());
        verifyNoInteractions(firebase);
    }

    @Test
    void unavailableEligibilityNeverBecomesPermission() {
        when(orders.canDeleteUser("student")).thenThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.deactivateUser("caller-token", "student"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.getStatusCode());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        verify(repository, never()).save(any());
        verifyNoInteractions(firebase);
    }

    @Test
    void noActiveOrderRetainsMongoDocumentAndDeletesFirebaseIdentity() {
        when(orders.canDeleteUser("student")).thenReturn(true);
        service.deactivateUser("caller-token", "student");
        assertEquals(AccountStatus.DEACTIVATED, user.getStatus());
        verify(orders).canDeleteUser("student");
        verify(firebase).deleteUser("student");
        verify(repository, times(2)).save(user);
        verify(repository, never()).delete(any());
    }

    @Test
    void lastAdministratorRuleIsPreserved() {
        user.setRoles(List.of("admin"));
        when(repository.countByRolesContainingAndStatus("admin", AccountStatus.ACTIVE)).thenReturn(1L);
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.deactivateUser("admin-token", "student"));
        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        verifyNoInteractions(orders, firebase);
    }

    @Test
    void uncertainFirebaseOutcomeRemainsPendingForRecovery() {
        when(orders.canDeleteUser("student")).thenReturn(true);
        doThrow(new IllegalStateException("lost Firebase response")).when(firebase).deleteUser("student");
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.deactivateUser("caller-token", "student"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.getStatusCode());
        assertEquals(AccountStatus.DEACTIVATION_PENDING, user.getStatus());
        verify(repository).save(user);
    }

    @Test
    void recoveryCannotBypassTheActiveOrderRuleAndContinuesOtherUsers() {
        User safe = new User();
        safe.setUserId("safe");
        safe.setStatus(AccountStatus.DEACTIVATION_PENDING);
        user.setStatus(AccountStatus.DEACTIVATION_PENDING);
        when(repository.findByStatus(AccountStatus.DEACTIVATION_PENDING)).thenReturn(List.of(user, safe));
        when(orders.canDeleteUser("student")).thenReturn(false);
        when(orders.canDeleteUser("safe")).thenReturn(true);
        new AccountDeletionRecoveryJob(repository, firebase, orders).recoverPendingDeletions();
        assertEquals(AccountStatus.DEACTIVATION_PENDING, user.getStatus());
        verify(firebase, never()).deleteUserIfExists("student");
        verify(repository, never()).save(user);
        verify(firebase).deleteUserIfExists("safe");
        assertEquals(AccountStatus.DEACTIVATED, safe.getStatus());
    }

    @Test
    void recoveryEligibilityOutageDoesNotDeleteTheAccount() {
        user.setStatus(AccountStatus.DEACTIVATION_PENDING);
        when(repository.findByStatus(AccountStatus.DEACTIVATION_PENDING)).thenReturn(List.of(user));
        when(orders.canDeleteUser("student")).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        assertDoesNotThrow(() -> new AccountDeletionRecoveryJob(repository, firebase, orders).recoverPendingDeletions());
        assertEquals(AccountStatus.DEACTIVATION_PENDING, user.getStatus());
        verifyNoInteractions(firebase);
        verify(repository, never()).save(any());
    }
}
