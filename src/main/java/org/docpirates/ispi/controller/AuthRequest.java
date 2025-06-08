package org.docpirates.ispi.controller;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthRequest {
    private String username;
    private String password;
}
