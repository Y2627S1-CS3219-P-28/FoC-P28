package sg.edu.nus.foc.order.domain;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.time.Instant;
import java.util.Optional;

public interface ErrandRepository extends JpaRepository<Errand, String> {
    Page<Errand> findByStatusAndExpiresAtAfter(
            Errand.Status status, Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Errand e where e.id = :id")
    Optional<Errand> lockById(String id);
}
