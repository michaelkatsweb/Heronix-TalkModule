package com.heronix.talkmodule.repository;

import com.heronix.talkmodule.model.domain.CurrentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrentSessionRepository extends JpaRepository<CurrentSession, Long> {

    @Query("SELECT s FROM CurrentSession s ORDER BY s.id DESC")
    Optional<CurrentSession> findLatestSession();

    Optional<CurrentSession> findBySessionToken(String token);

    @Modifying
    @Query("DELETE FROM CurrentSession s")
    void clearAllSessions();
}
