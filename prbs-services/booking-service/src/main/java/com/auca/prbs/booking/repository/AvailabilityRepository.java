package com.auca.prbs.booking.repository;

import com.auca.prbs.booking.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findBySupervisorIdAndDateGreaterThanEqualOrderByDateAsc(Long supervisorId, LocalDate from);
    List<Availability> findByDateGreaterThanEqualOrderByDateAsc(LocalDate from);
}
