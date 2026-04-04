package com.secure.notes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secure.notes.model.Note;

@Repository
public interface NoteRepository extends JpaRepository<Note , Long> {
	List<Note> findByOwnerUsername(String username);
}
