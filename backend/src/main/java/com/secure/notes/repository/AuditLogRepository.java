package com.secure.notes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secure.notes.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog , Long>{

	List<AuditLog> findByNoteId(Long id);

}
