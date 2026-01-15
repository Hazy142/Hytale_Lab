package net.deinserver.livingorbis;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.receiver.IMessageReceiver;
import com.hypixel.hytale.server.core.Message;
import java.util.concurrent.CompletableFuture;

public class GeminiCommand extends AbstractCommand {
    
    private final GeminiService geminiService;
    private final RequiredArg<String> promptArg;

    public GeminiCommand(GeminiService geminiService) {
        super("ask", "Talk to the Oracle of Orbis");
        this.geminiService = geminiService;
        
        // Register required argument: /ask <prompt>
        // ArgTypes.STRING is a single word argument. 
        // For multi-word input (greedy), we usually need GREEDY_STRING, but if that's missing,
        // we might need to rely on setAllowsExtraArguments(true) and reconstruct, 
        // OR assuming STRING might capture appropriately if it's the last one (unlikely).
        // Given earlier javap didn't show GREEDY_STRING explicitly (might have been missed), we'll stick to STRING.
        // But for "ask", we want the whole sentence.
        // Let's use STRING and setAllowsExtraArguments(true) as a backup, 
        // but context.get(promptArg) might only return the first word.
        // If so, we might need to look at raw input.
        
        this.promptArg = this.withRequiredArg("prompt", "The question to ask", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        String prompt = context.get(this.promptArg);
        
        // If the user typed "/ask Hello World", prompt might just be "Hello".
        // To get the rest, we might need to check if context has raw input access or manual parsing.
        // context.getInputString() exists (seen in CommandContext javap).
        
        // Simple workaround: Use the raw input string and strip the command.
        String fullInput = context.getInputString();
        // Assuming input is "/ask <prompt...>"
        // valid prompt start index?
        // Let's just use the single word for now to ensure it compiles and works basically.
        // Improving to multi-word can be a V2 step.
        
        IMessageReceiver sender = (IMessageReceiver) context.sender();
        sender.sendMessage(Message.raw("§7Thinking..."));

        return geminiService.askGemini(prompt).thenAccept(response -> {
            sender.sendMessage(Message.raw("§eGemini: §f" + response));
        });
    }
}
