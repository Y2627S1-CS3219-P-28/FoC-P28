package sg.edu.nus.foc.user.logic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import com.mongodb.client.result.UpdateResult;
import sg.edu.nus.foc.user.model.AccountStatus;
import sg.edu.nus.foc.user.model.User;

class UserLifecycleStoreTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final UserLifecycleStore lifecycle = new UserLifecycleStore(mongo);

    @Test
    void staleProfileCannotReplaceLifecycleFieldsOrUpdateAnInactiveAccount() {
        User stale = new User(); stale.setUserId("student");
        assertThrows(ResponseStatusException.class, () -> lifecycle.saveActive(stale));
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(query.capture(), update.capture(), any(FindAndModifyOptions.class), eq(User.class));
        assertEquals(AccountStatus.ACTIVE, query.getValue().getQueryObject().get("status"));
        org.bson.Document fields = (org.bson.Document) update.getValue().getUpdateObject().get("$set");
        assertFalse(fields.containsKey("status"));
        assertFalse(fields.containsKey("deletionOperationId"));
        assertFalse(fields.containsKey("deletionCanRestore"));
    }

    @Test
    void aConcurrentDeletionCannotReplaceTheExistingOperation() {
        assertThrows(ResponseStatusException.class, () -> lifecycle.begin("student"));
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findAndModify(query.capture(), any(Update.class), any(FindAndModifyOptions.class), eq(User.class));
        assertEquals(AccountStatus.ACTIVE, query.getValue().getQueryObject().get("status"));
    }

    @Test
    void onlyMatchingUnsealedOperationCanRestoreAfterAnAuthoritativeDenial() {
        User pending = new User(); pending.setUserId("student");
        ReflectionTestUtils.setField(pending, "deletionOperationId", "saved-operation");
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertTrue(lifecycle.restoreAfterDenial(pending));
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).updateFirst(query.capture(), any(Update.class), eq(User.class));
        assertEquals("saved-operation", query.getValue().getQueryObject().get("deletionOperationId"));
        assertEquals(true, query.getValue().getQueryObject().get("deletionCanRestore"));
        assertEquals(AccountStatus.DEACTIVATION_PENDING, query.getValue().getQueryObject().get("status"));
    }
}
