package server.vpn.com.auth.util.enums;

public class Constant {

    public static final String REGISTER_REQUEST_MESSAGE = "POST /api/auth/register - user registration: {}";
    public static final String LOGIN_REQUEST_MESSAGE = "POST /api/auth/login - user login: {}";
    public static final String PROFILE_REQUEST_MESSAGE = "GET /api/auth/profile - getting a user profile: {}";
    public static final String LOGOUT_REQUEST_MESSAGE = "POST /api/auth/logout - log out of the system";
    public static final String LOGOUT_ALL_REQUEST_MESSAGE = "POST /api/auth/logout-all - logging out of all devices: {}";
    public static final String REFRESH_TOKEN_REQUEST_MESSAGE = "POST /api/auth/refresh - token update";
    public static final String GET_SESSIONS_REQUEST_MESSAGE = "GET /api/auth/sessions - getting active sessions: {}";
    public static final String REVOKE_SESSION_REQUEST_MESSAGE = "DELETE /api/auth/sessions/{} - revoking a session: {}";
    public static final String DELETE_ACCOUNT_REQUEST_MESSAGE = "DELETE /api/auth/account - deleting account: {}";
    public static final String BECOME_SELLER_REQUEST_MESSAGE = "POST /api/auth/become-seller - becoming a seller: {}";

    public static final String USER_ALREADY_EXIST_MESSAGE = "The user with this email already exists";
    public static final String ACCOUNT_DELETED_MESSAGE = "Account has been successfully deleted";
    public static final String INVALID_EMAIL_MESSAGE = "This email does not exist";
    public static final String INVALID_PASSWORD_MESSAGE = "Invalid password";
    public static final String INVALID_REFRESH_TOKEN_MESSAGE = "Refresh token does not exist";
    public static final String REFRESH_TOKEN_EXPIRED_OR_REVOKE_MESSAGE = "Refresh token expired or was revoked";
    public static final String SESSION_NOT_FOUND_MESSAGE = "Session not found";

    public static final String SCHEDULER_WORK_START_MESSAGE = "Starting the cleanup of expired tokens";
    public static final String SCHEDULER_WORK_END_MESSAGE = "The cleaning is complete. Refresh tokens were removed: {} expired, {} revoked";

    public static final String REFRESH_TOKEN = "refreshToken";

    public static final String SOMETHING_WRONG_MESSAGE = "Some shit happened: {}";
}
