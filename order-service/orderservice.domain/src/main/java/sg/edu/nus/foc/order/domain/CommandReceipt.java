package sg.edu.nus.foc.order.domain;
import jakarta.persistence.*;
@Entity @Table(name="order_commands")
public class CommandReceipt {
    @Id @Column(length=36) private String id;
    @Column(nullable=false, length=64) private String fingerprint;
    protected CommandReceipt() {}
    public CommandReceipt(String id, String fingerprint) { this.id=id; this.fingerprint=fingerprint; }
    public void verify(String expected) { if (!fingerprint.equals(expected)) throw OrderProblem.conflict("Command ID was already used with different input."); }
}
