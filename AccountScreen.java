package dev.zxht.client.ui;

import dev.zxht.client.account.Account;
import dev.zxht.client.account.AccountManager;
import dev.zxht.client.account.MicrosoftAuth;
import dev.zxht.client.config.ZxhtConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Account manager: offline (username only) accounts and Microsoft accounts. */
public class AccountScreen extends ZxhtScreen {
    private static final int PAGE_SIZE = 5;

    private TextFieldWidget nameField;
    private Account selected;
    private int page = 0;
    private volatile String status = "";
    private volatile boolean cancelLogin = false;

    public AccountScreen(Screen parent) {
        super(Text.literal("Accounts"), parent);
    }

    @Override
    protected void init() {
        List<Account> accounts = AccountManager.all();
        int pages = Math.max(1, (accounts.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages - 1);

        int cx = this.width / 2;
        int top = Math.max(40, this.height / 2 - 112);

        // account list
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = page * PAGE_SIZE + i;
            if (idx >= accounts.size()) break;
            Account a = accounts.get(idx);
            String prefix = (a == selected ? "> " : "") + (AccountManager.isActive(a) ? "[active] " : "");
            this.addDrawableChild(ButtonWidget.builder(Text.literal(prefix + a.label()), b -> {
                selected = a;
                this.clearAndInit();
            }).dimensions(cx - 110, top + i * 24, 220, 20).build());
        }

        // paging
        if (pages > 1) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                page = Math.max(0, page - 1);
                this.clearAndInit();
            }).dimensions(cx - 140, top + 2 * 24, 24, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                page = Math.min(pages - 1, page + 1);
                this.clearAndInit();
            }).dimensions(cx + 116, top + 2 * 24, 24, 20).build());
        }

        // add offline
        int rowY = top + PAGE_SIZE * 24 + 6;
        nameField = new TextFieldWidget(this.textRenderer, cx - 110, rowY, 140, 20, Text.literal("Username"));
        nameField.setPlaceholder(Text.literal("Offline username"));
        nameField.setMaxLength(16);
        this.addDrawableChild(nameField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Offline"), b -> addOffline())
                .dimensions(cx + 34, rowY, 76, 20).build());

        // actions
        int actionY = rowY + 24;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Microsoft"), b -> addMicrosoft())
                .dimensions(cx - 110, actionY, 106, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Use"), b -> useSelected())
                .dimensions(cx - 0, actionY, 52, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> deleteSelected())
                .dimensions(cx + 56, actionY, 54, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
            cancelLogin = true;
            this.close();
        }).dimensions(cx - 50, actionY + 26, 100, 20).build());
    }

    private void addOffline() {
        String name = nameField.getText().trim();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            status = "Usernames are 3-16 characters: letters, numbers, underscore.";
            return;
        }
        selected = AccountManager.addOffline(name);
        status = "Added offline account " + name + ".";
        this.clearAndInit();
    }

    private void addMicrosoft() {
        String clientId = ZxhtConfig.get().msaClientId;
        if (clientId == null || clientId.isBlank()) {
            status = "Set \"msaClientId\" in config/zxht-client.json first (see README).";
            return;
        }
        cancelLogin = false;
        status = "Contacting Microsoft...";
        CompletableFuture.runAsync(() -> {
            try {
                MicrosoftAuth.DeviceCode dc = MicrosoftAuth.begin(clientId);
                this.client.execute(() -> this.client.keyboard.setClipboard(dc.userCode()));
                status = "Open " + dc.verificationUri() + " and enter code " + dc.userCode()
                        + "  (code copied to clipboard). Waiting...";
                Account account = MicrosoftAuth.poll(clientId, dc, () -> cancelLogin || this.client.currentScreen != this);
                AccountManager.addOrReplace(account);
                this.client.execute(() -> {
                    selected = account;
                    status = "Added " + account.username + ".";
                    this.clearAndInit();
                });
            } catch (Exception e) {
                status = "Microsoft login failed: " + e.getMessage();
            }
        });
    }

    private void useSelected() {
        if (selected == null) {
            status = "Select an account first.";
            return;
        }
        final Account target = selected;
        status = "Switching to " + target.username + "...";
        CompletableFuture.runAsync(() -> {
            try {
                AccountManager.use(target);
                this.client.execute(() -> {
                    status = "Now playing as " + target.username + ".";
                    this.clearAndInit();
                });
            } catch (Exception e) {
                status = "Could not switch: " + e.getMessage();
            }
        });
    }

    private void deleteSelected() {
        if (selected == null) {
            status = "Select an account first.";
            return;
        }
        AccountManager.remove(selected);
        status = "Deleted " + selected.username + ".";
        selected = null;
        this.clearAndInit();
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        float f = fade();
        int top = Math.max(40, this.height / 2 - 112);

        String title = "A C C O U N T S";
        c.drawText(this.textRenderer, Text.literal(title), this.width / 2 - this.textRenderer.getWidth(title) / 2,
                top - 18, Draw.gray(255, (int) (255 * f)), false);

        String current = "Playing as: " + this.client.getSession().getUsername();
        c.drawText(this.textRenderer, Text.literal(current), this.width / 2 - this.textRenderer.getWidth(current) / 2,
                top - 8, Draw.gray(170, (int) (230 * f)), false);

        if (AccountManager.all().isEmpty()) {
            String empty = "No accounts yet - add one below.";
            c.drawText(this.textRenderer, Text.literal(empty), this.width / 2 - this.textRenderer.getWidth(empty) / 2,
                    top + 24, Draw.gray(150, (int) (220 * f)), false);
        }

        String s = status;
        if (!s.isEmpty()) {
            List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(s), 320);
            int y = this.height - 12 - lines.size() * 10;
            for (OrderedText line : lines) {
                c.drawText(this.textRenderer, line, this.width / 2 - 160, y, Draw.gray(255, (int) (255 * f)), false);
                y += 10;
            }
        }
    }
}
