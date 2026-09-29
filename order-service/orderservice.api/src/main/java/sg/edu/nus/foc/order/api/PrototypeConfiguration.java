package sg.edu.nus.foc.order.api;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration(proxyBeanMethods=false) @Profile("!prod & (local | test)")
public class PrototypeConfiguration {
    @Bean Clock orderClock() { return Clock.systemUTC(); }
}
