package com.gamerecs.back.repository;

import com.gamerecs.back.model.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    Optional<Publisher> findByIgdbCompanyId(Long igdbCompanyId);
    boolean existsByIgdbCompanyId(Long igdbCompanyId);
} 
