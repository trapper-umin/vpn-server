package server.vpn.com.servermanagement.util.enums;

public class ServerManagementConstants {
    public static final String SERVER_NAME_ALREADY_EXISTS = "A server with that name already exists";
    public static final String SUBSCRIPTION_NAME_ALREADY_EXISTS = "A subscription with that name already exists for this server.";
    public static final String SERVER_NOT_FOUND = "Server not found";
    public static final String SUBSCRIPTION_NOT_FOUND = "Subscription not found";
    public static final String THERE_ARE_ACTIVE_SUBSCRIPTIONS = "You cannot delete a server with active subscriptions";
    public static final String THERE_ARE_ACTIVE_SUBSCRIBERS = "You cannot delete a subscription with active subscribers";
    public static final String CANNOT_BUY_OWN_SUBSCRIPTION = "The seller cannot buy his own subscriptions";
    public static final String IS_NOT_AVAILABLE_FOR_PURCHASE = "The subscription is not available for purchase";
    public static final String MAXIMUM_NUMBER_OF_SUBSCRIBERS_FOR_THIS_SUBSCRIPTION = "The maximum number of subscribers for this plan has been reached";
    public static final String ALREADY_HAVE_THIS_SUBSCRIPTION = "You already have this subscription";
    public static final String USER_NOT_FOUND = "User not found or inactive";
    public static final String THE_SUBSCRIPTION_DOES_NOT_BELONG_TO_THE_USER = "The subscription does not belong to the user";
    public static final String THE_SUBSCRIPTION_IS_INACTIVE = "The subscription is inactive";
    public static final String MAX_CONNECTIONS_EXCEEDS_SERVER_LIMIT = "Максимальное количество подписчиков плана не может превышать лимит сервера";
    public static final String SERVER_CONNECTION_CAPACITY_EXCEEDED = "Превышена максимальная вместимость сервера по подписчикам";


    public static final String AUTHORIZATION_HEADER = "Authorization";
}
