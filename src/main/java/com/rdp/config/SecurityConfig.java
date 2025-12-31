package com.rdp.config;

import com.rdp.security.JwtAuthFilter;
import com.rdp.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
public class SecurityConfig {
	@Autowired private JwtAuthFilter jwtAuthFilter;

	@Bean
	public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

	@Bean
	public UserDetailsService userDetailsService(CustomUserDetailsService customService) { return customService; }

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(req -> {
					var c = new CorsConfiguration();
					c.setAllowedOrigins(List.of("http://localhost:3000", "http://192.168.1.103:3000"));
					c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
					c.setAllowedHeaders(List.of("Authorization","Content-Type"));
					return c;
				}))
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/auth/login").permitAll()

                        // categories
                        .requestMatchers(HttpMethod.GET, "/api/categories/**")
                        .hasAnyRole("ADMIN","PHARMACIST","MANAGER")
                        .requestMatchers("/api/categories/**")
                        .hasRole("ADMIN") // POST/PUT/DELETE

						// Suppliers
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/suppliers/**")
						.hasAnyRole("ADMIN","PHARMACIST","MANAGER")
						.requestMatchers("/api/suppliers/**")
						.hasRole("ADMIN") // POST/PUT/DELETE

                        // Products
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**")
                        .hasAnyRole("ADMIN","PHARMACIST","MANAGER")
                        .requestMatchers("/api/products/**")
                        .hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/products/bulk").hasRole("ADMIN")// POST/PUT/DELETE

						// Customers
						.requestMatchers(HttpMethod.GET, "/api/customers/**")
						.hasAnyRole("ADMIN","PHARMACIST","MANAGER")
						.requestMatchers("/api/customers/**")
						.hasRole("ADMIN") // POST/PUT/DELETE

						// purchase order
						.requestMatchers(HttpMethod.GET,   "/api/purchase-orders/**").hasAnyRole("ADMIN","MANAGER","PHARMACIST")
						.requestMatchers(HttpMethod.POST,  "/api/purchase-orders/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE,"/api/purchase-orders/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PATCH,"/api/purchase-orders/**").hasRole("ADMIN")


						// User self-profile update (allow all authenticated users)
						.requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()

						// other domains
						.requestMatchers("/api/grn/approve").hasRole("ADMIN")
						.requestMatchers("/api/reports/**").hasAnyRole("ADMIN","MANAGER")
						.requestMatchers("/api/sales/**").hasAnyRole("ADMIN","MANAGER","PHARMACIST")
						.anyRequest().authenticated()
                )
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable());

		http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
