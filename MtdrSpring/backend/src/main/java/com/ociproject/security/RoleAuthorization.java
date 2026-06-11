package com.ociproject.security;

import com.ociproject.exception.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public final class RoleAuthorization {

    private static final String FORBIDDEN_PROJECT_MSG =
            "No tienes permiso para crear o editar proyectos.";
    private static final String FORBIDDEN_SPRINT_MSG =
            "No tienes permiso para crear o editar sprints.";

    private RoleAuthorization() {
    }

    public static String roleFromContext() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return "USER";
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(Objects::nonNull)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("USER");
    }

    public static boolean canManageProjects() {
        return !"DEVELOPER".equals(roleFromContext());
    }

    public static void requireCanManageProjects() {
        if (!canManageProjects()) {
            throw new ForbiddenException(FORBIDDEN_PROJECT_MSG);
        }
    }

    public static boolean canManageSprints() {
        return !"DEVELOPER".equals(roleFromContext());
    }

    public static void requireCanManageSprints() {
        if (!canManageSprints()) {
            throw new ForbiddenException(FORBIDDEN_SPRINT_MSG);
        }
    }
}
