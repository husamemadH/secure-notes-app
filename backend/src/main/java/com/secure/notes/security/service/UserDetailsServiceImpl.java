package com.secure.notes.security.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.secure.notes.model.ResetToken;
import com.secure.notes.model.User;
import com.secure.notes.repository.ResetTokenRepository;
import com.secure.notes.repository.UserRepository;
import com.secure.notes.util.EmailService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
	@Value("${frontend.url}")
	private String frontendUrl;
	
    private final UserRepository userRepository;
    
    private final ResetTokenRepository resetTokenRepository;
    
    private final EmailService emailService;
    
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        return UserDetailsImpl.build(user);
    }
    
    public User findByUsername(String username) {
        Optional<User> user = userRepository.findByUserName(username);
        return user.orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
    }
    
    public void generateResetToken(String email) {
    	
    	User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user not found"));
    	
    	String token = UUID.randomUUID().toString();
    	
    	Instant expiryDate = Instant.now().plus(24, ChronoUnit.HOURS);
    	
    	ResetToken resetToken = new ResetToken(token , expiryDate , user);
    	
    	resetTokenRepository.save(resetToken);
    	
    	String resetUrl = frontendUrl + "/reset-password?token=" + token;
    	// send email to user
    	
    	emailService.sendPasswordResetEmail(email, resetUrl);
    }

	public void resetPassword(String token, String newPassword) {

		ResetToken resetToken = resetTokenRepository.findByToken(token).orElseThrow(() -> new RuntimeException("Token doesnt exist"));

		if(resetToken.isUsed() || resetToken.getExpiryDate().isBefore(Instant.now())) {
			throw new RuntimeException("token already used or expired");
		}

		User user = resetToken.getUser();
		user.setPassword(passwordEncoder.encode(newPassword));
		resetToken.setUsed(true);
		userRepository.save(user);
		resetTokenRepository.save(resetToken);

	}

	public void generateVerificationToken(String email) {

		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

		String token = UUID.randomUUID().toString();

		Instant expiryDate = Instant.now().plus(24, ChronoUnit.HOURS);

		ResetToken verificationToken = new ResetToken(token, expiryDate, user);
		resetTokenRepository.save(verificationToken);

		String verifyUrl = frontendUrl + "/verify-email?token=" + token;
		emailService.sendVerificationEmail(email, verifyUrl);
	}

	public void verifyEmail(String token) {

		ResetToken verificationToken = resetTokenRepository.findByToken(token)
				.orElseThrow(() -> new RuntimeException("Token doesn't exist"));

		if (verificationToken.isUsed() || verificationToken.getExpiryDate().isBefore(Instant.now())) {
			throw new RuntimeException("Token already used or expired");
		}

		User user = verificationToken.getUser();
		user.setEnabled(true);
		verificationToken.setUsed(true);
		userRepository.save(user);
		resetTokenRepository.save(verificationToken);
	}

}
