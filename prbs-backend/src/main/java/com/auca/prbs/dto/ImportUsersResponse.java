package com.auca.prbs.dto;

import java.util.List;

/**
 * Summary returned from {@code POST /api/v1/users/import}.
 * The response is always {@code 200 OK} — even if some rows failed — with the
 * per-row outcomes broken out so the admin UI can show a partial-success report.
 */
public record ImportUsersResponse(
        int imported,
        int skipped,           // existing email; row was a no-op
        List<ImportUserError> errors
) {}
