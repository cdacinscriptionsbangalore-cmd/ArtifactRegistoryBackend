package com.cadac.stone_inscription.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.cadac.stone_inscription.auth.CustomOAuth2SuccessHandler;
import com.cadac.stone_inscription.auth.JwtAuthenticationEntryPoint;
import com.cadac.stone_inscription.auth.JwtRequestFilter;
import com.cadac.stone_inscription.exception.ExceptionHandlerFilter;

@Configuration
@EnableWebSecurity
@EnableMongoAuditing
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class StoneinscriptionConfiguration implements WebMvcConfigurer {

        @Autowired
        private JwtRequestFilter jwtRequestFilter;
        @Autowired
        private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        @Autowired
        private UserDetailsService StoneInscriptionUserDetailsService;

        @Autowired
        private ExceptionHandlerFilter exceptionHandlerFilter;

        @Value("${app.cors.url}")
        private  String corsUrl;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }


        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                                .allowedOrigins(corsUrl)
                                .allowedMethods("GET", "POST", "PUT", "DELETE")
                                .allowedHeaders("*")
                                .exposedHeaders("*");
        }
        @Bean
        public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder authenticationManagerBuilder = http
                                .getSharedObject(AuthenticationManagerBuilder.class);
                authenticationManagerBuilder.userDetailsService(StoneInscriptionUserDetailsService)
                                .passwordEncoder(passwordEncoder());
                return authenticationManagerBuilder.build();
        }
        
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                                        CustomOAuth2SuccessHandler successHandler) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/noauth/**", "/post/public/**").permitAll()
                .requestMatchers("/api/v1/**", "/post/**").authenticated()
                .requestMatchers("/oauth2/**", "/oauth2/login/**").permitAll()
                .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2.successHandler(successHandler))

                // 🧱 Security headers for XSS & content protection
                .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; script-src 'self' https://www.google.com https://www.gstatic.com; " +
                  "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                  "font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; " +
                  "object-src 'none'; base-uri 'self'; frame-ancestors 'none';")

                )
                .frameOptions(frame -> frame.deny()) // Prevent clickjacking
                .contentTypeOptions(Customizer.withDefaults()) // Prevent MIME sniffing
                // .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000)
                )
                );

        return http.build();
        }
}
