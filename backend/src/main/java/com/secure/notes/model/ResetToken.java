package com.secure.notes.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class ResetToken {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant expiryDate;
	
	@Column(nullable = false)
	private String token;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id" , nullable = false)
	private User user;
	
	private boolean used;

	public ResetToken(String token, Instant expiryDate, User user) {
		 this.user = user;
		 this.token = token;
		 this.expiryDate = expiryDate;
	}
	
	
	
}
