package com.agri.mapapp.auth;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class UserRes {
    private Long id;
    private String username;
    private Role role;
    private UserStatus status;

    private String fullName;
    private String position;
    private String title;
    private String phone;
    private String avatarUrl;

    private Long orgId;        // OrganizationUnit.id
    private String orgName;    // OrganizationUnit.name
    private String department;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class CreateUserReq {
    private String username;
    private String password;
    private Role role;

    private String fullName;
    private String position;
    private String title;
    private String phone;
    private String avatarUrl;

    private Long orgId;
    private String department;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class UpdateUserReq {
    private Role role;

    private String fullName;
    private String position;
    private String title;
    private String phone;
    private String avatarUrl;

    private Long orgId;
    private String department;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class StatusReq {
    private UserStatus status;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class MoveReq {
    private Long orgId;       // OrganizationUnitga ko'chirish
    private String department;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class ResetPasswordRes {
    private String tempPassword;
}
