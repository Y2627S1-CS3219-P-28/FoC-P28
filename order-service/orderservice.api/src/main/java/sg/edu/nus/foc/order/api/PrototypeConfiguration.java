package sg.edu.nus.foc.order.api;

import org.springframework.context.annotation.*;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@Profile("!prod & (local | test)")
public class PrototypeConfiguration {
    @Bean
    Clock orderClock() {
        return Clock.systemUTC();
    }
}
