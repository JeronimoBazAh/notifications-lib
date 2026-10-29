package com.example.notifications.error;

import java.util.List;

/** The notification is invalid. Carries ALL violations, not just the first one. */
public final class ValidationException extends NotificationException {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("serial") // immutable List.copyOf, serializable in practice
    private final List<String> violations;

    public ValidationException(String subject, List<String> violations) {
        super(ErrorCode.VALIDATION, "Invalid " + subject + ": " + String.join("; ", violations), null);
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
