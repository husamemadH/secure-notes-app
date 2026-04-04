package com.secure.notes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secure.notes.model.User;

public interface UserRepository extends JpaRepository<User , Long> {
	
	Optional<User> findByUserName(String username);
	boolean existsByUserName(String username);
	boolean existsByEmail(String email);
	Optional<User> findByEmail(String email);

}
