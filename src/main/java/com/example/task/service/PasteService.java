package com.example.task.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.task.entity.Paste;
import com.example.task.repository.PasteRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasteService {

    private final PasteRepository pasteRepository;

    // CREATE PASTE
    public Paste createPaste(String content, Integer ttlSeconds, Integer maxViews) {

        Paste paste = Paste.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .createdAt(Instant.now())
                .viewCount(0)
                .maxViews(maxViews)
                .build();

        if (ttlSeconds != null) {
            paste.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        }

        return pasteRepository.save(paste);
    }

    // FETCH PASTE (counts as a view)
    public Optional<Paste> fetchPaste(String id, Instant now) {

        Optional<Paste> optionalPaste = pasteRepository.findById(id);

        if (optionalPaste.isEmpty()) {
            return Optional.empty();
        }

        Paste paste = optionalPaste.get();

        // Expiry check
        if (paste.getExpiresAt() != null && now.isAfter(paste.getExpiresAt())) {
            return Optional.empty();
        }

        // Max views check
        if (paste.getMaxViews() != null &&
                paste.getViewCount() >= paste.getMaxViews()) {
            return Optional.empty();
        }

        // Increment view count
        paste.setViewCount(paste.getViewCount() + 1);
        pasteRepository.save(paste);

        return Optional.of(paste);
    }
}
