package com.secure.notes.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.secure.notes.model.AuditLog;
import com.secure.notes.model.Note;
import com.secure.notes.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;
	
	
	public void logNoteCreation(String username , Note note) {
	 AuditLog noteLog = new AuditLog();
	 
	 		 noteLog.setAction("CREATE");
			 noteLog.setNoteId(note.getId());
			 noteLog.setNoteContent(note.getContent());
			 noteLog.setOwnerUsername(username);
			 noteLog.setTimestamp(LocalDateTime.now());
			 auditLogRepository.save(noteLog);
			 }
	
	public void logNoteUpdate(String username , Note note) {
		 AuditLog noteLog = new AuditLog();
		 
 		 noteLog.setAction("UPDATE");
		 noteLog.setNoteId(note.getId());
		 noteLog.setNoteContent(note.getContent());
		 noteLog.setOwnerUsername(username);
		 noteLog.setTimestamp(LocalDateTime.now());
		 auditLogRepository.save(noteLog);
	}

	public void logNoteDeletion(String username , Note note) {
		 AuditLog noteLog = new AuditLog();
		 
 		 noteLog.setAction("DELETE");
		 noteLog.setNoteId(note.getId());
		 noteLog.setNoteContent(note.getContent());
		 noteLog.setOwnerUsername(username);
		 noteLog.setTimestamp(LocalDateTime.now());
		 auditLogRepository.save(noteLog);
	}

	public List<AuditLog> getAuditLogsForNoteId(Long id) {
		 
		return auditLogRepository.findByNoteId(id);
	}

	public List<AuditLog> getAllAuditLogs() {


		return auditLogRepository.findAll();
	}

	 
	
}
