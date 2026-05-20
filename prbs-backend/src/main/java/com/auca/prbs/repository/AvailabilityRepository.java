package com.auca.prbs.repository;

import com.auca.prbs.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findBySupervisorIdAndDateGreaterThanEqualOrderByDateAsc(Long supervisorId, LocalDate from);
}
