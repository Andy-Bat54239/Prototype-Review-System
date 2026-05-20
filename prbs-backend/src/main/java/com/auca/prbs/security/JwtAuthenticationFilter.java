package com.auca.prbs.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                if (jwtTokenProvider.isValid(token)) {
                    Long userId = jwtTokenProvider.getUserId(token);
                    String role = jwtTokenProvider.getRole(token);

                    var authority = new SimpleGrantedAuthority("ROLE_" + role);
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));

                    // Spring Security 6: create a fresh context and set it, rather than
                    // mutating the existing one. Without this, downstream filters can see
                    // the user as anonymous and return 401 on role mismatch instead of 403.
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // Invalid token — leave SecurityContext empty; downstream returns 401.
            }
        }

        chain.doFilter(request, response);
    }
}
