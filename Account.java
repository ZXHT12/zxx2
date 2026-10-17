package dev.zxht.client.account;

/** One saved account. type is "offline" or "microsoft". */
public class Account {
    public String type = "offline";
    public String username;
    /** Undashed or dashed UUID (Microsoft accounts only). */
    public String uuid;
    public String accessToken;
    public String refreshToken;
    /** Epoch millis when the Minecraft access token expires. */
    public long expiresAt;

    public boolean isMicrosoft() {
        return "microsoft".equals(type);
    }

    public String label() {
        return username + (isMicrosoft() ? "  (Microsoft)" : "  (Offline)");
    }
}
