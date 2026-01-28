package com.example.task.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.task.entity.Paste;

@Repository
public interface PasteRepository extends JpaRepository<Paste, String> {
}

