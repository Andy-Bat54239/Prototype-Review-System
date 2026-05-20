package com.auca.prbs.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Temporary stub POJO. Delete when P1 delivers the JPA entity of the same name
 * (see Handoff Checklist in task_list.md).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpToken {
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private boolean used;
    /** Failed verification attempts. Token is locked once this reaches OtpService.MAX_ATTEMPTS. */
    private int attempts;
}
