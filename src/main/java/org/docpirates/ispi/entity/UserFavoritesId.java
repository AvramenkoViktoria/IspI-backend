package org.docpirates.ispi.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFavoritesId implements Serializable {
    private Long user;
    private Long document;
}
