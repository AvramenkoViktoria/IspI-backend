package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.AuthRequest;
import org.docpirates.ispi.entity.User;
import org.docpirates.ispi.repository.UserRepository;
import org.docpirates.ispi.service.DatabaseAuthenticationManager;
import org.docpirates.ispi.service.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DatabaseAuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("User with this email already exists");
        }

        User user = User.builder()
                .pib(req.pib())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .phoneNumber(req.phoneNumber())
                .build();

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
        String phoneNumber
) {
}

record AuthResponse(String token) {
}
