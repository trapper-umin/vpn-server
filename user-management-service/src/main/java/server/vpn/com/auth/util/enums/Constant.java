package server.vpn.com.auth.util.enums;

public class Constant {

    public final static String REGISTER_REQUEST_MESSAGE = "POST /api/auth/register - user registration: {}";
    public final static String LOGIN_REQUEST_MESSAGE = "POST /api/auth/login - user login: {}";
    public final static String PROFILE_REQUEST_MESSAGE = "GET /api/auth/profile - getting a user profile: {}";
    public final static String LOGOUT_REQUEST_MESSAGE = "POST /api/auth/logout - log out of the system";
    public final static String LOGOUT_ALL_REQUEST_MESSAGE = "POST /api/auth/logout-all - logging out of all devices: {}";
    public final static String REFRESH_TOKEN_REQUEST_MESSAGE = "POST /api/auth/refresh - token update";
    public final static String GET_SESSIONS_REQUEST_MESSAGE = "GET /api/auth/sessions - getting active sessions: {}";
    public final static String REVOKE_SESSION_REQUEST_MESSAGE = "DELETE /api/auth/sessions/{} - revoking a session: {}";
    public final static String DELETE_ACCOUNT_REQUEST_MESSAGE = "DELETE /api/auth/account - deleting account: {}";
    public final static String BECOME_SELLER_REQUEST_MESSAGE = "POST /api/auth/become-seller - becoming a seller: {}";

    public final static String USER_ALREADY_EXIST_MESSAGE = "The user with this email already exists";
    public final static String ACCOUNT_DELETED_MESSAGE = "Account has been successfully deleted";
    public final static String INVALID_EMAIL_MESSAGE = "This email does not exist";
    public final static String INVALID_PASSWORD_MESSAGE = "Invalid password";
    public final static String INVALID_REFRESH_TOKEN_MESSAGE = "Refresh token does not exist";
    public final static String REFRESH_TOKEN_EXPIRED_OR_REVOKE_MESSAGE = "Refresh token expired or was revoked";
    public final static String SESSION_NOT_FOUND_MESSAGE = "Session not found";

    public final static String SCHEDULER_WORK_START_MESSAGE = "Starting the cleanup of expired tokens";
    public final static String SCHEDULER_WORK_END_MESSAGE = "The cleaning is complete. Refresh tokens were removed: {} expired, {} revoked";

    public final static String REFRESH_TOKEN = "refreshToken";

    public final static String SOMETHING_WRONG_MESSAGE = "Some shit happened: {}";
}
