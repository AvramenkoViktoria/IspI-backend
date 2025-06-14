package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.AuthRequest;
import org.docpirates.ispi.dto.AuthResponse;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.enums.ContactErrorStatus;
import org.docpirates.ispi.repository.ProfileErrorRepository;
import org.docpirates.ispi.repository.SubscriptionRepository;
import org.docpirates.ispi.repository.UserRepository;
import org.docpirates.ispi.service.ContactInfoService;
import org.docpirates.ispi.service.DatabaseAuthenticationManager;
import org.docpirates.ispi.service.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DatabaseAuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileErrorRepository profileErrorRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        var auth = new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword());
        try {
            authManager.authenticate(auth);
            String token = jwtUtil.generateToken(request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        if (req.email() == null || req.email().trim().isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
        if (req.password() == null || req.password().trim().isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password is required");
        if (req.phoneNumber() == null || req.phoneNumber().trim().isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phone number is required");
        if (userRepository.existsByEmail(req.email()))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User with this email already exists");
        if (req.pib() == null || req.pib().trim().isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("PIB is required");

        if (!req.moderatorFlag()) {
            if (ContactInfoService.containsContactInfo(req.pib())) {
                ProfileError error = ProfileError.builder()
                        .description("Profile contains contact information")
                        .creationDate(LocalDateTime.now())
                        .pib(req.pib())
                        .email(req.email())
                        .phoneNumber(req.phoneNumber())
                        .role(req.role())
                        .userDescription(req.description())
                        .password(req.password())
                        .contactErrorStatus(ContactErrorStatus.REVIEW)
                        .build();
                profileErrorRepository.save(error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile contains contact information. ProfileError was created");
            }
            if ("TEACHER".equalsIgnoreCase(req.role()) && ContactInfoService.containsContactInfo(req.description())) {
                ProfileError error = ProfileError.builder()
                        .description("Profile contains contact information")
                        .creationDate(LocalDateTime.now())
                        .pib(req.pib())
                        .email(req.email())
                        .phoneNumber(req.phoneNumber())
                        .role(req.role())
                        .userDescription(req.description())
                        .password(req.password())
                        .contactErrorStatus(ContactErrorStatus.REVIEW)
                        .build();
                profileErrorRepository.save(error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile contains contact information. ProfileError was created");
            }
        }

        User user;
        if ("STUDENT".equalsIgnoreCase(req.role())) {
            user = Student.builder()
                    .pib(req.pib())
                    .email(req.email())
                    .password(passwordEncoder.encode(req.password()))
                    .phoneNumber(req.phoneNumber())
                    .subscription(null)
                    .lastActivationDate(LocalDateTime.now())
                    .bankCardNumber(req.bankCardNumber())
                    .build();
        } else if ("TEACHER".equalsIgnoreCase(req.role())) {
            user = Teacher.builder()
                    .pib(req.pib())
                    .email(req.email())
                    .password(passwordEncoder.encode(req.password()))
                    .phoneNumber(req.phoneNumber())
                    .subscription(null)
                    .lastActivationDate(LocalDateTime.now())
                    .bankCardNumber(req.bankCardNumber())
                    .description(req.description())
                    .rating(0.0f)
                    .build();
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid status value. Must be STUDENT or TEACHER.");
        }

        userRepository.save(user);
        return login(new AuthRequest(req.email(), req.password()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
        }

        String token = authHeader.substring(7);
        String email;

        try {
            email = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        userRepository.delete(user);
        return ResponseEntity.ok("Account deleted");
    }
}

record RegisterRequest(
        String pib,
        String email,
        String password,
        String phoneNumber,
        String role,
        String bankCardNumber,
        String description,
        boolean moderatorFlag
) {
}
