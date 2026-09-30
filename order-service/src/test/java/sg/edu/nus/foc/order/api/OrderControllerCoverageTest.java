package sg.edu.nus.foc.order.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import sg.edu.nus.foc.order.application.*;
import sg.edu.nus.foc.order.domain.*;

class OrderControllerCoverageTest {
    private static final Instant START = Instant.parse("2026-09-30T00:00:00Z");
    private static final String AUTH = "Bearer token";
    private OrderCreationService creation;
    private OrderAssignmentService assignment;
    private OrderTransitionService transitions;
    private OrderQueryService queries;
    private OrderRepostService reposts;
    private LifecycleProcessingService lifecycle;
    private UserServicePort users;
    private OrderController controller;
    private Order order;

    @BeforeEach
    void setUp() {
        creation = mock(OrderCreationService.class);
        assignment = mock(OrderAssignmentService.class);
        transitions = mock(OrderTransitionService.class);
        queries = mock(OrderQueryService.class);
        reposts = mock(OrderRepostService.class);
        lifecycle = mock(LifecycleProcessingService.class);
        users = mock(UserServicePort.class);
        controller = new OrderController(creation, assignment, transitions, queries, reposts, lifecycle, users, "lifecycle-secret");
        order = Order.open("requester", "item", "pickup", "delivery", 2, 15, START, START.plusSeconds(86400));
        when(queries.get(order.getId())).thenReturn(order);
        when(queries.available(anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of(order)));
        when(queries.requestedBy(anyString(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of(order)));
        when(queries.courierFor(anyString(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of(order)));
    }

    @Test
    void createsReadsAndListsOrders() {
        when(creation.create(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyInt(), any(), any(), anyString())).thenReturn(order);
        OrderDtos.Create request = new OrderDtos.Create("create", "requester", "item", "pickup", "delivery", 2, 15,
            START.plusSeconds(86400), false, null, 0, 0);
        assertEquals(order.getId(), controller.create(request, AUTH).id());
        assertEquals(order.getId(), controller.get(order.getId()).id());
        assertEquals(1, controller.available(1, 20).items().size());
        when(users.verifyRequester("requester", AUTH)).thenReturn("requester");
        when(users.verifyCourier("courier", AUTH)).thenReturn("courier");
        assertEquals(1, controller.mine("requester", "requester", AUTH, 1, 20).items().size());
        assertEquals(1, controller.mine("courier", "courier", AUTH, 1, 20).items().size());
        assertThrows(OrderProblem.class, () -> controller.mine("other", "id", AUTH, 1, 20));
        verify(creation).create(eq("create"), eq("requester"), eq("item"), eq("pickup"), eq("delivery"), eq(2L),
            eq(15), eq(START.plusSeconds(86400)), isNull(), eq(AUTH));
    }

    @Test
    void createsWithAutomaticRepostPlanAndDispatchesActions() {
        Instant due = START.plusSeconds(3600);
        OrderDtos.Create request = new OrderDtos.Create("create", "requester", "item", "pickup", "delivery", 2, 15,
            START.plusSeconds(86400), true, due, 3, 20);
        when(creation.create(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyInt(), any(), any(), anyString())).thenReturn(order);
        controller.create(request, AUTH);
        verify(creation).create(eq("create"), eq("requester"), eq("item"), eq("pickup"), eq("delivery"), eq(2L),
            eq(15), eq(START.plusSeconds(86400)), argThat(plan -> plan.enabled() && plan.dueAt().equals(due)
                && plan.creditAmount() == 3 && plan.deliveryDurationMinutes() == 20), eq(AUTH));

        OrderDtos.Actor actor = new OrderDtos.Actor("command", "actor", 0);
        when(assignment.accept("command", order.getId(), "actor", 0, AUTH)).thenReturn(order);
        when(transitions.start(anyString(), eq(order.getId()), eq("actor"), eq(0L), eq(AUTH))).thenReturn(order);
        when(transitions.pickup(anyString(), eq(order.getId()), eq("actor"), eq(0L), eq(AUTH))).thenReturn(order);
        when(transitions.deliver(anyString(), eq(order.getId()), eq("actor"), eq(0L), eq(AUTH))).thenReturn(order);
        when(transitions.complete(anyString(), eq(order.getId()), eq("actor"), eq(0L), eq(AUTH))).thenReturn(order);
        when(transitions.cancel(anyString(), eq(order.getId()), eq("actor"), eq(0L), eq(AUTH))).thenReturn(order);
        when(reposts.configure(eq("config"), eq(order.getId()), eq("actor"), eq(0L), any(RepostPlan.class), eq(AUTH))).thenReturn(order);
        when(reposts.manual(eq("manual"), eq(order.getId()), eq("actor"), eq(0L), anyString(), anyLong(), anyInt(), any(), eq(AUTH))).thenReturn(order);
        assertEquals(order.getId(), controller.accept(order.getId(), actor, AUTH).id());
        assertEquals(order.getId(), controller.start(order.getId(), actor, AUTH).id());
        assertEquals(order.getId(), controller.pickup(order.getId(), actor, AUTH).id());
        assertEquals(order.getId(), controller.deliver(order.getId(), actor, AUTH).id());
        assertEquals(order.getId(), controller.complete(order.getId(), actor, AUTH).id());
        assertEquals(order.getId(), controller.cancel(order.getId(), actor, AUTH).id());
        OrderDtos.RepostConfig config = new OrderDtos.RepostConfig("config", "actor", 0, false, START, 1, 15);
        assertEquals(order.getId(), controller.configure(order.getId(), config, AUTH).id());
        OrderDtos.ManualRepost manual = new OrderDtos.ManualRepost("manual", "actor", 0, "item", 1, 15, START.plusSeconds(86400));
        assertEquals(order.getId(), controller.repost(order.getId(), manual, AUTH).id());
    }

    @Test
    void draftRequiresRequesterAndExpiredOrder() {
        when(users.verifyRequester("requester", AUTH)).thenReturn("requester");
        Order expired = Order.open("requester", "item", "pickup", "delivery", 2, 15, START.minusSeconds(3600), START);
        expired.expire(0, START);
        when(queries.get("expired")).thenReturn(expired);
        OrderDtos.Draft draft = controller.draft("expired", "requester", AUTH);
        assertEquals("item", draft.itemDescription());
        assertEquals("pickup", draft.pickupSupplierId());

        when(users.verifyRequester("other", AUTH)).thenReturn("other");
        assertThrows(OrderProblem.class, () -> controller.draft("expired", "other", AUTH));
        when(queries.get("open")).thenReturn(order);
        assertThrows(OrderProblem.class, () -> controller.draft("open", "requester", AUTH));
    }

    @Test
    void lifecycleEndpointsValidateTokenAndReturnCounts() {
        when(lifecycle.expireDue(any(), eq("Bearer lifecycle-secret"))).thenReturn(2);
        when(lifecycle.repostDue(any(), eq("Bearer lifecycle-secret"))).thenReturn(1);
        assertEquals(Map.of("expired", 2), controller.expire("lifecycle-secret"));
        assertEquals(Map.of("reposted", 1), controller.automaticRepost("lifecycle-secret"));
        assertThrows(OrderProblem.class, () -> controller.expire("wrong"));
        assertThrows(OrderProblem.class, () -> controller.automaticRepost(null));
    }
}
