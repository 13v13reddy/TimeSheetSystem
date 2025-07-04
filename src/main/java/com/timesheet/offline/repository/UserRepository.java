package com.timesheet.offline.repository;

import com.timesheet.offline.model.Role;
import com.timesheet.offline.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a user by their email address.
     * @param email The email to search for.
     * @return An Optional containing the user if found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email.
     * This is more efficient than fetching the whole entity.
     * @param email The email to check.
     * @return true if a user with the email exists, false otherwise.
     */
    boolean existsByEmail(String email);

    /**
     * --- NEW METHOD ---
     * Finds all users with a specific role.
     * This is used to get all employees for PIN validation.
     * @param role The role to search for.
     * @return A list of users with the specified role.
     */
    List<User> findAllByRole(Role role);

    /**
     * --- NEW METHOD ---
     * Finds all users with a specific role, excluding a user with a given ID.
     * This is used to check for PIN uniqueness when resetting an existing user's PIN.
     * @param role The role to search for.
     * @param id The ID of the user to exclude from the results.
     * @return A list of users.
     */
    List<User> findAllByRoleAndIdNot(Role role, Long id);
}
