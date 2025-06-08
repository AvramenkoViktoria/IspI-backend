package org.docpirates.ispi.service;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.User;
import org.docpirates.ispi.entity.Moderator;
import org.docpirates.ispi.entity.Teacher;
import org.docpirates.ispi.entity.Student;
import org.docpirates.ispi.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Користувача не знайдено: " + email));

        String role = switch (user) {
            case Moderator moderator -> "MODERATOR";
            case Teacher teacher -> "TEACHER";
            case Student student -> "STUDENT";
            case null, default -> throw new UsernameNotFoundException("Неможливо визначити роль користувача: " + email);
        };

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(role)
                .build();
    }
}

