package com.sallahli.security.constant;

public class KeycloakUtils {

    // Verification attributes
    public static final String VERIFICATION_CODE_ATTRIBUTE = "verificationCode";
    public static final String VERIFICATION_CODE_RETRY_NUMBER = "retryNumber";
    public static final String IS_VERIFIED_ATTRIBUTE = "isVerified";
    public static final String PHONE_NUMBER_ATTRIBUTE = "phoneNumber";
    public static final String VERIFICATION_CODE_EXPIRATION_ATTRIBUTE = "verificationCodeExpiration";
    public static final String IS_ARCHIVED_ATTRIBUTE = "isArchived";

    // Roles
    public static final String CLIENT_ROLE = "CLIENT";
    public static final String PRO_ROLE = "PRO";
    public static final String PARTNER_MANAGER_ROLE = "PARTNER_MANAGER";
    public static final String PARTNER_OWNER_ROLE = "PARTNER_OWNER";
    public static final String DRIVER_ROLE = "DRIVER";
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String AGENT_ROLE = "AGENT";
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    public static final String ADMIN_PARTNER_MANAGER_ROLE = "ADMIN_PARTNER_MANAGER";
    public static final String CUSTOMER_SUPPORT_AGENT_ROLE = "CUSTOMER_SUPPORT_AGENT";
    public static final String LOGISTICS_COORDINATOR_ROLE = "LOGISTICS_COORDINATOR";
    public static final String ACCOUNTANT_ROLE = "ACCOUNTANT";

    // JWT Claims
    public static final String CLAIM_ACCOUNT_ACTIVE = "verified";
    public static final String CLAIM_CLIENT_ID = "client_id";
    public static final String CLAIM_REALM_ACCESS = "realm_access";
    public static final String CLAIM_ROLES = "roles";

    private KeycloakUtils() {
        // Utility class - prevent instantiation
    }
}
