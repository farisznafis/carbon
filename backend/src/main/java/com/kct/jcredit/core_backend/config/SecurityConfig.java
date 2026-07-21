package com.kct.jcredit.core_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.kct.jcredit.core_backend.security.CompanyAuthorizationFilter;
import com.kct.jcredit.core_backend.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CompanyAuthorizationFilter companyAuthorizationFilter;
    private final SecurityFlowProperties securityFlowProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        CompanyAuthorizationFilter companyAuthorizationFilter,
                        SecurityFlowProperties securityFlowProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.companyAuthorizationFilter = companyAuthorizationFilter;
        this.securityFlowProperties = securityFlowProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .anonymous(anonymous -> anonymous.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/auth/**",
                    "/company/signup",
                    "/company/signup/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(companyAuthorizationFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(securityFlowProperties.getBcryptStrength());
    }
}
