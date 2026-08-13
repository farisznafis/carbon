package com.kct.jcredit.core_backend.constant;
//the permission list for operator user and admin user, used in @PreAuthorize annotation
//add this to the start of each endpoint when needeed
//and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_SUSPEND.name())
//comment added for all that have been included
public enum PermissionCode {

    //project management
    PROJECT_CREATE, //included
    PROJECT_READ, //included
    PROJECT_UPDATE, //included
    PROJECT_STATUS_UPDATE, //included
    PROJECT_APPLICATION_OUTPUT,
    PROJECT_VIEW, //included

    //participating-company management
    PARTICIPATING_COMPANY_READ, //included
    PARTICIPATING_COMPANY_APPLICATION_REVIEW, //included
    PARTICIPATING_COMPANY_SUSPEND, //included
    MASTER_USER_PASSWORD_RESET, //included

    //monitoring data management
    MONITORING_READ, //included
    MONITORING_DELETE,
    MONITORING_CANCEL, //included
    MONITORING_BATCH_HISTORY_READ,//included
    MONITORING_BATCH_ERROR_READ, 

    //operator user management
    OPERATOR_USER_READ,   //included
    OPERATOR_USER_CREATE, //included
    OPERATOR_USER_UPDATE, //included

    //project form management
    METHODOLOGY_READ,   
    APPLICATION_FORM_READ
    
}
