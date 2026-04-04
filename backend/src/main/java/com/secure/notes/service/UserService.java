package com.secure.notes.service;

 
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.secure.notes.dto.UserDTO;
import com.secure.notes.model.AppRole;
import com.secure.notes.model.Role;
import com.secure.notes.model.User;
import com.secure.notes.repository.RoleRepository;
import com.secure.notes.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    
      
    private final UserRepository userRepository;
    
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;
    
    private final TotpService totpService;
    
    public void updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        AppRole appRole = AppRole.valueOf(roleName);
        Role role = roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);
        userRepository.save(user);
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    public UserDTO getUserById(Long id) {
//        return userRepository.findById(id).orElseThrow();
        User user = userRepository.findById(id).orElseThrow();
        return convertToDto(user);
    }

    private UserDTO convertToDto(User user) {
        return new UserDTO(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.isAccountNonLocked(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isEnabled(),
                user.getCredentialsExpiryDate(),
                user.getAccountExpiryDate(),
                user.getTwoFactorSecret(),
                user.isTwoFactorEnabled(),
                user.getSignUpMethod(),
                user.getRole(),
                user.getCreatedDate(),
                user.getUpdatedDate()
        );
    }

    public void updatePassword(Long userId, String password) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update password");
        }
    }


    public void updateAccountLockStatus(Long userId, boolean lock) {
        User user = userRepository.findById(userId).orElseThrow(()
                -> new RuntimeException("User not found"));
        user.setAccountNonLocked(!lock);
        userRepository.save(user);
    }

    public void updateAccountExpiryStatus(Long userId, boolean expire) {
        User user = userRepository.findById(userId).orElseThrow(()
                -> new RuntimeException("User not found"));
        user.setAccountNonExpired(!expire);
        userRepository.save(user);
    }

    public void updateAccountEnabledStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId).orElseThrow(()
                -> new RuntimeException("User not found"));
        user.setEnabled(!enabled);
        userRepository.save(user);
    }

    public void updateCredentialsExpiryStatus(Long userId, boolean expire) {
        User user = userRepository.findById(userId).orElseThrow(()
                -> new RuntimeException("User not found"));
        user.setCredentialsNonExpired(!expire);
        userRepository.save(user);
    }


	public Optional<User> findByEmail(String email) {
		 
		return userRepository.findByEmail(email);
		
	}
	
	public User registerUser(User user) {
		
		if(user.getPassword() != null) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));;
		}
		
		return userRepository.save(user);
	}
    
	public GoogleAuthenticatorKey generate2FASecret(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		
		GoogleAuthenticatorKey key = totpService.generateSecret();
		user.setTwoFactorSecret(key.getKey());
		
		userRepository.save(user);
		return key;	
	}
	public boolean validate2FA(Long userId , int code) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		return totpService.verifyCode(user.getTwoFactorSecret() , code);
	}
	public void disable2FA(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		user.setTwoFactorEnabled(false);
		userRepository.save(user);

	}
	
	public void enable2FA(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		user.setTwoFactorEnabled(true);
		userRepository.save(user);


	}


	public Optional<User> findByUsername(String username) {
		 
		return userRepository.findByUserName(username);
	}
}
