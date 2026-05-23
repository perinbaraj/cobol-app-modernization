package com.acme.customer.model;

/**
 * Phone type enumeration.
 * Migrated from CUST-REC copybook 88-levels:
 * - 88 PHONE-HOME VALUE 'H'
 * - 88 PHONE-WORK VALUE 'W'
 * - 88 PHONE-MOBILE VALUE 'M'
 */
public enum PhoneType {
    HOME('H'),
    WORK('W'),
    MOBILE('M');

    private final char code;

    PhoneType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static PhoneType fromCode(char code) {
        for (PhoneType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid phone type code: " + code);
    }

    public static PhoneType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return HOME; // Default
        }
        return fromCode(code.charAt(0));
    }
}
