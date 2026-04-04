package com.secure.notes.security;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.secure.notes.model.AppRole;
import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.security.jwt.JwtUtils;
import com.secure.notes.security.service.UserDetailsImpl;
import com.secure.notes.service.UserService;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final RoleRepository roleRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException, java.io.IOException {
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        // 1. Extract info into LOCAL variables (Thread-safe)
        String email = attributes.getOrDefault("email", "").toString();
        String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
        
        final String finalUsername; 
        final String idAttributeKey;

        if ("github".equals(registrationId)) {
            finalUsername = attributes.getOrDefault("login", "").toString();
            idAttributeKey = "id";
        } else if ("google".equals(registrationId)) {
            finalUsername = email.split("@")[0];
            idAttributeKey = "sub";
        } else {
            finalUsername = email; // Fallback to email if login/sub is missing
            idAttributeKey = "id";
        }

        // 2. Sync with Database
        userService.findByEmail(email).ifPresentOrElse(user -> {
            updateSecurityContext(attributes, user.getRole().getRoleName().name(), idAttributeKey, registrationId);
        }, () -> {
            User newUser = new User();
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            
            newUser.setRole(userRole);
            newUser.setEmail(email);
            newUser.setUserName(finalUsername);
            newUser.setSignUpMethod(registrationId);
            userService.registerUser(newUser);
            
            updateSecurityContext(attributes, userRole.getRoleName().name(), idAttributeKey, registrationId);
        });

        // 3. JWT TOKEN LOGIC (Using local finalUsername to avoid NPE)
        UserDetailsImpl userDetails = new UserDetailsImpl(
                null,
                finalUsername,
                email,
                null,
                false,
                principal.getAuthorities().stream()
                        .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                        .collect(Collectors.toList())
        );

        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

        // 4. Redirect
        this.setAlwaysUseDefaultTargetUrl(true);
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", jwtToken)
                .build().toUriString();
        this.setDefaultTargetUrl(targetUrl);
        
        super.onAuthenticationSuccess(request, response, authentication);
    }

    // Helper to keep code clean
    private void updateSecurityContext(Map<String, Object> attributes, String roleName, String idKey, String regId) {
        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(roleName)),
                attributes,
                idKey
        );
        Authentication securityAuth = new OAuth2AuthenticationToken(
                oauthUser,
                List.of(new SimpleGrantedAuthority(roleName)),
                regId
        );
        SecurityContextHolder.getContext().setAuthentication(securityAuth);
    }
}