package net.deinserver.livingorbis;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.Message;

import net.deinserver.livingorbis.core.BipedalAgent;
import net.deinserver.livingorbis.persona.Archetype;
import net.deinserver.livingorbis.persona.PersonaProfile;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AletheiaCommand - Parent Command for Aletheia System
 * Supports subcommands via AbstractCommandCollection
 */
public class AletheiaCommand extends AbstractCommandCollection {

    public AletheiaCommand(LivingOrbisPlugin plugin) {
        super("aletheia", "Manage Aletheia AI agents");
        
        addSubCommand(new SpawnSubCommand(plugin));
        addSubCommand(new ListSubCommand(plugin));
        addSubCommand(new StopSubCommand(plugin));
        addSubCommand(new StopAllSubCommand(plugin));
    }

    // ==================== SUBCOMMANDS ====================

    public static class SpawnSubCommand extends AbstractCommand {
        private final LivingOrbisPlugin plugin;
        private final RequiredArg<String> nameArg;
        private final OptionalArg<String> archetypeArg;

        public SpawnSubCommand(LivingOrbisPlugin plugin) {
            super("spawn", "Spawn an AI agent");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("name", "Agent name", ArgTypes.STRING);
            this.archetypeArg = withOptionalArg("archetype", "IMITATOR|BLADE_RUNNER|OPERATOR|GLITCH", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String name = context.get(nameArg);
            String archetypeStr = context.get(archetypeArg);

            Archetype archetype = Archetype.IMITATOR;
            if (archetypeStr != null) {
                try {
                    archetype = Archetype.valueOf(archetypeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    context.sender().sendMessage(Message.raw("§cUnknown archetype: " + archetypeStr));
                    return null;
                }
            }

            // Create persona based on archetype
            PersonaProfile persona;
            switch (archetype) {
                case IMITATOR: persona = PersonaProfile.createImitator(name); break;
                case BLADE_RUNNER: persona = PersonaProfile.createBladeRunner(name); break;
                case OPERATOR: persona = PersonaProfile.createOperator(name); break;
                case GLITCH: persona = PersonaProfile.createGlitch(name); break;
                default: persona = PersonaProfile.createImitator(name);
            }

            BipedalAgent.Location spawnLocation = new BipedalAgent.Location(0, 64, 0);
            plugin.spawnAgent(name, persona, spawnLocation);

            context.sender().sendMessage(Message.raw("§a✅ Agent spawned: §f" + name));
            context.sender().sendMessage(Message.raw("§7Archetype: §e" + archetype.getDisplayName()));
            
            return null;
        }
    }

    public static class ListSubCommand extends AbstractCommand {
        private final LivingOrbisPlugin plugin;

        public ListSubCommand(LivingOrbisPlugin plugin) {
            super("list", "List active agents");
            this.plugin = plugin;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            Map<String, BipedalAgent> agents = plugin.getActiveAgents();

            if (agents.isEmpty()) {
                context.sender().sendMessage(Message.raw("§7No active agents."));
                return null;
            }

            context.sender().sendMessage(Message.raw("§6=== Active Agents (" + agents.size() + ") ==="));
            for (Map.Entry<String, BipedalAgent> entry : agents.entrySet()) {
                BipedalAgent agent = entry.getValue();
                String status = agent.isRunning() ? "§a●" : "§c●";
                context.sender().sendMessage(Message.raw(status + " §f" + entry.getKey() + 
                    " §7(" + agent.getPersona().getArchetype().getDisplayName() + ")"));
            }
            return null;
        }
    }

    public static class StopSubCommand extends AbstractCommand {
        private final LivingOrbisPlugin plugin;
        private final RequiredArg<String> nameArg;

        public StopSubCommand(LivingOrbisPlugin plugin) {
            super("stop", "Stop an agent");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("name", "Agent name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String name = context.get(nameArg);

            if (plugin.getAgent(name) == null) {
                context.sender().sendMessage(Message.raw("§cAgent not found: " + name));
                return null;
            }

            plugin.stopAgent(name);
            context.sender().sendMessage(Message.raw("§c⏹ Agent stopped: §f" + name));
            return null;
        }
    }

    public static class StopAllSubCommand extends AbstractCommand {
        private final LivingOrbisPlugin plugin;

        public StopAllSubCommand(LivingOrbisPlugin plugin) {
            super("stopall", "Stop all agents");
            this.plugin = plugin;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            int count = plugin.getActiveAgents().size();
            
            // Safe copy to avoid ConcurrentModificationException
            ArrayList<String> agentNames = new ArrayList<>(plugin.getActiveAgents().keySet());
            for (String name : agentNames) {
                plugin.stopAgent(name);
            }
            
            context.sender().sendMessage(Message.raw("§c⏹ Stopped " + count + " agents."));
            return null;
        }
    }
}
