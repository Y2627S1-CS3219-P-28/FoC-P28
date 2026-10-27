
package sg.edu.nus.foc.user.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/api/users/docs", "/api/users/docs/**",
            "/api/users/v3/api-docs", "/api/users/v3/api-docs/**",
            "/api/users/swagger-ui/**",
            // Spring's error dispatch: lets unexpected failures surface as 500 instead of a misleading 401.
            "/error"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // bearer tokens, no cookies
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @org.springframework.beans.factory.annotation.Value(
                    "${firebase.auth.project-id:demo-foc}")
            String projectId,
            @org.springframework.beans.factory.annotation.Value(
                    "${firebase.auth.emulator-host:}")
            String emulatorHost) {

        if (!emulatorHost.isBlank()) {
            return new sg.edu.nus.foc.user.security.EmulatorJwtDecoder(
                    projectId);
        }

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withJwkSetUri(
                                sg.edu.nus.foc.user.security
                                        .FirebaseTokenValidators
                                        .GOOGLE_JWK_SET_URI)
                        .build();

        decoder.setJwtValidator(
                sg.edu.nus.foc.user.security.FirebaseTokenValidators
                        .forProject(projectId));

        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Application roles come from User Service, not JWT claims.
        converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of());

        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value(
                    "${p28.userservice.cors-origins:"
                    + "http://localhost:3000,http://localhost:8080}")
            List<String> allowedOrigins) {

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(
                List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "X-Request-Id"));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}

