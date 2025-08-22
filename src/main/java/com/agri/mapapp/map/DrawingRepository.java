package com.agri.mapapp.map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrawingRepository extends JpaRepository<Drawing, Long> {
    Optional<Drawing> findTopByOrderByCreatedAtDesc();
    // Yangi: nom bo‘yicha qidirish + sahifalash
    Page<Drawing> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
