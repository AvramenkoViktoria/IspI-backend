package org.docpirates.ispi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.docpirates.ispi.enums.ContactErrorStatus;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private LocalDateTime creationDate;

    private String pib;
    private String email;
    private String phoneNumber;
    private String role;
    private String userDescription;
    private String password;
    private ContactErrorStatus contactErrorStatus;
    private long profileId;

}
