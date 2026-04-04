package com.secure.notes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secure.notes.model.ResetToken;

@Repository
public interface ResetTokenRepository extends JpaRepository<ResetToken, Long>{

	Optional<ResetToken> findByToken(String token);
}
