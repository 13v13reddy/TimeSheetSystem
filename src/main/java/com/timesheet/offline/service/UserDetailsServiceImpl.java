package com.timesheet.offline.service;

import com.timesheet.offline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Service to load user-specific data for Spring Security.
 * It's used by the AuthenticationProvider to look up users from the database
 * during the authentication process.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email (which Spring Security treats as the 'username').
     * @param email The email address of the user to load.
     * @return A UserDetails object that Spring Security can use for authentication and authorization.
     * @throws UsernameNotFoundException if no user is found with the given email.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.timesheet.offline.model.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
