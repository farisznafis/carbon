package com.kct.jcredit.core_backend.enums;

/**
 * Categorizes an operational blocker without coupling it to billing state.
 */
public enum SuspensionReasonCode {
    BILLING,
    POLICY_VIOLATION,
    SECURITY,
    ADMINISTRATIVE,
    OTHER
}
