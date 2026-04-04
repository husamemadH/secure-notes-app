package com.secure.notes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.secure.notes.model.Note;
import com.secure.notes.repository.NoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoteService {

	
	private final NoteRepository noteRepository;
	private final AuditLogService auditLogService;
	
	public Note createNoteForUser(String username , String content) {
		
		Note note = new Note();
		
		note.setContent(content);
		note.setOwnerUsername(username);
		
		Note savedNote = noteRepository.save(note);
		
		auditLogService.logNoteCreation(username, savedNote);
		return savedNote;
	}
	
	public Note updateNoteForUser(Long id , String content , String username) {
		
		
		Note note = noteRepository.findById(id)
		        .orElseThrow(() -> new RuntimeException("Note not found with id: " + id));
  
		note.setContent(content);
		note.setOwnerUsername(username);
		
		final Note UPDATED_NOTE = noteRepository.save(note);
		
		auditLogService.logNoteUpdate(username, UPDATED_NOTE);
		return UPDATED_NOTE;
	}
	
	 
	public void deleteNoteForUser(Long noteId , String username) {
		Note note = noteRepository.findById(noteId)
				.orElseThrow( () -> new RuntimeException("Note not found with id : " + noteId));
	    auditLogService.logNoteDeletion(username, note);	
		
		noteRepository.deleteById(noteId);
	}
	
	public List<Note> getNotesForUser(String username) {
		
		return noteRepository.findByOwnerUsername(username);
	}
}
