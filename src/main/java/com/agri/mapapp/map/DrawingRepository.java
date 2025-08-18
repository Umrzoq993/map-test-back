package com.agri.mapapp.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrawingRepository extends JpaRepository<Drawing, Long> {
    Optional<Drawing> findTopByOrderByCreatedAtDesc();
}
