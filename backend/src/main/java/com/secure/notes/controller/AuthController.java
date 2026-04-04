package com.secure.notes.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.secure.notes.model.AppRole;
import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.repository.UserRepository;
import com.secure.notes.response.LoginResponse;
import com.secure.notes.response.MessageResponse;
import com.secure.notes.response.UserInfoResponse;
import com.secure.notes.security.jwt.JwtUtils;
import com.secure.notes.security.request.LoginRequest;
import com.secure.notes.security.request.SignupRequest;
import com.secure.notes.security.service.UserDetailsImpl;
import com.secure.notes.security.service.UserDetailsServiceImpl;
import com.secure.notes.service.TotpService;
import com.secure.notes.service.UserService;
import com.secure.notes.util.AuthUtil;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
	
	private final JwtUtils jwtUtils;
	
	private final AuthenticationManager authenticationManager;
	
	private final PasswordEncoder passwordEncoder;
	
	private final UserRepository userRepository;
	
	private final RoleRepository roleRepository;
	
	private final UserDetailsServiceImpl userDetailsService;
	
	private final TotpService totpService;
	
	private final UserService userService;
	
	private final AuthUtil authUtil;
	
	// AuthController.java
	@PostMapping("/public/signin")
	    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
	        Authentication authentication;
	        try {
	            authentication = authenticationManager
	                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
	        } catch (AuthenticationException exception) {
	            Map<String, Object> map = new HashMap<>();
	            map.put("message", "Bad credentials");
	            map.put("status", false);
	            return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);
	        }

//	      set the authentication
	        SecurityContextHolder.getContext().setAuthentication(authentication);

	        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

	        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

	        // Collect roles from the UserDetails
	        List<String> roles = userDetails.getAuthorities().stream()
	                .map(item -> item.getAuthority())
	                .collect(Collectors.toList());

	        // Prepare the response body, now including the JWT token directly in the body
	        LoginResponse response = new LoginResponse(userDetails.getUsername(), roles, jwtToken);

	        // Return the response entity with the JWT token included in the response body
	        return ResponseEntity.ok(response);
	    }
	
	@PostMapping("/public/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
	    if (userRepository.existsByUserName(signUpRequest.getUsername()) || userRepository.existsByEmail(signUpRequest.getEmail())) {
	        return ResponseEntity.badRequest().body(new MessageResponse("Error: Username or Email is already taken!"));
	    }
	    
	    try 
	    {
	    User user = new User(signUpRequest.getUsername(), 
	                         signUpRequest.getEmail(),
	                         passwordEncoder.encode(signUpRequest.getPassword()));

	    Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow(() -> new RuntimeException("Role not found"));
	    
	    user.setRole(userRole);
	    userRepository.save(user);

	    return ResponseEntity.ok(new MessageResponse("User registered successfully!")); }
	    catch (Exception e) {
	    	e.printStackTrace();
	    	return ResponseEntity.internalServerError().build();
	    }
	}
	
	@GetMapping("/user")
	public ResponseEntity<?> getUserDetails(Authentication authentication) {
		if (authentication == null || authentication.getPrincipal() == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Not authenticated"));
		}

		String username = extractUsername(authentication);

		if (username == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Could not determine username"));
		}

		User user = userDetailsService.findByUsername(username);

		// Extract roles safely depending on the principal type
		List<String> roles = authentication.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());

		UserInfoResponse response = new UserInfoResponse(
				user.getUserId(),
				user.getUserName(),
				user.getEmail(),
				user.isAccountNonLocked(),
				user.isAccountNonExpired(),
				user.isCredentialsNonExpired(),
				user.isEnabled(),
				user.getCredentialsExpiryDate(),
				user.getAccountExpiryDate(),
				user.isTwoFactorEnabled(),
				roles
		);

		return ResponseEntity.ok().body(response);
	}
	
	@GetMapping("/username")
	public ResponseEntity<?> getUsername(Authentication authentication) {
		if (authentication == null || authentication.getPrincipal() == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Not authenticated"));
		}
		
		String username = extractUsername(authentication);
		return ResponseEntity.ok(username);
	}

	// Helper method to safely extract the username regardless of login method
	private String extractUsername(Authentication authentication) {
		Object principal = authentication.getPrincipal();

		if (principal instanceof UserDetails) {
			return ((UserDetails) principal).getUsername();
		} else if (principal instanceof DefaultOAuth2User) {
			DefaultOAuth2User oauth2User = (DefaultOAuth2User) principal;
			// GitHub uses 'login', Google uses 'email'
			String username = oauth2User.getAttribute("login"); 
			if (username == null) {
				String email = oauth2User.getAttribute("email");
				username = email != null ? email.split("@")[0] : null;
			}
			return username;
		}
		return null;
	}
	
	@PostMapping("/public/forgot-password")
	public ResponseEntity<?> forgotPassword(@RequestParam String email) {
		
		try {
			userDetailsService.generateResetToken(email);
			
		}
		catch(Exception e) {
			return ResponseEntity.internalServerError().body(new MessageResponse("Error sending password reset email"));
		}
		return ResponseEntity.ok().body(new MessageResponse("Email for reseting password has been sent!"));
		
	}
	
	@PostMapping("/public/reset-password")
	public ResponseEntity<?> resetPassword(@RequestParam String token , @RequestParam String newPassword) {
		
		try {
			userDetailsService.resetPassword(token , newPassword);
			
		}
		catch(Exception e) {
			return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
		}
		
		return ResponseEntity.ok().body(new MessageResponse("Password Reset has been successful"));
	}
	
	//2FA 
	
	@PostMapping("/enable-2fa")
	public ResponseEntity<String> enable2FA() {
		Long userId = authUtil.loggedInUserId();
		
		GoogleAuthenticatorKey secret = userService.generate2FASecret(userId);
		
		String qrCodeUrl = totpService.getQrCodeUrl(secret, authUtil.loggedInUser().getUserName());
		
		return ResponseEntity.ok(qrCodeUrl);
		
	}
	
	@PostMapping("/disable-2fa")
	public ResponseEntity<String> disable2FA() {
		Long userId = authUtil.loggedInUserId();
		
		userService.disable2FA(userId);
		return ResponseEntity.ok("2FA disabled");
		
	}
	
	@PostMapping("/verify-2fa")
	public ResponseEntity<String> verify2FA(@RequestParam int code) {
		Long userId = authUtil.loggedInUserId();
		boolean isValid = userService.validate2FA(userId, code);
		if(isValid) {
			userService.enable2FA(userId);
			return ResponseEntity.ok("2FA Verified");
		}
		else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid 2FA Code");
		}
	}
	
	@GetMapping("/user/2fa-status")
	public ResponseEntity<?> get2FAStatus() {
		User user = authUtil.loggedInUser();
		if(user != null) {
			return ResponseEntity.ok().body(Map.of("is2faEnabled" , user.isTwoFactorEnabled()));
		}
		else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not Found");
			
		}
	}
	
	@PostMapping("/public/verify-2fa-login")
	public ResponseEntity<String> verify2FALogin(@RequestParam int code , @RequestParam String jwtToken) {
		
		String username = jwtUtils.getUserNameFromJwtToken(jwtToken);
		User user = userService.findByUsername(username).orElseThrow(() -> new RuntimeException("user not found"));
		
		boolean isValid = userService.validate2FA(user.getUserId(), code);
		if(isValid) {
			 
			return ResponseEntity.ok("2FA Verified");
		}
		else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid 2FA Code");
		}
	}
}
