package sg.edu.nus.foc.order.application.adminmock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import sg.edu.nus.foc.order.api.dto.request.AdminMockSupplierRequest;
import sg.edu.nus.foc.order.api.dto.response.AdminMockOrderResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockPageResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockRoleResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockSupplierResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockUserResponse;
import sg.edu.nus.foc.order.api.dto.response.AdminMockTransactionResponse;
import sg.edu.nus.foc.order.api.mapper.AdminMockMapper;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.config.AdminMockCondition;

/** Isolated, process-local sample data. Never accesses a real database or peer. */
@Service
@Conditional(AdminMockCondition.class)
@RequiredArgsConstructor
public class AdminMockService {

    private final AdminMockMapper mapper;

    private final Map<String, AdminMockUserResponse> users = new LinkedHashMap<>();
    private final Map<String, AdminMockSupplierResponse> suppliers = new LinkedHashMap<>();
    private final List<AdminMockOrderResponse> orders = new ArrayList<>();
    private final Map<String, List<AdminMockTransactionResponse>> transactions = new LinkedHashMap<>();

    private boolean seeded;

    public AdminMockRoleResponse roles(String userId) {
        if (userId == null || userId.isBlank()) throw invalid("A current user ID is required.");
        return new AdminMockRoleResponse(userId, List.of("admin"));
    }

    public synchronized AdminMockPageResponse<AdminMockOrderResponse> orders(
            OrderStatus status, String orderId, int page, int size) {
        seed();
        List<AdminMockOrderResponse> matches = orders.stream()
                .filter(order -> status == null || status.name().equals(order.getStatus()))
                .filter(order -> orderId == null || orderId.isBlank() || order.getId().equals(orderId.strip()))
                .sorted(Comparator.comparing(AdminMockOrderResponse::getCreatedAt).reversed()
                        .thenComparing(AdminMockOrderResponse::getId))
                .toList();
        return page(matches, page, size, mapper::copyOrder);
    }

    public synchronized AdminMockPageResponse<AdminMockUserResponse> users(
            String searchBy, String q, int page, int size) {
        seed();
        validateSearch(searchBy);
        List<AdminMockUserResponse> matches = users.values().stream()
                .filter(user -> matches(searchBy, q, user.getUsername(), user.getUserId())).toList();
        return page(matches, page, size, mapper::copyUser);
    }

    public synchronized AdminMockUserResponse user(String id) {
        seed();
        return mapper.copyUser(required(users, id));
    }

    public synchronized AdminMockPageResponse<AdminMockTransactionResponse> transactions(String id, int page, int size) {
        seed();
        required(users, id);
        return page(required(transactions, id), page, size, mapper::copyTransaction);
    }

    public synchronized AdminMockUserResponse suspend(String id, Instant end) {
        seed();
        if (end == null || !end.isAfter(Instant.now())) throw invalid("Suspension end must be in the future.");
        AdminMockUserResponse user = required(users, id);
        if (!user.isActive()) throw OrderProblem.conflict("An inactive sample user cannot be suspended.");
        user.setCourierSuspended(true);
        user.setSuspensionEndDate(end);
        return mapper.copyUser(user);
    }

    public synchronized AdminMockUserResponse promote(String id) {
        seed();
        AdminMockUserResponse user = required(users, id);
        if (!user.isActive()) throw OrderProblem.conflict("An inactive sample user cannot be promoted.");
        List<String> roles = new ArrayList<>(user.getRoles());
        if (!roles.contains("admin")) roles.add("admin");
        user.setRoles(List.copyOf(roles));
        return mapper.copyUser(user);
    }

    public synchronized AdminMockUserResponse deactivateUser(String id) {
        seed();
        AdminMockUserResponse user = required(users, id);
        user.setActive(false);
        return mapper.copyUser(user);
    }

    public synchronized AdminMockPageResponse<AdminMockSupplierResponse> suppliers(
            String searchBy, String q, int page, int size) {
        seed();
        validateSearch(searchBy);
        List<AdminMockSupplierResponse> matches = suppliers.values().stream()
                .filter(supplier -> matches(searchBy, q, supplier.getName(), supplier.getId())).toList();
        return page(matches, page, size, mapper::copySupplier);
    }

    public synchronized AdminMockSupplierResponse createSupplier(AdminMockSupplierRequest request) {
        seed();
        AdminMockSupplierResponse supplier = mapper.toSupplier(request);
        supplier.setId("demo-supplier-" + UUID.randomUUID());
        supplier.setName(supplier.getName().strip());
        supplier.setType(supplier.getType().strip());
        supplier.setBuilding(supplier.getBuilding().strip());
        supplier.setCreatedAt(Instant.now());
        supplier.setUpdatedAt(supplier.getCreatedAt());
        suppliers.put(supplier.getId(), supplier);
        return mapper.copySupplier(supplier);
    }

    public synchronized AdminMockSupplierResponse supplier(String id) {
        seed();
        return mapper.copySupplier(required(suppliers, id));
    }

    public synchronized AdminMockSupplierResponse deactivateSupplier(String id) {
        seed();
        AdminMockSupplierResponse supplier = required(suppliers, id);
        supplier.setActive(false);
        supplier.setOpenNow(false);
        supplier.setUpdatedAt(Instant.now());
        return mapper.copySupplier(supplier);
    }

    private static void validateSearch(String searchBy) {
        if (!List.of("name", "id").contains(searchBy)) throw invalid("Search by must be name or id.");
    }

    private static boolean matches(String by, String q, String name, String id) {
        if (q == null || q.isBlank()) return true;
        return "id".equals(by) ? id.equals(q.strip())
                : name.toLowerCase(Locale.ROOT).contains(q.strip().toLowerCase(Locale.ROOT));
    }

    private static <T> T required(Map<String, T> records, String id) {
        T record = records.get(id);
        if (record == null) throw OrderProblem.notFound("Sample record was not found.");
        return record;
    }

    private static <T> AdminMockPageResponse<T> page(
            List<T> records, int page, int size, Function<T, T> copy) {
        if (page < 1 || page > 10000 || size < 1 || size > 100) throw invalid("Invalid page or size.");
        int from = Math.min((page - 1) * size, records.size());
        int to = Math.min(from + size, records.size());
        List<T> items = records.subList(from, to).stream().map(copy).toList();
        return new AdminMockPageResponse<>(items, page, size, records.size(), (records.size() + size - 1) / size);
    }

    private static OrderProblem invalid(String message) {
        return new OrderProblem("VALIDATION_ERROR", message);
    }

    private void seed() {
        if (seeded) return;
        Instant now = Instant.now();
        String[] names = {"Alex Tan", "Jamie Lee", "Priya Nair", "Daniel Lim", "Sarah Chen",
                "Amir Hassan", "Rachel Wong", "Marcus Teo", "Nadia Rahman", "Chris Ng",
                "Grace Koh", "Ryan Goh"};
        for (int index = 0; index < names.length; index++) {
            String id = String.format("demo-user-%03d", index + 1);
            users.put(id, new AdminMockUserResponse("sample-profile-" + (index + 1), id,
                    names[index], "demo" + (index + 1) + "@u.nus.edu", List.of("requester", "courier"),
                    index % 4, false, null, "+65 8000 " + String.format("%04d", index),
                    "NUS, Kent Ridge campus", true, 100 + index * 5, 5, 105 + index * 5));
        }
        for (AdminMockUserResponse user : users.values()) {
            String id = user.getUserId();
            transactions.put(id, List.of(
                    new AdminMockTransactionResponse(id + "-transaction-3", "RECEIVED", 10, "CREDIT",
                            now.minusSeconds(3600), "demo-user-002".equals(id) ? "demo-order-006" : "demo-history-completed-" + id),
                    new AdminMockTransactionResponse(id + "-transaction-2", "RESERVATION", 5, "DEBIT",
                            now.minusSeconds(7200), "demo-history-reserved-" + id),
                    new AdminMockTransactionResponse(id + "-transaction-1", "INITIAL_ALLOCATION",
                            user.getTotalBalance() - 10, "CREDIT", now.minusSeconds(259200), null)));
        }
        for (int index = 0; index < 14; index++) {
            String id = String.format("demo-supplier-%03d", index + 1);
            String name = index == 0 ? "The Deck" : index == 1 ? "Central Library" : "Campus Stop " + (index + 1);
            suppliers.put(id, new AdminMockSupplierResponse(id, name, index % 2 == 0 ? "Food" : "Facility",
                    index % 2 == 0 ? "Arts" : "COM3", "1", "Near the main entrance",
                    1.2966, 103.7764, "09:00", "18:00", null, index != 13, false,
                    now.minusSeconds(86400), now.minusSeconds(3600)));
        }
        OrderStatus[] statuses = {OrderStatus.OPEN, OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS,
                OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.COMPLETED,
                OrderStatus.CANCELLED, OrderStatus.ABORTED, OrderStatus.EXPIRED};
        for (int index = 0; index < 24; index++) {
            AdminMockOrderResponse order = new AdminMockOrderResponse();
            OrderStatus status = statuses[index % statuses.length];
            order.setId(String.format("demo-order-%03d", index + 1));
            order.setRequesterId("demo-user-001");
            order.setRequesterName("Alex Tan");
            boolean assigned = !List.of(OrderStatus.OPEN, OrderStatus.CANCELLED, OrderStatus.EXPIRED).contains(status);
            order.setCourierId(assigned ? "demo-user-002" : null);
            order.setCourierName(assigned ? "Jamie Lee" : null);
            order.setItemDescription(index % 2 == 0 ? "Collect lunch from The Deck" : "Deliver library notes to COM3");
            order.setPickupSupplierId("demo-supplier-001");
            order.setPickupSupplierName("The Deck");
            order.setDeliverySupplierId("demo-supplier-002");
            order.setDeliverySupplierName("Central Library");
            order.setOfferedCredits(5 + index);
            order.setStatus(status.name());
            order.setCreatedAt(now.minusSeconds(index * 3600L));
            order.setExpiresAt(order.getCreatedAt().plusSeconds(7200));
            order.setDeliveryTimeLimitMinutes(45);
            order.setVersion(index % 4);
            if (status == OrderStatus.ABORTED) order.setAttemptId("demo-attempt-" + index);
            orders.add(order);
        }
        seeded = true;
    }
}
