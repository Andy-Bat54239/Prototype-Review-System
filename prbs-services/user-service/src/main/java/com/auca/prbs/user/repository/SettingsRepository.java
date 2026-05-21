package com.auca.prbs.user.repository;

import com.auca.prbs.user.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, Long> {
}
