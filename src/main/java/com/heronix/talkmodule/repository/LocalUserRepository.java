package com.heronix.talkmodule.repository;

import com.heronix.talkmodule.model.domain.LocalUser;
import com.heronix.talkmodule.model.enums.SyncStatus;
import com.heronix.talkmodule.model.enums.UserRole;
import com.heronix.talkmodule.model.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalUserRepository extends JpaRepository<LocalUser, Long> {

    Optional<LocalUser> findByUsername(String username);

    Optional<LocalUser> findByEmployeeId(String employeeId);

    Optional<LocalUser> findByEmail(String email);

    List<LocalUser> findByActiveTrue();

    List<LocalUser> findByActiveTrueOrderByLastNameAsc();

    List<LocalUser> findByRole(UserRole role);

    List<LocalUser> findByDepartment(String department);

    List<LocalUser> findByStatus(UserStatus status);

    @Query("SELECT u FROM LocalUser u WHERE u.status != 'OFFLINE' AND u.active = true")
    List<LocalUser> findOnlineUsers();

    List<LocalUser> findBySyncStatus(SyncStatus status);

    @Query("SELECT u FROM LocalUser u WHERE u.active = true AND " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<LocalUser> searchByName(@Param("term") String term);

    boolean existsByUsername(String username);

    long countByActiveTrue();

    @Query("SELECT COUNT(u) FROM LocalUser u WHERE u.status != 'OFFLINE' AND u.active = true")
    long countOnlineUsers();
}
