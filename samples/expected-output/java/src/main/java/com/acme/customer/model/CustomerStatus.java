package com.acme.customer.model;

/**
 * Customer status enumeration.
 * Migrated from CUST-REC copybook 88-levels:
 * - 88 CUST-ACTIVE VALUE 'A'
 * - 88 CUST-INACTIVE VALUE 'I'
 * - 88 CUST-SUSPENDED VALUE 'S'
 */
public enum CustomerStatus {
    ACTIVE('A'),
    INACTIVE('I'),
    SUSPENDED('S');

    private final char code;

    CustomerStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    /**
     * Convert COBOL status code to enum.
     *
     * @param code the single-character COBOL status code
     * @return the corresponding CustomerStatus enum value
     * @throws IllegalArgumentException if code is not valid
     */
    public static CustomerStatus fromCode(char code) {
        for (CustomerStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid customer status code: " + code);
    }

    public static CustomerStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ACTIVE; // Default for COBOL spaces
        }
        return fromCode(code.charAt(0));
    }
}
