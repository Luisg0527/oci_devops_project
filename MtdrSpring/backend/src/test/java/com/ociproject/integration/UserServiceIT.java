package com.ociproject.integration;

import com.ociproject.model.Role;
import com.ociproject.model.User;
import com.ociproject.service.RoleService;
import com.ociproject.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests against a real Oracle DB.
 *
 * Prerequisites:
 *  - Oracle instance running at the URL in application-it.properties
 *  - Schema applied via DB/schema.sql
 *  - Credentials in src/test/resources/application-it.properties
 *
 * Run with: mvn test -Dtest="*IT"
 *
 * Each test is wrapped in a transaction that rolls back automatically,
 * so no manual cleanup is needed between runs.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-it.properties")
@Transactional
class UserServiceIT {

    @Autowired UserService userService;
    @Autowired RoleService roleService;

    private Role developerRole() {
        return roleService.findByName("DEVELOPER")
                .orElseGet(() -> roleService.save(
                        Role.builder().roleName("DEVELOPER").build()));
    }

    @Test
    void saveAndFindUser_roundTrip() {
        Role role = developerRole();
        User user = User.builder()
                .fullName("Integration Test User")
                .email("it-test-" + System.currentTimeMillis() + "@example.com")
                .role(role)
                .build();

        User saved = userService.save(user);

        assertThat(saved.getUserId()).isNotNull();
        assertThat(userService.findById(saved.getUserId()))
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .asString()
                .startsWith("it-test-");
    }

    @Test
    void findByEmail_returnsCorrectUser() {
        Role role = developerRole();
        String email = "find-by-email-" + System.currentTimeMillis() + "@example.com";
        userService.save(User.builder().fullName("Email User").email(email).role(role).build());

        assertThat(userService.findByEmail(email))
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .isEqualTo(email);
    }

    @Test
    void softDelete_marksUserAsDeletedAndExcludesFromFindAll() {
        Role role = developerRole();
        User user = userService.save(User.builder()
                .fullName("To Be Deleted")
                .email("delete-it-" + System.currentTimeMillis() + "@example.com")
                .role(role)
                .build());

        userService.softDelete(user.getUserId());

        // findAll() filters deleted users
        assertThat(userService.findAll())
                .noneMatch(u -> u.getUserId().equals(user.getUserId()));

        // findById returns raw row — soft-delete fields should be set
        assertThat(userService.findById(user.getUserId()))
                .isPresent()
                .get()
                .satisfies(u -> {
                    assertThat(u.getDeleted()).isTrue();
                    assertThat(u.getDeletedAt()).isNotNull();
                    assertThat(u.getStatus()).isEqualTo(User.Status.INACTIVE);
                });
    }

    @Test
    void updateStatus_toLocked_persists() {
        Role role = developerRole();
        User user = userService.save(User.builder()
                .fullName("Status User")
                .email("status-it-" + System.currentTimeMillis() + "@example.com")
                .role(role)
                .build());

        userService.updateStatus(user.getUserId(), User.Status.LOCKED);

        assertThat(userService.findById(user.getUserId()))
                .isPresent()
                .get()
                .extracting(User::getStatus)
                .isEqualTo(User.Status.LOCKED);
    }

    @Test
    void updateStatus_notFound_throwsEntityNotFoundException() {
        assertThatThrownBy(() -> userService.updateStatus(Long.MAX_VALUE, User.Status.INACTIVE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(String.valueOf(Long.MAX_VALUE));
    }
}
