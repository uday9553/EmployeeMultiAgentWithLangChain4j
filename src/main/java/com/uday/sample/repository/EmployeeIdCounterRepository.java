package com.uday.sample.repository;

import com.uday.sample.model.EmployeeIdCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface EmployeeIdCounterRepository extends JpaRepository<EmployeeIdCounter, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select counter from EmployeeIdCounter counter where counter.id = 1")
    Optional<EmployeeIdCounter> lockCounter();
}
