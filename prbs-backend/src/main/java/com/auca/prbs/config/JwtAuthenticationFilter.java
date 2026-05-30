package com.auca.prbs.config;

import com.auca.prbs.auth.service.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads {@code Authorization: Bearer <jwt>}, validates via {@link JwtTokenProvider},
 * populates the SecurityContext. Every service includes this filter so it can
 * trust the gateway's incoming tokens without re-calling auth-service.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader(HEADER);
        if (header == null) {
            // No Authorization header — public endpoint or anonymous request. Move on.
            chain.doFilter(req, res);
            return;
        }
        if (!header.startsWith(PREFIX)) {
            // Wrong header shape — typically "Bearer Bearer <token>" from a client
            // that prepended the prefix manually when the OpenAPI scheme already adds it.
            log.warn("Rejecting Authorization header on {} — does not start with single 'Bearer ' prefix. " +
                     "If using Swagger UI, paste JUST the token (no 'Bearer ' prefix).",
                     req.getRequestURI());
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(PREFIX.length()).trim();
        // Catch the most common Swagger mistake: pasting "Bearer <token>" into
        // the Authorize dialog, which then double-prefixes the header.
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            log.warn("Rejecting Authorization header on {} — double 'Bearer ' prefix. " +
                     "In Swagger UI's Authorize dialog, paste ONLY the JWT (no 'Bearer ' prefix).",
                     req.getRequestURI());
            chain.doFilter(req, res);
            return;
        }
        try {
            Long userId = jwtTokenProvider.getUserId(token);
            String role = jwtTokenProvider.getRole(token);
            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
        } catch (ExpiredJwtException e) {
            log.warn("Rejecting expired JWT on {} — token expired at {}. Get a fresh one via /api/v1/auth/refresh.",
                     req.getRequestURI(), e.getClaims().getExpiration());
        } catch (SignatureException e) {
            log.warn("Rejecting JWT on {} — signature mismatch. Token signed with a different JWT_SECRET than this server uses. " +
                     "Common cause: container restart with a different JWT_SECRET, or token from a different environment.",
                     req.getRequestURI());
        } catch (MalformedJwtException e) {
            log.warn("Rejecting malformed JWT on {} — value is not a valid JWT. Check you copied the full accessToken (3 dot-separated parts).",
                     req.getRequestURI());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejecting JWT on {} — {}: {}", req.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
        }
        chain.doFilter(req, res);
    }
}
