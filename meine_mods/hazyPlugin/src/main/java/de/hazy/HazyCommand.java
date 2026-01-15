package de.hazy;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HazyCommand - The /hazy command.
 * 
 * When a player types "/hazy" in chat, they receive "gang gang".
 */
public class HazyCommand extends AbstractCommand {

    public HazyCommand() {
        super("hazy", "server.commands.hazy.description");
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        context.sender().sendMessage(Message.raw("gang gang"));
        return null;
    }
}
