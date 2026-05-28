package com.auca.prbs.user.dto;

import java.util.List;

public record ImportUsersResponse(int imported, int skipped, List<ImportUserError> errors) {}
