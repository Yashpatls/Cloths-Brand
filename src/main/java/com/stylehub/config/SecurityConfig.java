package com.stylehub.config;

import com.stylehub.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                response.sendRedirect("/admin");
            } else {
                response.sendRedirect("/");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomUserDetailsService userDetailsService) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                // Static resources & uploads
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico", "/error").permitAll()
                // Public customer-facing storefront pages
                .requestMatchers(
                    "/", "/shop", "/products", "/products/**", "/category/**",
                    "/combos", "/combos/**",
                    "/search", "/contact", "/register", "/login",
                    "/cart", "/cart/**", "/wishlist", "/wishlist/**"
                ).permitAll()
                // Admin dashboard & management
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Authenticated customer routes
                .requestMatchers(
                    "/checkout", "/checkout/**", "/orders", "/orders/**",
                    "/profile", "/profile/**", "/reviews/add"
                ).hasAnyRole("CUSTOMER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(customAuthenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?loggedout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("styleHubSecretRememberMeKey")
                .tokenValiditySeconds(86400 * 14) // 14 days
                .userDetailsService(userDetailsService)
            );

        return http.build();
    }
}
