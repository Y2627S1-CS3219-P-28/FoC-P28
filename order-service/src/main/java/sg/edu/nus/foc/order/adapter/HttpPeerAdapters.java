package sg.edu.nus.foc.order.adapter;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.client.RestClient;
import sg.edu.nus.foc.order.application.CreditServicePort;
import sg.edu.nus.foc.order.application.SupplierServicePort;
import sg.edu.nus.foc.order.application.UserServicePort;

@Component
@ConditionalOnProperty(name="order.peers.mode", havingValue="http")
public class HttpPeerAdapters implements UserServicePort, SupplierServicePort, CreditServicePort {
    private final RestClient user; private final RestClient supplier; private final RestClient credit;
    public HttpPeerAdapters(@Value("${order.peers.user-url}") String userUrl,
                            @Value("${order.peers.supplier-url}") String supplierUrl,
                            @Value("${order.peers.credit-url}") String creditUrl) {
        this(RestClient.builder().baseUrl(userUrl).build(),
            RestClient.builder().baseUrl(supplierUrl).build(),
            RestClient.builder().baseUrl(creditUrl).build());
    }
    HttpPeerAdapters(RestClient user, RestClient supplier, RestClient credit) {
        this.user = user; this.supplier = supplier; this.credit = credit;
    }
    public String verifyRequester(String id,String auth){
        UserRoleContext context = call(user,"/api/users/role-context",auth,UserRoleContext.class);
        requireMatchingRole(context, id, "requester");
        return context.userId();
    }
    public String verifyCourier(String id,String auth){
        CourierEligibility eligibility = call(user,"/api/users/courier-eligibility",auth,CourierEligibility.class);
        if (eligibility == null || !eligibility.isCourierEligible()) {
            throw new IllegalStateException("Courier is not eligible to accept orders.");
        }
        UserRoleContext context = call(user,"/api/users/role-context",auth,UserRoleContext.class);
        requireMatchingRole(context, id, "courier");
        return context.userId();
    }
    public void validatePair(String pickup,String delivery,String auth){supplier.post().uri("/api/suppliers/validate").header(HttpHeaders.AUTHORIZATION,auth==null?"":auth).body(new Pair(pickup,delivery)).retrieve().toBodilessEntity();}
    public void reserve(String orderId,String requester,long amount,String auth){credit.put().uri("/api/credits/orders/{id}/reservation",orderId).header(HttpHeaders.AUTHORIZATION,auth==null?"":auth).body(new Reservation(requester,amount)).retrieve().toBodilessEntity();}
    public void settle(String commandId,String orderId,String requesterId,String courierId,long amount,long expectedOrderVersion,String auth){credit.post().uri("/api/credits/orders/{id}/settlement",orderId).header(HttpHeaders.AUTHORIZATION,auth==null?"":auth).body(new Settlement(commandId,requesterId,courierId,amount,expectedOrderVersion)).retrieve().toBodilessEntity();}
    public void release(String commandId,String orderId,String requesterId,long amount,String outcome,long expectedOrderVersion,String auth){credit.post().uri("/api/credits/orders/{id}/release",orderId).header(HttpHeaders.AUTHORIZATION,auth==null?"":auth).body(new Release(commandId,requesterId,amount,outcome,expectedOrderVersion)).retrieve().toBodilessEntity();}
    private static <T> T call(RestClient client,String path,String auth,Class<T> responseType){
        return client.get().uri(path).header(HttpHeaders.AUTHORIZATION,auth==null?"":auth).retrieve().body(responseType);
    }
    private static void requireMatchingRole(UserRoleContext context, String requestedId, String role) {
        if (context == null || context.userId() == null || context.userId().isBlank()
                || !context.userId().equals(requestedId)
                || context.roles() == null
                || context.roles().stream().noneMatch(value -> role.equalsIgnoreCase(value))) {
            throw new IllegalStateException("Authenticated user does not match the requested " + role + " identity.");
        }
    }
    private record Pair(String pickupSupplierId,String deliverySupplierId) {}
    private record Reservation(String requesterId,long amount) {}
    private record Settlement(String commandId,String requesterId,String courierId,long amount,long expectedOrderVersion) {}
    private record Release(String commandId,String requesterId,long amount,String outcome,long expectedOrderVersion) {}
    private record UserRoleContext(String userId, List<String> roles) {}
    private record CourierEligibility(boolean isCourierEligible) {}
}
