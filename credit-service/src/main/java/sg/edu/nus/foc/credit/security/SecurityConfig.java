/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Code generation.
 * Scope: Generated the security implementation from the team-finalized Firebase authentication, authorization, and CORS design.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.security;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sg.edu.nus.foc.credit.config.CreditProperties;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    static final String[] PUBLIC_PATHS = {
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/api/credits/docs", "/api/credits/docs/**",
            "/api/credits/v3/api-docs", "/api/credits/v3/api-docs/**",
            "/api/credits/swagger-ui/**", "/error"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityHandlers handlers) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(CreditProperties properties) {
        CreditProperties.AuthSettings auth = properties.auth();
        if (auth.usesEmulator()) {
            log.warn("Accepting unsigned Firebase Auth emulator tokens for project '{}'.", auth.projectId());
            return new EmulatorJwtDecoder(auth.projectId());
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(FirebaseTokenValidators.GOOGLE_JWK_SET_URI).build();
        decoder.setJwtValidator(FirebaseTokenValidators.forProject(auth.projectId()));
        return decoder;
    }

    @Bean
    JsonSecurityHandlers jsonSecurityHandlers(JsonMapper mapper, Clock clock) {
        return new JsonSecurityHandlers(mapper, clock);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CreditProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.corsOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
