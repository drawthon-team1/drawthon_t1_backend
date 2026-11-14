package com.drawthon.team1.entity;

public enum MemberRole {
    ADMIN,
    USER;

    public String authority() {
        return "ROLE_" + name();
    }
}
