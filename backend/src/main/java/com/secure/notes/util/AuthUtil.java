package com.secure.notes.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.secure.notes.model.User;
import com.secure.notes.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthUtil {

	private final UserRepository userRepository;
	
	public Long loggedInUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		User user = userRepository.findByUserName(authentication.getName()).orElseThrow(() -> new RuntimeException("User Not Found"));
		
		return user.getUserId();
		
	}
	
	public User loggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		User user = userRepository.findByUserName(authentication.getName()).orElseThrow(() -> new RuntimeException("User Not Found"));
		
		return user;
		
	}
}
