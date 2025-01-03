package com.gamerecs.back.repository;

import com.gamerecs.back.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for VerificationToken entity operations.
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Find a verification token by its token string.
     *
     * @param token the token string to search for
     * @return an Optional containing the verification token if found
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Delete all expired tokens for a given user ID.
     *
     * @param userId the user ID
     */
    void deleteByUser_UserId(Long userId);
} 
