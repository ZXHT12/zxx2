package dev.zxht.client.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * Microsoft -> Xbox Live -> XSTS -> Minecraft login using the OAuth "device code" flow.
 * You must register your own (free) Azure app and put its client ID in config/zxht-client.json.
 */
public final class MicrosoftAuth {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final String MS = "https://login.microsoftonline.com/consumers/oauth2/v2.0/";

    public record DeviceCode(String userCode, String verificationUri, String deviceCode, int interval, int expiresIn) {}

    private MicrosoftAuth() {}

    /** Step 1: ask Microsoft for a code the user types at microsoft.com/link. */
    public static DeviceCode begin(String clientId) throws Exception {
        JsonObject o = postForm(MS + "devicecode",
                "client_id=" + enc(clientId) + "&scope=" + enc("XboxLive.signin offline_access"));
        if (o.has("error")) throw new IOException(str(o, "error_description").isEmpty() ? str(o, "error") : str(o, "error_description"));
        return new DeviceCode(
                str(o, "user_code"),
                str(o, "verification_uri"),
                str(o, "device_code"),
                o.has("interval") ? o.get("interval").getAsInt() : 5,
                o.has("expires_in") ? o.get("expires_in").getAsInt() : 900);
    }

    /** Step 2: wait until the user finished signing in, then finish the whole chain. */
    public static Account poll(String clientId, DeviceCode dc, BooleanSupplier cancelled) throws Exception {
        long deadline = System.currentTimeMillis() + dc.expiresIn() * 1000L;
        int wait = Math.max(dc.interval(), 2);
        while (System.currentTimeMillis() < deadline && !cancelled.getAsBoolean()) {
            Thread.sleep(wait * 1000L);
            JsonObject o = postForm(MS + "token",
                    "grant_type=" + enc("urn:ietf:params:oauth:grant-type:device_code")
                            + "&client_id=" + enc(clientId)
                            + "&device_code=" + enc(dc.deviceCode()));
            if (o.has("access_token")) {
                return exchange(str(o, "access_token"), str(o, "refresh_token"));
            }
            String err = str(o, "error");
            if (err.equals("authorization_pending")) continue;
            if (err.equals("slow_down")) {
                wait += 5;
                continue;
            }
            throw new IOException(err + ": " + str(o, "error_description"));
        }
        throw new IOException("Login cancelled or timed out.");
    }

    /** Uses the stored refresh token to get a fresh Minecraft token. Updates the account in place. */
    public static void refresh(String clientId, Account a) throws Exception {
        if (clientId == null || clientId.isBlank()) throw new IOException("msaClientId is not set in config/zxht-client.json");
        if (a.refreshToken == null || a.refreshToken.isBlank()) throw new IOException("No refresh token saved - please re-add this account.");
        JsonObject o = postForm(MS + "token",
                "grant_type=refresh_token&client_id=" + enc(clientId)
                        + "&refresh_token=" + enc(a.refreshToken)
                        + "&scope=" + enc("XboxLive.signin offline_access"));
        if (!o.has("access_token")) {
            throw new IOException("Session expired, please re-add this account (" + str(o, "error") + ").");
        }
        Account fresh = exchange(str(o, "access_token"), str(o, "refresh_token"));
        a.username = fresh.username;
        a.uuid = fresh.uuid;
        a.accessToken = fresh.accessToken;
        a.refreshToken = fresh.refreshToken.isEmpty() ? a.refreshToken : fresh.refreshToken;
        a.expiresAt = fresh.expiresAt;
    }

    private static Account exchange(String msAccessToken, String msRefreshToken) throws Exception {
        // Xbox Live user token
        JsonObject xblProps = new JsonObject();
        xblProps.addProperty("AuthMethod", "RPS");
        xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
        xblProps.addProperty("RpsTicket", "d=" + msAccessToken);
        JsonObject xblReq = new JsonObject();
        xblReq.add("Properties", xblProps);
        xblReq.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xblReq.addProperty("TokenType", "JWT");
        JsonObject xbl = postJson("https://user.auth.xboxlive.com/user/authenticate", xblReq, null);
        String xblToken = str(xbl, "Token");
        String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        // XSTS token for Minecraft services
        JsonArray userTokens = new JsonArray();
        userTokens.add(xblToken);
        JsonObject xstsProps = new JsonObject();
        xstsProps.addProperty("SandboxId", "RETAIL");
        xstsProps.add("UserTokens", userTokens);
        JsonObject xstsReq = new JsonObject();
        xstsReq.add("Properties", xstsProps);
        xstsReq.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsReq.addProperty("TokenType", "JWT");
        JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsReq, null);
        if (xsts.has("XErr")) {
            throw new IOException(describeXboxError(xsts.get("XErr").getAsLong()));
        }
        String xstsToken = str(xsts, "Token");

        // Minecraft access token
        JsonObject mcReq = new JsonObject();
        mcReq.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
        JsonObject mc = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcReq, null);
        String mcToken = str(mc, "access_token");
        if (mcToken.isEmpty()) throw new IOException("Minecraft login failed: " + mc);
        long expiresIn = mc.has("expires_in") ? mc.get("expires_in").getAsLong() : 86400L;

        // Profile (also checks the account owns Java Edition)
        HttpRequest profileReq = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + mcToken)
                .GET().build();
        HttpResponse<String> profileRes = HTTP.send(profileReq, HttpResponse.BodyHandlers.ofString());
        if (profileRes.statusCode() == 404) {
            throw new IOException("This Microsoft account does not own Minecraft: Java Edition.");
        }
        JsonObject profile = JsonParser.parseString(profileRes.body()).getAsJsonObject();

        Account a = new Account();
        a.type = "microsoft";
        a.username = str(profile, "name");
        a.uuid = str(profile, "id");
        a.accessToken = mcToken;
        a.refreshToken = msRefreshToken == null ? "" : msRefreshToken;
        a.expiresAt = System.currentTimeMillis() + expiresIn * 1000L;
        return a;
    }

    private static String describeXboxError(long code) {
        if (code == 2148916233L) return "This Microsoft account has no Xbox profile. Sign in once at xbox.com to create one.";
        if (code == 2148916238L) return "This is a child account; it must be added to a Microsoft family by an adult.";
        if (code == 2148916235L) return "Xbox Live is not available in your country/region.";
        return "Xbox login failed (code " + code + ").";
    }

    private static JsonObject postForm(String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return parse(HTTP.send(req, HttpResponse.BodyHandlers.ofString()));
    }

    private static JsonObject postJson(String url, JsonObject body, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        return parse(HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString()));
    }

    private static JsonObject parse(HttpResponse<String> res) throws IOException {
        try {
            return JsonParser.parseString(res.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException("Unexpected response (HTTP " + res.statusCode() + ")");
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
