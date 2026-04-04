package com.secure.notes.security;

import static org.springframework.security.config.Customizer.withDefaults;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.secure.notes.model.AppRole;
import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.repository.UserRepository;
import com.secure.notes.security.jwt.AuthEntryPointJwt;
import com.secure.notes.security.jwt.AuthTokenFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true , securedEnabled = true , jsr250Enabled = true  )
@RequiredArgsConstructor
public class SecurityConfig {
	 
	 private final AuthEntryPointJwt unauthorizedHandler;
	 private final  AuthTokenFilter  authTokenFilter;

	@Bean
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http , OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler ) throws Exception {
		http.csrf(csrf -> csrf.disable()); // Standard for JWT-based REST APIs		
		http.cors(Customizer.withDefaults());
		http.authorizeHttpRequests((requests) 
				-> requests.requestMatchers("/api/csrf-token").permitAll()
				.requestMatchers("/api/auth/public/**").permitAll()
				.requestMatchers("/api/oauth2/**").permitAll()
				.anyRequest().authenticated())
				.oauth2Login(oauth2 ->  {oauth2.successHandler(oAuth2LoginSuccessHandler);}); 
		
		http.httpBasic(withDefaults());
		http.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler));
	    http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

 		return http.build();
 	}
	
	 

	@Bean 
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
	@Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is the current industry standard
        return new BCryptPasswordEncoder();
    }
	
	@Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository , PasswordEncoder passwordEncoder) {
        return args -> {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_USER)));

            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_ADMIN)));

            if (!userRepository.existsByUserName("user1")) {
                User user1 = new User("user1", "user1@example.com", passwordEncoder.encode("password1"));
                user1.setAccountNonLocked(false);
                user1.setAccountNonExpired(true);
                user1.setCredentialsNonExpired(true);
                user1.setEnabled(true);
                user1.setCredentialsExpiryDate(LocalDate.now().plusYears(1));
                user1.setAccountExpiryDate(LocalDate.now().plusYears(1));
                user1.setTwoFactorEnabled(false);
                user1.setSignUpMethod("email");
                user1.setRole(userRole);
                userRepository.save(user1);
            }
            
            if (!userRepository.existsByUserName("admin")) {
                User admin = new User("admin", "admin@example.com", passwordEncoder.encode("password2"));
                admin.setAccountNonLocked(true);
                admin.setAccountNonExpired(true);
                admin.setCredentialsNonExpired(true);
                admin.setEnabled(true);
                admin.setCredentialsExpiryDate(LocalDate.now().plusYears(1));
                admin.setAccountExpiryDate(LocalDate.now().plusYears(1));
                admin.setTwoFactorEnabled(false);
                admin.setSignUpMethod("email");
                admin.setRole(adminRole);
                userRepository.save(admin);
            }
        };
    }
	}

 
