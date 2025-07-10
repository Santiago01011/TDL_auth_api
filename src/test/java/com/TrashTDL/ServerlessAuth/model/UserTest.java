package com.TrashTDL.ServerlessAuth.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the User entity and its implementation of UserDetails.
 */
@DisplayName("User Model & UserDetails Implementation Tests")
class UserTest {

    private User.UserBuilder createDefaultUserBuilder() {
        return User.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("hashed_password");
    }

    @Test
    @DisplayName("should return email for getUsername() as per UserDetails contract")
    void getUsername_shouldReturnEmail() {
        // Arrange
        String email = "user@example.com";
        User user = createDefaultUserBuilder().email(email).build();

        // Act & Assert
        assertEquals(email, user.getUsername(), "getUsername() must return the user's email.");
    }

    @Test
    @DisplayName("should return the password hash for getPassword()")
    void getPassword_shouldReturnPasswordHash() {
        // Arrange
        String passwordHash = "a_very_long_and_secure_hash";
        User user = createDefaultUserBuilder().password(passwordHash).build();

        // Act & Assert
        assertEquals(passwordHash, user.getPassword(), "getPassword() must return the password hash.");
    }

    @Test
    @DisplayName("isEnabled should return true for an active user (deletedAt is null)")
    void isEnabled_shouldReturnTrue_whenNotDeleted() {
        // Arrange
        User user = createDefaultUserBuilder().deletedAt(null).build();

        // Act & Assert
        assertTrue(user.isEnabled(), "An active user (deletedAt is null) should be enabled.");
    }

    @Test
    @DisplayName("isEnabled should return false for a deleted user (deletedAt is not null)")
    void isEnabled_shouldReturnFalse_whenDeleted() {
        // Arrange
        User user = createDefaultUserBuilder().deletedAt(OffsetDateTime.now()).build();

        // Act & Assert
        assertFalse(user.isEnabled(), "A deleted user (deletedAt is not null) should be disabled.");
    }

    @Test
    @DisplayName("getAuthorities should return an empty collection")
    void getAuthorities_shouldReturnEmptyList() {
        // Arrange
        User user = createDefaultUserBuilder().build();

        // Act
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // Assert
        assertNotNull(authorities, "Authorities collection should not be null.");
        assertTrue(authorities.isEmpty(), "Authorities collection should be empty as per implementation.");
    }

    @Test
    @DisplayName("account status flags should always return true")
    void accountStatusFlags_shouldAlwaysBeTrue() {
        // Arrange
        User user = createDefaultUserBuilder().build();

        // Act & Assert
        assertTrue(user.isAccountNonExpired(), "isAccountNonExpired should always return true.");
        assertTrue(user.isAccountNonLocked(), "isAccountNonLocked should always return true.");
        assertTrue(user.isCredentialsNonExpired(), "isCredentialsNonExpired should always return true.");
    }

    @Test
    @DisplayName("Builder should correctly create a User object with all properties")
    void builder_shouldCreateUserCorrectly() {
        // Arrange
        UUID id = UUID.randomUUID();
        String username = "johndoe";
        String email = "john.doe@work.com";
        String password = "another_hash";
        OffsetDateTime now = OffsetDateTime.now();

        // Act
        User user = User.builder()
                .userId(id)
                .username(username)
                .email(email)
                .password(password)
                .createdAt(now)
                .deletedAt(null)
                .build();

        // Assert
        assertEquals(id, user.getUserId());
        assertEquals(email, user.getEmail());
        assertEquals(password, user.getPassword());
        assertEquals(now, user.getCreatedAt());
        assertNull(user.getDeletedAt());
    }

    @Test
    @DisplayName("equals and hashCode should be based on userId")
    void equalsAndHashCode_shouldBeBasedOnUserId() {
        UUID sharedId = UUID.randomUUID();

        // Arrange
        User user1 = createDefaultUserBuilder()
                .userId(sharedId)
                .username("user_one")
                .build();

        User user2 = createDefaultUserBuilder()
                .userId(sharedId)
                .username("user_two") // Different username
                .build();

        User user3 = createDefaultUserBuilder()
                .userId(UUID.randomUUID()) // Different ID
                .build();

        // Assert
        assertEquals(user1, user2, "Users with the same userId should be equal.");
        assertEquals(user1.hashCode(), user2.hashCode(), "Hashcodes for users with the same userId should be equal.");
        assertNotEquals(user1, user3, "Users with different userIds should not be equal.");
        assertNotEquals(user1.hashCode(), user3.hashCode(), "Hashcodes for users with different userIds should not be equal.");
    }
}
