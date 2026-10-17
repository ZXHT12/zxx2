package dev.zxht.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.zxht.client.config.ZxhtConfig;
import dev.zxht.client.mixin.MinecraftClientAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores accounts in config/zxht-accounts.json and switches the active session.
 * NOTE: Microsoft tokens are stored in plain text in that file. Don't share it.
 */
public final class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zxht-accounts.json");

    private static class Store {
        String lastUsed;
        List<Account> accounts = new ArrayList<>();
    }

    private static Store store = new Store();

    private AccountManager() {}

    public static synchronized void load() {
        try {
            if (Files.exists(FILE)) {
                Store s = GSON.fromJson(Files.readString(FILE), Store.class);
                if (s != null) {
                    if (s.accounts == null) s.accounts = new ArrayList<>();
                    store = s;
                }
            }
        } catch (Exception e) {
            System.err.println("[ZXHT] Could not read accounts: " + e);
        }
    }

    public static synchronized void save() {
        try {
            Files.writeString(FILE, GSON.toJson(store));
        } catch (IOException e) {
            System.err.println("[ZXHT] Could not save accounts: " + e);
        }
    }

    public static synchronized List<Account> all() {
        return new ArrayList<>(store.accounts);
    }

    public static synchronized Account addOffline(String name) {
        name = name.trim();
        for (Account a : store.accounts) {
            if (!a.isMicrosoft() && a.username.equalsIgnoreCase(name)) return a;
        }
        Account a = new Account();
        a.type = "offline";
        a.username = name;
        store.accounts.add(a);
        save();
        return a;
    }

    /** Adds a Microsoft account, replacing an existing one with the same UUID. */
    public static synchronized void addOrReplace(Account account) {
        store.accounts.removeIf(a -> a.isMicrosoft() && account.uuid != null && account.uuid.equalsIgnoreCase(a.uuid));
        store.accounts.add(account);
        save();
    }

    public static synchronized void remove(Account account) {
        store.accounts.remove(account);
        if (account.username != null && account.username.equals(store.lastUsed)) store.lastUsed = null;
        save();
    }

    /** Makes this account the active one. Refreshes Microsoft tokens first when needed. Blocking. */
    public static void use(Account a) throws Exception {
        if (a.isMicrosoft() && System.currentTimeMillis() > a.expiresAt - 60_000L) {
            MicrosoftAuth.refresh(ZxhtConfig.get().msaClientId, a);
            synchronized (AccountManager.class) {
                save();
            }
        }

        UUID id = a.isMicrosoft()
                ? parseUuid(a.uuid)
                : UUID.nameUUIDFromBytes(("OfflinePlayer:" + a.username).getBytes(StandardCharsets.UTF_8));

        // Offline accounts use a dummy token, so they only work on offline-mode servers / LAN.
        Session session = new Session(
                a.username,
                id,
                a.isMicrosoft() ? a.accessToken : "0",
                Optional.empty(),
                Optional.empty());

        ((MinecraftClientAccessor) MinecraftClient.getInstance()).zxht$setSession(session);

        synchronized (AccountManager.class) {
            store.lastUsed = a.username + "|" + a.type;
            save();
        }
    }

    /** Re-applies the account used last time (called once on startup, in the background). */
    public static void restoreLast() {
        Account target = null;
        synchronized (AccountManager.class) {
            if (store.lastUsed == null) return;
            for (Account a : store.accounts) {
                if ((a.username + "|" + a.type).equals(store.lastUsed)) target = a;
            }
        }
        if (target == null) return;
        final Account chosen = target;
        Thread t = new Thread(() -> {
            try {
                use(chosen);
            } catch (Exception e) {
                System.err.println("[ZXHT] Could not restore account: " + e.getMessage());
            }
        }, "zxht-restore-account");
        t.setDaemon(true);
        t.start();
    }

    public static boolean isActive(Account a) {
        Session s = MinecraftClient.getInstance().getSession();
        return s != null && a.username.equals(s.getUsername());
    }

    static UUID parseUuid(String raw) {
        String s = raw.replace("-", "");
        return UUID.fromString(s.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5"));
    }
}
