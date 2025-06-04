package org.docpirates.ispi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Moderator {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    private User user;

    private String description;
    private String role;
    private String email;
    private String phoneNumber;
    private Integer rating;
}
