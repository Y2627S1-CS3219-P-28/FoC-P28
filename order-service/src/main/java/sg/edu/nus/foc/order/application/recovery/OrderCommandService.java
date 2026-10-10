package sg.edu.nus.foc.order.application.recovery;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import sg.edu.nus.foc.order.api.mapper.OrderMapper;
import sg.edu.nus.foc.order.application.*;
import sg.edu.nus.foc.order.domain.*;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;
import sg.edu.nus.foc.order.domain.repository.CommandReceiptRepository;

/** Commit intent -> claim -> remote effect -> atomic local finalize/result. */
@Service
@RequiredArgsConstructor
public class OrderCommandService {
    private final OrderCommandStore store;
    private final OrderRepository orders;
    private final CommandReceiptRepository receipts;
    private final OrderCreationService creation;
    private final OrderAssignmentService assignment;
    private final OrderTransitionService transitions;
    private final UserServicePort users;
    private final SupplierServicePort suppliers;
    private final ObjectProvider<CreditCommandGateway> gateway;
    private final PlatformTransactionManager transactions;
    private final OrderMapper mapper;
    private final OrderAuditLogger audit;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    @Value("${order.peers.mode:mock}") private String mode;
    @Value("${order.commands.enabled:true}") private boolean flag;
    @Value("${order.commands.lease-seconds:120}") private int leaseSeconds;
    @Value("${order.commands.retry-seconds:15}") private int retrySeconds;
    @Value("${order.commands.batch-size:20}") private int batchSize;

    public boolean enabled() { return flag && "mock".equals(mode) && gateway.getIfAvailable() != null; }
    private void requireEnabled() {
        if (!enabled()) throw new OrderProblem("SERVICE_UNAVAILABLE", "Command recovery is disabled until the Credit protocol is verified.");
    }

    public CommandView submit(String key, CommandRequest input, String authorization) {
        requireEnabled();
        authorize(input, authorization);
        try (CommandScope ignored = new CommandScope(key)) {
            tx().executeWithoutResult(status -> {
                if (!"CREATE".equals(input.kind())) {
                    orders.getForUpdate(input.orderId()).orElseThrow(() -> OrderProblem.notFound("Order not found."));
                }
                // Legacy receipts have no immutable input/actor evidence. Never guess a migration binding.
                String operation = input.kind().equals("ABORT") ? "CANCEL_ACCEPTED" : input.kind();
                if (receipts.findExisting(operation, key).isPresent() && store.find(key).isEmpty()) {
                    throw OrderProblem.conflict("This key belongs to a legacy operation. Use its original workflow.");
                }
                store.register(key, input);
            });
        }
        return execute(key, authorization, true);
    }

    public CommandView status(String key, String actor, String authorization) {
        var saved = owned(key, actor, authorization);
        return view(saved);
    }

    public List<CommandView> pending(String actor, String authorization, int page, int size) {
        // USER identity must be verified before discovery. No arbitrary actor lookup.
        verifyReader(actor, authorization);
        return store.pendingFor(actor, page, size).stream().map(this::view).toList();
    }

    public long pendingCount(String actor) { return store.pendingCount(actor); }

    public CommandView resume(String key, String actor, String authorization) {
        requireEnabled(); owned(key, actor, authorization);
        return execute(key, authorization, true);
    }

    public void recoverDue() {
        if (!enabled()) return;
        for (String key : store.due(Math.max(1, Math.min(batchSize, 100)))) {
            try { execute(key, null, false); }
            catch (RuntimeException failure) { audit.action("COMMAND_RECOVERY", null, null, key, "pending"); }
        }
    }

    private OrderCommandStore.Entry owned(String key, String actor, String authorization) {
        var saved = store.get(key);
        if (!saved.actorId().equals(actor)) throw OrderProblem.forbidden("Only the command owner may access it.");
        verifyReader(actor, authorization);
        return saved;
    }

    private void verifyReader(String actor, String authorization) {
        // A past owned outcome is not a new courier task. Reuse already-validated
        // JWT identity/role facts without rechecking current courier eligibility.
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken caller) {
            boolean requester = caller.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_REQUESTER".equals(a.getAuthority()));
            var role = requester ? sg.edu.nus.foc.order.security.Role.REQUESTER : sg.edu.nus.foc.order.security.Role.COURIER;
            sg.edu.nus.foc.order.security.VerifiedOrderCaller.identityFor(role, actor);
        } else if (!actor.equals(users.verifyRequester(actor, authorization))) {
            throw OrderProblem.forbidden("Command owner mismatch.");
        }
    }

    private void authorize(CommandRequest input, String authorization) {
        String verified = switch (input.kind()) {
            case "CREATE" -> users.verifyRequester(input.actorId(), authorization);
            case "ACCEPT", "ABORT" -> users.verifyCourier(input.actorId(), authorization);
            default -> throw new OrderProblem("VALIDATION_ERROR", "Unsupported command operation.");
        };
        if (!input.actorId().equals(verified)) throw OrderProblem.forbidden("Command owner mismatch.");
    }

    private CommandView execute(String key, String authorization, boolean userResume) {
        var candidate = store.claim(key, UUID.randomUUID().toString(), Math.max(1, leaseSeconds), userResume);
        if (candidate.isEmpty()) return view(store.get(key));
        var claim = candidate.get();
        var mutation = mutation(claim);
        try (CommandScope ignored = new CommandScope(key)) {
            authorize(claim.input(), authorization);
            try {
                tx().executeWithoutResult(status -> {
                    store.assertOwned(claim);
                    validate(claim);
                });
                if (claim.kind().equals("CREATE")) {
                    var fields = claim.input().creation();
                    suppliers.validatePair(fields.pickup(), fields.delivery(), authorization);
                }
            } catch (OrderProblem invalid) {
                if (isAuthorization(invalid)) throw invalid;
                if (isTemporary(invalid)) throw invalid;
                if (claim.possibleEffect()) {
                    if (!gateway.getObject().compensate(key, mutation, claim.generation(), authorization)) {
                        store.defer(claim, "RECONCILIATION_REQUIRED", "We are checking the earlier Credit outcome. Please wait.", retrySeconds);
                        return view(store.get(key));
                    }
                }
                reject(claim, invalid.getCode(), safeRejection(invalid.getCode()));
                return view(store.get(key));
            }
            // Persist the uncertainty barrier BEFORE invoking Credit, even if this process crashes next.
            tx().executeWithoutResult(status -> store.markPossibleEffect(claim));
            var remote = gateway.getObject().execute(key, mutation, claim.generation(), authorization);
            if (!remote.applied()) {
                // Only an authoritative saved no-effect result may be returned as a rejection.
                reject(claim, remote.code(), safeRejection(remote.code()));
            } else {
                try {
                    tx().executeWithoutResult(status -> {
                        store.assertOwned(claim);
                        Order result = finalizeOrder(claim, authorization);
                        entityManager.flush(); // Include the committed JPA version in the saved replay result.
                        store.complete(claim, "SUCCESS", "SUCCEEDED", "Request completed.", mapper.toResponse(result));
                    });
                } catch (OrderProblem invalidAfterSuccess) {
                    if (isAuthorization(invalidAfterSuccess) || isTemporary(invalidAfterSuccess)) throw invalidAfterSuccess;
                    // Re-read/claim-fence before reversal: a stale worker must not compensate a new owner.
                    tx().executeWithoutResult(status -> store.assertOwned(claim));
                    if (gateway.getObject().compensate(key, mutation, claim.generation(), authorization)) {
                        reject(claim, "DEADLINE_OR_STATE_CHANGED", "The request is no longer valid; its Credit change was reversed.");
                    } else {
                        store.defer(claim, "RECONCILIATION_REQUIRED", "We are checking the earlier Credit outcome. Please wait.", retrySeconds);
                    }
                }
            }
        } catch (RuntimeException failure) {
            boolean auth = failure instanceof OrderProblem p && isAuthorization(p);
            store.defer(claim, auth ? "AUTHORIZATION_REQUIRED" : "RETRY_SCHEDULED",
                    auth ? "Sign in or continue to authorize recovery of this request."
                         : "Outcome not confirmed. We will safely retry this request.", Math.max(1, retrySeconds));
            audit.action("COMMAND_RECOVERY", claim.orderId(), claim.actorId(), key, auth ? "authorization_required" : "pending");
        }
        return view(store.get(key));
    }

    private void validate(OrderCommandStore.Entry claim) {
        var input = claim.input();
        switch (claim.kind()) {
            case "CREATE" -> {
                var f = input.creation();
                if (f == null) throw new OrderProblem("VALIDATION_ERROR", "Creation details are required.");
                Order.openWithId(claim.orderId(), input.actorId(), f.description(), f.pickup(), f.delivery(),
                        f.amount(), f.duration(), Instant.now(), f.expiresAt(), f.plan());
            }
            case "ACCEPT" -> locked(claim).validateAcceptance(input.actorId(), input.expectedVersion(), Instant.now());
            case "ABORT" -> locked(claim).validateAcceptedCancellation(input.actorId(), input.expectedVersion());
            default -> throw new OrderProblem("VALIDATION_ERROR", "Unsupported command.");
        }
    }

    private Order locked(OrderCommandStore.Entry claim) {
        return orders.getForUpdate(claim.orderId()).orElseThrow(() -> OrderProblem.notFound("Order not found."));
    }

    private Order finalizeOrder(OrderCommandStore.Entry claim, String authorization) {
        var input = claim.input();
        return switch (claim.kind()) {
            case "CREATE" -> {
                var f = input.creation();
                yield creation.createConfirmed(claim.key(), claim.orderId(), input.actorId(), f.description(),
                        f.pickup(), f.delivery(), f.amount(), f.duration(), f.expiresAt(), f.plan(), authorization);
            }
            case "ACCEPT" -> assignment.acceptConfirmed(claim.key(), claim.orderId(), input.actorId(), input.expectedVersion(), authorization);
            case "ABORT" -> transitions.abortConfirmed(claim.key(), claim.orderId(), input.actorId(), input.expectedVersion(), authorization);
            default -> throw new IllegalStateException("Unsupported saved command.");
        };
    }

    private CreditCommandGateway.Mutation mutation(OrderCommandStore.Entry claim) {
        return new CreditCommandGateway.Mutation(claim.kind(), claim.orderId(), claim.actorId(),
                claim.input().creation() == null ? 0 : claim.input().creation().amount());
    }
    private void reject(OrderCommandStore.Entry claim, String code, String message) {
        tx().executeWithoutResult(status -> store.complete(claim, "REJECTED", code, message, null));
    }
    private CommandView view(OrderCommandStore.Entry entry) {
        return new CommandView(entry.key(), entry.kind(), entry.status(), entry.outcome(), entry.reason(), entry.message(),
                entry.orderId(), entry.attempts(), entry.nextRetry(), entry.result());
    }
    private TransactionTemplate tx() { return new TransactionTemplate(transactions); }
    private static boolean isAuthorization(OrderProblem p) {
        return "UNAUTHENTICATED".equals(p.getCode()) || "FORBIDDEN".equals(p.getCode());
    }
    private static boolean isTemporary(OrderProblem p) {
        return "SERVICE_UNAVAILABLE".equals(p.getCode()) || "DEPENDENCY_UNAVAILABLE".equals(p.getCode());
    }
    private static String safeRejection(String code) {
        return "INSUFFICIENT_CREDITS".equals(code) ? "Request failed: insufficient available credits."
                : "Request could not be completed because its details or order state are no longer valid.";
    }
}
