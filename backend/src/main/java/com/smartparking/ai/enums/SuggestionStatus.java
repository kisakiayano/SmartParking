package com.smartparking.ai.enums;

public enum SuggestionStatus {

    PENDING,
    PROCESSING,
    RESOLVED,
    REJECTED;

    public boolean isTerminal() {
        return this == RESOLVED || this == REJECTED;
    }

    public boolean canTransitionTo(SuggestionStatus target) {
        if (target == null || isTerminal()) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == PROCESSING || target.isTerminal();
            case PROCESSING -> target.isTerminal();
            default -> false;
        };
    }
}
