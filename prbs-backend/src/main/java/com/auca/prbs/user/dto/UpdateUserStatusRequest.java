package com.auca.prbs.user.dto;

import com.auca.prbs.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {}
