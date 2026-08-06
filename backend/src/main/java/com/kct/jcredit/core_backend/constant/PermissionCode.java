package com.kct.jcredit.core_backend.constant;

public enum PermissionCode {

    //project management
    PROJECT_CREATE,
    PROJECT_READ,
    PROJECT_UPDATE,
    PROJECT_STATUS_UPDATE,
    PROJECT_APPLICATION_OUTPUT,

    //participating-company management
    PARTICIPATING_COMPANY_READ,
    PARTICIPATING_COMPANY_APPLICATION_REVIEW,
    PARTICIPATING_COMPANY_SUSPEND,
    MASTER_USER_PASSWORD_RESET,

    //monitoring data management
    MONITORING_READ,
    MONITORING_DELETE,
    MONITORING_CANCEL,
    MONITORING_BATCH_HISTORY_READ,
    MONITORING_BATCH_ERROR_READ,

    //operator user management
    OPERATOR_USER_READ,
    OPERATOR_USER_CREATE,
    OPERATOR_USER_UPDATE,

    //project form management
    METHODOLOGY_READ,
    APPLICATION_FORM_READ
    
}
