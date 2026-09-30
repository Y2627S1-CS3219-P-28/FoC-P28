package sg.edu.nus.foc.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="command_receipts", uniqueConstraints=@UniqueConstraint(name="ux_command_operation", columnNames={"operation","command_id"}))
public class CommandReceipt {
    @Id private String id = UUID.randomUUID().toString();
    @Column(nullable=false,length=80) private String operation;
    @Column(name="command_id",nullable=false,length=128) private String commandId;
    @Column(name="order_id",nullable=false,length=36) private String orderId;
    @Column(nullable=false) private Instant recordedAt;
    protected CommandReceipt() {}
    public CommandReceipt(String operation,String commandId,String orderId,Instant recordedAt){this.operation=operation;this.commandId=commandId;this.orderId=orderId;this.recordedAt=recordedAt;}
    public String getOrderId(){return orderId;}
}
