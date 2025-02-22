package com.gamerecs.back.repository;

import com.gamerecs.back.model.Developer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Long> {
    Optional<Developer> findByIgdbCompanyId(Long igdbCompanyId);
    boolean existsByIgdbCompanyId(Long igdbCompanyId);
} 
