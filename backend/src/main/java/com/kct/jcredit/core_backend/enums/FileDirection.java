package com.kct.jcredit.core_backend.enums;

/**
 * Direction relative to the operator (kanrisha) using this system.
 * SENT     = a document the operator prepared and handed to the VVB
 *            (certifying body) or the scheme secretariat.
 * RECEIVED = a document the operator got back from them and is
 *            keeping a copy of in this system.
 */
public enum FileDirection {
    SENT,
    RECEIVED
}
