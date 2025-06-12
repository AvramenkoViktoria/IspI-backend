package org.docpirates.ispi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserDownloadsId.class)
public class UserDownloads {
    @Id
    @ManyToOne
    private User user;

    @Id
    @ManyToOne
    private Document document;
}
