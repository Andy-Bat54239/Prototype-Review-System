package com.auca.prbs.service;

import com.auca.prbs.dto.ImportUserError;
import com.auca.prbs.dto.ImportUsersResponse;
import com.auca.prbs.entity.User;
import com.auca.prbs.entity.UserRole;
import com.auca.prbs.entity.UserStatus;
import com.auca.prbs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Bulk admin onboarding from a CSV provided by the university registrar.
 *
 * <p>Expected format (header row required, order doesn't matter, case-insensitive):
 * <pre>
 *   name,email,role,status
 *   Andy Biyonga,andy@university.ac.rw,STUDENT,ACTIVE
 *   Dr. Mensah,sarah@university.ac.rw,SUPERVISOR
 * </pre>
 * - {@code status} is optional; defaults to {@code ACTIVE}.
 * - Rows starting with {@code #} are treated as comments.
 * - Blank lines are skipped.
 * - Users whose email already exists are skipped (no overwrite). To update an
 *   existing user, use {@code PATCH /api/v1/users/{id}/status} — full update
 *   would be a separate endpoint.
 *
 * <p>The parser handles a useful subset of CSV: quoted fields with embedded
 * commas ({@code "Doe, John"}) and escaped double-quotes ({@code ""}). It does
 * not handle multi-line quoted fields.
 */
@Service
@RequiredArgsConstructor
public class UserImportService {

    private static final Logger log = LoggerFactory.getLogger(UserImportService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;

    @Transactional
    public ImportUsersResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ImportUsersResponse(0, 0,
                    List.of(new ImportUserError(0, null, "File is empty")));
        }

        List<String[]> rows = parseCsv(file);
        if (rows.isEmpty()) {
            return new ImportUsersResponse(0, 0,
                    List.of(new ImportUserError(0, null, "No header row found")));
        }

        Map<String, Integer> headerIndex = mapHeaders(rows.get(0));
        List<String> missing = requireColumns(headerIndex, "name", "email", "role");
        if (!missing.isEmpty()) {
            return new ImportUsersResponse(0, 0,
                    List.of(new ImportUserError(0, null, "Missing required columns: " + missing)));
        }

        int imported = 0;
        int skipped  = 0;
        List<ImportUserError> errors = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] cells = rows.get(i);
            int csvRow = i; // 1-based, excluding the header
            String email = pick(cells, headerIndex, "email");

            try {
                String name = required(pick(cells, headerIndex, "name"), "name");
                String emailVal = required(email, "email");
                if (!EMAIL.matcher(emailVal).matches()) {
                    throw new IllegalArgumentException("Invalid email format");
                }
                UserRole role = parseEnum(UserRole.class, required(pick(cells, headerIndex, "role"), "role"));
                UserStatus status = headerIndex.containsKey("status") && pick(cells, headerIndex, "status") != null && !pick(cells, headerIndex, "status").isBlank()
                        ? parseEnum(UserStatus.class, pick(cells, headerIndex, "status"))
                        : UserStatus.ACTIVE;

                if (userRepository.existsByEmail(emailVal)) {
                    skipped++;
                    continue;
                }

                userRepository.save(User.builder()
                        .name(name)
                        .email(emailVal)
                        .role(role)
                        .status(status)
                        .build());
                imported++;
            } catch (IllegalArgumentException e) {
                errors.add(new ImportUserError(csvRow, email, e.getMessage()));
            } catch (RuntimeException e) {
                log.warn("Unexpected error importing row {} ({})", csvRow, email, e);
                errors.add(new ImportUserError(csvRow, email, "Unexpected error: " + e.getClass().getSimpleName()));
            }
        }

        log.info("CSV import: imported={}, skipped={}, errors={}", imported, skipped, errors.size());
        return new ImportUsersResponse(imported, skipped, errors);
    }

    // ── CSV parsing ──────────────────────────────────────────────────────────

    private static List<String[]> parseCsv(MultipartFile file) {
        List<String[]> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                out.add(splitCsvLine(line));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read CSV", e);
        }
        return out;
    }

    /**
     * Splits one CSV line. Handles quoted fields with embedded commas and the
     * RFC 4180 escape for double-quote ({@code ""}).
     */
    static String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == ',') {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else if (c == '"' && cur.length() == 0) {
                inQuotes = true;
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private static Map<String, Integer> mapHeaders(String[] headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.length; i++) {
            map.put(headerRow[i].toLowerCase(Locale.ROOT), i);
        }
        return map;
    }

    private static List<String> requireColumns(Map<String, Integer> headers, String... names) {
        List<String> missing = new ArrayList<>();
        for (String n : names) if (!headers.containsKey(n)) missing.add(n);
        return missing;
    }

    private static String pick(String[] cells, Map<String, Integer> headers, String name) {
        Integer idx = headers.get(name);
        if (idx == null || idx >= cells.length) return null;
        return cells[idx];
    }

    private static String required(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return v.trim();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid " + type.getSimpleName() + ": " + raw);
        }
    }
}
