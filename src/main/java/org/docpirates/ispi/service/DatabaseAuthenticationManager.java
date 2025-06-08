package org.docpirates.ispi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseAuthenticationManager implements AuthenticationManager {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Невірна пошта або пароль");
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities()
        );
    }
}
