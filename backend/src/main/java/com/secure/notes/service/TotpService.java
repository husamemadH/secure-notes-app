package com.secure.notes.service;

import org.springframework.stereotype.Service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

import lombok.RequiredArgsConstructor;

@Service
public class TotpService {

	private final GoogleAuthenticator gAuth;
	
	public TotpService() {
		
		this.gAuth = new GoogleAuthenticator();
	}
	
	public GoogleAuthenticatorKey generateSecret() {
		return gAuth.createCredentials();
	}
	
	public String getQrCodeUrl(GoogleAuthenticatorKey secret, String username) {
		
		return GoogleAuthenticatorQRGenerator.getOtpAuthURL("Secure Notes", username, secret);
	}
	
	public boolean verifyCode(String secret, int code) {
		return gAuth.authorize(secret, code);
	}
}
