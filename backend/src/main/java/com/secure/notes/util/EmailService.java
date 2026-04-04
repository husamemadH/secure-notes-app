package com.secure.notes.util;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;
	
	
	public void sendPasswordResetEmail(String to , String resetUrl ) {
		
		SimpleMailMessage message = new SimpleMailMessage();
		
		message.setTo(to);
		message.setSubject("Secure Notes Password Reset Request");
		message.setText("Click the link to reset your password: " + resetUrl);
		mailSender.send(message);
		
		
	}
}
