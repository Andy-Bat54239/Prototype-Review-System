package com.auca.prbs.user.service;

import com.auca.prbs.user.dto.ImportUserError;
import com.auca.prbs.user.dto.ImportUsersResponse;
import com.auca.prbs.user.entity.User;
import com.auca.prbs.user.entity.UserRole;
import com.auca.prbs.user.entity.UserStatus;
import com.auca.prbs.user.repository.UserRepository;
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
 * Header columns: name, email, role (required); status optional, defaults ACTIVE.
 * Existing emails are skipped (no overwrite). Per-row errors collected; the
 * response is always 200 OK so the admin UI can show a partial-success report.
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

        int imported = 0, skipped = 0;
        List<ImportUserError> errors = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] cells = rows.get(i);
            int csvRow = i;
            String email = pick(cells, headerIndex, "email");
            try {
                String name = required(pick(cells, headerIndex, "name"), "name");
                String emailVal = required(email, "email");
                if (!EMAIL.matcher(emailVal).matches()) {
                    throw new IllegalArgumentException("Invalid email format");
                }
                UserRole role = parseEnum(UserRole.class, required(pick(cells, headerIndex, "role"), "role"));
                String statusRaw = pick(cells, headerIndex, "status");
                UserStatus status = (statusRaw != null && !statusRaw.isBlank())
                        ? parseEnum(UserStatus.class, statusRaw)
                        : UserStatus.ACTIVE;

                if (userRepository.existsByEmail(emailVal)) { skipped++; continue; }

                userRepository.save(User.builder()
                        .name(name).email(emailVal).role(role).status(status).build());
                imported++;
            } catch (IllegalArgumentException e) {
                errors.add(new ImportUserError(csvRow, email, e.getMessage()));
            } catch (RuntimeException e) {
                log.warn("Unexpected error importing row {} ({})", csvRow, email, e);
                errors.add(new ImportUserError(csvRow, email,
                        "Unexpected error: " + e.getClass().getSimpleName()));
            }
        }

        log.info("CSV import: imported={}, skipped={}, errors={}", imported, skipped, errors.size());
        return new ImportUsersResponse(imported, skipped, errors);
    }

    private static List<String[]> parseCsv(MultipartFile file) {
        List<String[]> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                out.add(splitCsvLine(line));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read CSV", e);
        }
        return out;
    }

    static String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(c);
            } else if (c == ',') { out.add(cur.toString().trim()); cur.setLength(0); }
            else if (c == '"' && cur.length() == 0) inQuotes = true;
            else cur.append(c);
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private static Map<String, Integer> mapHeaders(String[] headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.length; i++) map.put(headerRow[i].toLowerCase(Locale.ROOT), i);
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
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing required field: " + field);
        return v.trim();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        try { return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + type.getSimpleName() + ": " + raw);
        }
    }
}
