package com.ociproject.security;

import com.ociproject.model.User;
import com.ociproject.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!tokenProvider.validateToken(token)) {
            log.warn("JWT validation failed for {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        if (tokenProvider.isRefreshToken(token)) {
            log.warn("Refresh token used as access token for {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = tokenProvider.getUserIdFromToken(token);
            User user = userService.findById(userId).orElse(null);
            if (user == null) {
                log.warn("JWT references unknown userId={}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            // Read role from token claims — avoids lazy-loading User.role outside a Hibernate session
            String roleName = tokenProvider.getRoleFromToken(token);
            if (roleName == null) roleName = "USER";

            var auth = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated userId={} role={} → {} {}", userId, roleName,
                    request.getMethod(), request.getRequestURI());
        } catch (Exception e) {
            log.error("Could not set authentication from JWT: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
