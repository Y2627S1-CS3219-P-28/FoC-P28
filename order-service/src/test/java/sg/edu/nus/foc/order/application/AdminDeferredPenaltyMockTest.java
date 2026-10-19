package sg.edu.nus.foc.order.application;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import sg.edu.nus.foc.order.application.adminmock.AdminMockService;
import sg.edu.nus.foc.order.api.mapper.AdminMockMapper;
import sg.edu.nus.foc.order.api.dto.request.ApplyPenaltyRequest;
import sg.edu.nus.foc.order.api.dto.response.AdminMockOrderResponse;
import sg.edu.nus.foc.common.penalty.PenaltySystemPolicy;
import sg.edu.nus.foc.order.domain.OrderProblem;
class AdminDeferredPenaltyMockTest {
    private final AdminMockService service=new AdminMockService(Mappers.getMapper(AdminMockMapper.class));
    private ApplyPenaltyRequest input(AdminMockOrderResponse o,String cmd,PenaltySystemPolicy policy){ApplyPenaltyRequest i=new ApplyPenaltyRequest();i.setCommandId(cmd);i.setCourierId(o.getCourierId());i.setExpectedVersion(o.getVersion());i.setPolicy(policy);i.setReason("Confirmed: "+cmd);return i;}
    @Test void sampleStoresMaximumReplaysAndCancellationCannotClearIt(){
        AdminMockOrderResponse original=service.sampleOrder("demo-order-002");
        ApplyPenaltyRequest first=input(original,"first",PenaltySystemPolicy.ITEM_MISHANDLING_OR_DAMAGE);
        AdminMockOrderResponse stored=service.applyPenalty(original.getId(),first);
        assertEquals(4,stored.getAppliedPenaltyAmount());assertEquals(original.getVersion()+1,stored.getVersion());
        assertEquals(stored.getVersion(),service.applyPenalty(original.getId(),first).getVersion());
        AdminMockOrderResponse lower=service.applyPenalty(original.getId(),input(stored,"lower",PenaltySystemPolicy.WRONG_LOCATION_DELIVERY));
        assertEquals(4,lower.getAppliedPenaltyAmount());assertEquals("Confirmed: first",lower.getAppliedPenaltyReason());
        AdminMockOrderResponse highest=service.applyPenalty(original.getId(),input(lower,"theft",PenaltySystemPolicy.DELIBERATE_ITEM_RETENTION_OR_THEFT));
        assertEquals(10,highest.getAppliedPenaltyAmount());
        first.setReason("Changed");assertThrows(OrderProblem.class,()->service.applyPenalty(original.getId(),first));
        AdminMockOrderResponse cancelled=service.resolveOrder(original.getId(),original.getRequesterId(),original.getCourierId(),List.of(),"Refund");
        assertEquals("CANCELLED",cancelled.getStatus());assertEquals(10,cancelled.getAdminPenaltyAmount());
        assertThrows(OrderProblem.class,()->service.applyPenalty(original.getId(),input(cancelled,"terminal",PenaltySystemPolicy.WRONG_LOCATION_DELIVERY)));
    }
    @Test void sampleRejectsAutomaticPoliciesMissingReasonAndStaleOrWrongCourier(){
        AdminMockOrderResponse order=service.sampleOrder("demo-order-002");
        for(PenaltySystemPolicy p:new PenaltySystemPolicy[]{null,PenaltySystemPolicy.SUCCESSFUL_DELIVERY,PenaltySystemPolicy.OVERDUE_DELIVERY,PenaltySystemPolicy.COURIER_UNRESPONSIVE})assertThrows(OrderProblem.class,()->service.applyPenalty(order.getId(),input(order,"invalid",p)));
        assertThrows(OrderProblem.class,()->service.resolveOrder(order.getId(),order.getRequesterId(),order.getCourierId(),List.of(PenaltySystemPolicy.COURIER_UNRESPONSIVE),"Retired"));
        ApplyPenaltyRequest missing=input(order,"missing",PenaltySystemPolicy.WRONG_LOCATION_DELIVERY);missing.setReason(null);assertThrows(OrderProblem.class,()->service.applyPenalty(order.getId(),missing));
        ApplyPenaltyRequest wrong=input(order,"wrong",PenaltySystemPolicy.WRONG_LOCATION_DELIVERY);wrong.setCourierId("wrong");assertThrows(OrderProblem.class,()->service.applyPenalty(order.getId(),wrong));
        ApplyPenaltyRequest stale=input(order,"stale",PenaltySystemPolicy.WRONG_LOCATION_DELIVERY);stale.setExpectedVersion(99L);assertThrows(OrderProblem.class,()->service.applyPenalty(order.getId(),stale));
        assertThrows(OrderProblem.class,()->service.applyPenalty("missing",input(order,"notfound",PenaltySystemPolicy.WRONG_LOCATION_DELIVERY)));
    }
}
