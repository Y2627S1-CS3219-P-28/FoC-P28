package sg.edu.nus.foc.order.application.recovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import sg.edu.nus.foc.order.api.dto.response.OrderResponse;
import sg.edu.nus.foc.order.domain.OrderProblem;

@Repository
@RequiredArgsConstructor
public class OrderCommandStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public record Entry(String key, String actorId, String kind, String orderId,
                        CommandRequest input, String inputHash, String status, String outcome,
                        String reason, String message, int attempts, long generation,
                        String owner, Instant leaseExpiry, Instant nextRetry, boolean possibleEffect,
                        OrderResponse result) { }

    @Transactional
    public Entry register(String key, CommandRequest request) {
        if (key == null || key.isBlank() || key.length() > 128 || request.actorId() == null
                || request.actorId().isBlank() || request.actorId().length() > 128
                || !List.of("CREATE", "ACCEPT", "ABORT").contains(request.kind())
                || (!request.kind().equals("CREATE") && (request.orderId() == null || request.orderId().length() > 36))) {
            throw new OrderProblem("VALIDATION_ERROR", "Invalid command identity or operation.");
        }
        String payload = json.writeValueAsString(request);
        String target = request.kind().equals("CREATE") ? UUID.randomUUID().toString() : request.orderId();
        try {
            jdbc.update("""
                insert into order_commands(command_id, actor_id, kind, order_id, input_hash, payload)
                values (?, ?, ?, ?, ?, ?::jsonb) on conflict(command_id) do nothing
                """, key, request.actorId(), request.kind(), target, digest(payload), payload);
        } catch (DuplicateKeyException conflict) {
            throw OrderProblem.conflict("An unresolved request already guards this order.");
        }
        Entry saved = get(key);
        if (!saved.actorId().equals(request.actorId()) || !saved.inputHash().equals(digest(payload))) {
            throw OrderProblem.conflict("This command key belongs to different request details.");
        }
        return saved;
    }

    public Entry get(String key) {
        return find(key).orElseThrow(() -> OrderProblem.notFound("Command not found."));
    }

    public Optional<Entry> find(String key) {
        return jdbc.query("select * from order_commands where command_id=?", this::map, key).stream().findFirst();
    }

    @Transactional
    public Optional<Entry> claim(String key, String owner, int seconds, boolean authorizedResume) {
        int changed = jdbc.update("""
            update order_commands set owner=?, lease_expires_at=clock_timestamp() + (? * interval '1 second'),
              generation=generation+1, attempt_count=attempt_count+1, reason='PROCESSING',
              message='Processing your request.', updated_at=clock_timestamp()
            where command_id=? and status='PENDING'
              and (next_retry_at<=clock_timestamp() or ?)
              and (lease_expires_at is null or lease_expires_at<=clock_timestamp())
              and (reason<>'AUTHORIZATION_REQUIRED' or ?)
            """, owner, seconds, key, authorizedResume, authorizedResume);
        return changed == 1 ? Optional.of(get(key)) : Optional.empty();
    }

    public void assertOwned(Entry claim) {
        List<String> owned = jdbc.queryForList("""
            select command_id from order_commands where command_id=? and status='PENDING'
              and owner=? and generation=? and lease_expires_at>clock_timestamp() for update
            """, String.class, claim.key(), claim.owner(), claim.generation());
        if (owned.isEmpty()) throw OrderProblem.conflict("Command ownership changed; checking its outcome.");
    }

    public void markPossibleEffect(Entry claim) {
        assertOwned(claim);
        jdbc.update("update order_commands set possible_effect=true where command_id=?", claim.key());
    }

    public void complete(Entry claim, String outcome, String reason, String message, OrderResponse result) {
        assertOwned(claim);
        jdbc.update("""
            update order_commands set status='COMPLETED', outcome=?, reason=?, message=?, result=?::jsonb,
              owner=null, lease_expires_at=null, updated_at=clock_timestamp() where command_id=?
            """, outcome, reason, message, result == null ? null : json.writeValueAsString(result), claim.key());
    }

    @Transactional
    public void defer(Entry claim, String reason, String message, int delay) {
        jdbc.update("""
            update order_commands set reason=?, message=?, next_retry_at=clock_timestamp()+(? * interval '1 second'),
              owner=null, lease_expires_at=null, updated_at=clock_timestamp()
            where command_id=? and status='PENDING' and owner=? and generation=?
            """, reason, message, delay, claim.key(), claim.owner(), claim.generation());
    }

    public List<String> due(int limit) {
        return jdbc.queryForList("""
            select command_id from order_commands where status='PENDING' and next_retry_at<=clock_timestamp()
              and (lease_expires_at is null or lease_expires_at<=clock_timestamp())
              and reason<>'AUTHORIZATION_REQUIRED' order by next_retry_at limit ?
            """, String.class, limit);
    }

    public List<Entry> pendingFor(String actor, int page, int size) {
        return jdbc.query("select * from order_commands where actor_id=? and status='PENDING' order by created_at,command_id limit ? offset ?",
                this::map, actor, size, (long) (page - 1) * size);
    }

    public long pendingCount(String actor) {
        Long count = jdbc.queryForObject("select count(*) from order_commands where actor_id=? and status='PENDING'", Long.class, actor);
        return count == null ? 0 : count;
    }

    public void guard(String orderId) {
        Integer count = jdbc.queryForObject("""
            select count(*) from order_commands where order_id=? and status='PENDING'
              and command_id<>coalesce(?, '')
            """, Integer.class, orderId, CommandScope.current());
        if (count != null && count > 0) throw OrderProblem.conflict("This order has an unresolved request. Please wait.");
    }

    private Entry map(ResultSet r, int row) throws SQLException {
        return new Entry(r.getString("command_id"), r.getString("actor_id"), r.getString("kind"),
            r.getString("order_id"), json.readValue(r.getString("payload"), CommandRequest.class),
            r.getString("input_hash"), r.getString("status"), r.getString("outcome"),
            r.getString("reason"), r.getString("message"), r.getInt("attempt_count"), r.getLong("generation"),
            r.getString("owner"), instant(r, "lease_expires_at"), instant(r, "next_retry_at"),
            r.getBoolean("possible_effect"), r.getString("result") == null ? null
                : json.readValue(r.getString("result"), OrderResponse.class));
    }
    private static Instant instant(ResultSet r, String column) throws SQLException {
        var value = r.getTimestamp(column); return value == null ? null : value.toInstant();
    }
    private static String digest(String payload) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
