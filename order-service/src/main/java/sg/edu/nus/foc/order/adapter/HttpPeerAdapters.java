package sg.edu.nus.foc.order.adapter;

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
        user=RestClient.builder().baseUrl(userUrl).build(); supplier=RestClient.builder().baseUrl(supplierUrl).build(); credit=RestClient.builder().baseUrl(creditUrl).build();
    }
    public void verifyRequester(String id,String auth){call(user,"/api/users/role-context",auth,id);}
    public void verifyCourier(String id,String auth){call(user,"/api/users/courier-eligibility",auth,id);}
    public void validatePair(String pickup,String delivery,String auth){supplier.post().uri("/api/suppliers/validate").header(HttpHeaders.AUTHORIZATION,auth==null?"":auth).body(new Pair(pickup,delivery)).retrieve().toBodilessEntity();}
    public void reserve(String orderId,String requester,long amount,String auth){credit.put().uri("/api/credits/orders/{id}/reservation",orderId).header(HttpHeaders.AUTHORIZATION,auth==null?"":auth).body(new Reservation(requester,amount)).retrieve().toBodilessEntity();}
    private static void call(RestClient client,String path,String auth,String id){client.get().uri(path).header(HttpHeaders.AUTHORIZATION,auth==null?"":auth).header("X-User-Id",id).retrieve().toBodilessEntity();}
    private record Pair(String pickupSupplierId,String deliverySupplierId) {}
    private record Reservation(String requesterId,long amount) {}
}
