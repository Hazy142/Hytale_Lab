/*
 * Decompiled with CFR 0.152.
 */
package com.hypixel.hytale.server.core.command.system;

import com.hypixel.hytale.common.util.StringUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbbreviationMap;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandOwner;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.MatchResult;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.AbstractOptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.ArgWrapper;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.WrappedArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.arguments.types.ListArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.exceptions.GeneralCommandException;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectBooleanPair;
import java.lang.runtime.SwitchBootstraps;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractCommand {
    @Nonnull
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    @Nonnull
    private static final Message MESSAGE_COMMANDS_HELP_NO_PERMISSIBLE_SUB_COMMAND = Message.translation("server.commands.help.noPermissibleSubCommand");
    @Nonnull
    private static final Message MESSAGE_COMMANDS_PARSING_ERROR_NO_PERMISSION_FOR_COMMAND = Message.translation("server.commands.parsing.error.noPermissionForCommand");
    @Nonnull
    private static final Message MESSAGE_COMMANDS_PARSING_ERROR_ATTEMPTED_UNSAFE = Message.translation("server.commands.parsing.error.attemptedUnsafe");
    @Nonnull
    private static final Message MESSAGE_COMMANDS_PARSING_USAGE_REQUIRES_CONFIRMATION = Message.translation("server.commands.parsing.usage.requiresConfirmation");
    @Nonnull
    private static final Message MESSAGE_COMMAND_SINGLEPLAYER = Message.translation("server.commands.parsing.error.unavailableInSingleplayer");
    @Nonnull
    static final String CONFIRM_ARG_TAG = "confirm";
    @Nonnull
    private static final String COLOR_STRING_ARG_REQUIRED = "#C1E0FF";
    @Nonnull
    private static final String COLOR_STRING_ARG_OPTIONAL = "#7E9EBC";
    private AbstractCommand parentCommand;
    @Nullable
    private final String name;
    @Nonnull
    private final Set<String> aliases = new HashSet<String>();
    private final String description;
    @Nonnull
    private final List<RequiredArg<?>> requiredArguments = new ObjectArrayList();
    @Nonnull
    private final Map<String, AbstractOptionalArg<?, ?>> optionalArguments = new Object2ObjectOpenHashMap();
    private AbbreviationMap<AbstractOptionalArg<?, ?>> argumentAbbreviationMap;
    @Nonnull
    private final Map<String, AbstractCommand> subCommands = new LinkedHashMap<String, AbstractCommand>();
    @Nonnull
    private final Map<String, String> subCommandsAliases = new LinkedHashMap<String, String>();
    @Nonnull
    private final Int2ObjectMap<AbstractCommand> variantCommands = new Int2ObjectOpenHashMap<AbstractCommand>();
    @Nullable
    private CommandOwner owner;
    @Nullable
    private String permission;
    @Nullable
    private List<String> permissionGroups;
    private int totalNumRequiredParameters;
    private final boolean requiresConfirmation;
    private boolean unavailableInSingleplayer;
    private boolean allowsExtraArguments;
    private boolean hasBeenRegistered;

    protected AbstractCommand(@Nullable String name, @Nullable String description, boolean requiresConfirmation) {
        this.name = name == null ? null : name.toLowerCase();
        this.description = description;
        this.requiresConfirmation = requiresConfirmation;
        if (requiresConfirmation) {
            this.registerOptionalArg(new FlagArg(this, CONFIRM_ARG_TAG, ""));
        }
    }

    protected AbstractCommand(@Nullable String name, @Nullable String description) {
        this(name, description, false);
    }

    protected AbstractCommand(@Nullable String description) {
        this(null, description);
    }

    public void setOwner(@Nonnull CommandOwner owner) {
        this.owner = owner;
        if (this.permission == null && this.canGeneratePermission()) {
            this.permission = this.generatePermission();
        }
        for (AbstractCommand subCommand : this.subCommands.values()) {
            subCommand.setOwner(owner);
        }
        for (AbstractCommand variantCommand : this.variantCommands.values()) {
            variantCommand.setOwner(owner);
        }
    }

    protected boolean canGeneratePermission() {
        return true;
    }

    @Nullable
    protected String generatePermissionNode() {
        return this.name == null ? null : this.name.toLowerCase();
    }

    @Nonnull
    private String generatePermission() {
        String selfNode = this.generatePermissionNode();
        if (this.parentCommand != null) {
            String parentPermission = this.parentCommand.permission == null ? this.parentCommand.generatePermission() : this.parentCommand.permission;
            Object generatedPermission = selfNode == null || selfNode.isEmpty() ? parentPermission : parentPermission + "." + selfNode;
            ((HytaleLogger.Api)LOGGER.atFine()).log("Generated missing permission '" + (String)generatedPermission + "'.");
            return generatedPermission;
        }
        CommandOwner commandOwner = this.owner;
        if (commandOwner instanceof PluginBase) {
            PluginBase plugin = (PluginBase)commandOwner;
            return plugin.getBasePermission() + ".command." + selfNode;
        }
        if (this.owner instanceof CommandManager) {
            return "hytale.system.command." + selfNode;
        }
        throw new IllegalArgumentException("Unknown owner type, please use PluginBase or CommandManager");
    }

    @Nullable
    public List<String> getPermissionGroups() {
        return this.permissionGroups;
    }

    protected void setPermissionGroups(String ... groups) {
        this.permissionGroups = Arrays.asList(groups);
    }

    protected void setPermissionGroup(@Nullable GameMode gameMode) {
        this.setPermissionGroups(gameMode == null ? null : gameMode.toString());
    }

    @Nonnull
    public Map<String, Set<String>> getPermissionGroupsRecursive() {
        Object2ObjectOpenHashMap<String, Set<String>> permissionsByGroup = new Object2ObjectOpenHashMap<String, Set<String>>();
        this.putRecursivePermissionGroups(permissionsByGroup);
        return permissionsByGroup;
    }

    public void putRecursivePermissionGroups(@Nonnull Map<String, Set<String>> permissionsByGroup) {
        List<String> permissionGroups = this.permissionGroups;
        if (permissionGroups == null && this.parentCommand != null) {
            permissionGroups = this.parentCommand.permissionGroups;
        }
        if (permissionGroups != null && this.permission != null) {
            for (String group : permissionGroups) {
                if (group == null) continue;
                permissionsByGroup.computeIfAbsent(group, k -> new HashSet()).add(this.permission);
            }
        }
        for (AbstractCommand subCommand : this.subCommands.values()) {
            subCommand.putRecursivePermissionGroups(permissionsByGroup);
        }
    }

    protected void setUnavailableInSingleplayer(boolean unavailableInSingleplayer) {
        this.unavailableInSingleplayer = unavailableInSingleplayer;
    }

    public void setAllowsExtraArguments(boolean allowsExtraArguments) {
        this.allowsExtraArguments = allowsExtraArguments;
    }

    @Nonnull
    public MatchResult matches(@Nonnull String language, @Nonnull String search, int termDepth) {
        return this.matches(language, search, termDepth, 0);
    }

    @Nonnull
    private MatchResult matches(@Nonnull String language, @Nonnull String search, int termDepth, int depth) {
        MatchResult result;
        String descriptionMessage;
        if (this.name != null && this.name.contains(search)) {
            return MatchResult.of(termDepth, depth, 0, this.name, search);
        }
        for (String string : this.aliases) {
            if (!string.contains(search)) continue;
            return MatchResult.of(termDepth, depth, 1, string, search);
        }
        for (AbstractOptionalArg abstractOptionalArg : this.optionalArguments.values()) {
            if (abstractOptionalArg.getName().contains(search)) {
                return MatchResult.of(termDepth, depth, 3, abstractOptionalArg.getName(), search);
            }
            for (String alias : abstractOptionalArg.getAliases()) {
                if (!alias.contains(search)) continue;
                return MatchResult.of(termDepth, depth, 3, alias, search);
            }
        }
        for (RequiredArg requiredArg : this.requiredArguments) {
            if (!requiredArg.getName().contains(search)) continue;
            return MatchResult.of(termDepth, depth, 3, requiredArg.getName(), search);
        }
        if (this.description != null && (descriptionMessage = I18nModule.get().getMessage(language, this.description)) != null && descriptionMessage.contains(search)) {
            return MatchResult.of(termDepth, depth, 4, descriptionMessage, search);
        }
        for (AbstractOptionalArg abstractOptionalArg : this.optionalArguments.values()) {
            String usageDescription;
            String description = abstractOptionalArg.getDescription();
            if (description == null || (usageDescription = I18nModule.get().getMessage(language, description)) == null || !usageDescription.contains(search)) continue;
            return MatchResult.of(termDepth, depth, 5, usageDescription, search);
        }
        for (AbstractCommand abstractCommand : this.subCommands.values()) {
            result = abstractCommand.matches(language, search, termDepth, depth + 1);
            if (result == MatchResult.NONE) continue;
            return result;
        }
        for (AbstractCommand abstractCommand : this.variantCommands.values()) {
            result = abstractCommand.matches(language, search, termDepth, depth + 1);
            if (result == MatchResult.NONE) continue;
            return result;
        }
        return MatchResult.NONE;
    }

    public void completeRegistration() throws GeneralCommandException {
        this.hasBeenRegistered = true;
        for (AbstractCommand command : this.subCommands.values()) {
            command.completeRegistration();
        }
        for (AbstractCommand command : this.variantCommands.values()) {
            command.completeRegistration();
        }
        this.validateVariantNumberOfRequiredParameters(new ParseResult(true));
        this.validateDefaultArguments(new ParseResult(true));
        this.createOptionalArgumentAbbreviationMap();
    }

    private void createOptionalArgumentAbbreviationMap() {
        AbbreviationMap.AbbreviationMapBuilder abbreviationMapBuilder = AbbreviationMap.create();
        for (AbstractOptionalArg<?, ?> abstractOptionalArg : this.optionalArguments.values()) {
            abbreviationMapBuilder.put(abstractOptionalArg.getName(), abstractOptionalArg);
            for (String alias : abstractOptionalArg.getAliases()) {
                abbreviationMapBuilder.put(alias, abstractOptionalArg);
            }
        }
        this.argumentAbbreviationMap = abbreviationMapBuilder.build();
    }

    private void validateVariantNumberOfRequiredParameters(@Nonnull ParseResult result) {
        for (Int2ObjectMap.Entry entry : this.variantCommands.int2ObjectEntrySet()) {
            if (this.totalNumRequiredParameters != ((AbstractCommand)entry.getValue()).totalNumRequiredParameters) continue;
            result.fail(Message.raw("Command '" + this.getFullyQualifiedName() + "' and its variant '" + ((AbstractCommand)entry.getValue()).toString() + "' both have " + this.totalNumRequiredParameters + " required parameters. Variants must have different numbers of required parameters."));
            return;
        }
    }

    private void validateDefaultArguments(@Nonnull ParseResult parseResult) {
        for (AbstractOptionalArg<?, ?> value : this.optionalArguments.values()) {
            if (!(value instanceof DefaultArg)) continue;
            DefaultArg defaultArg = (DefaultArg)value;
            defaultArg.validateDefaultValue(parseResult);
        }
    }

    public void requirePermission(@Nonnull String permission) {
        this.permission = permission;
    }

    @Nullable
    public String getFullyQualifiedName() {
        if (this.parentCommand != null) {
            if (this.isVariant()) {
                return this.parentCommand.getFullyQualifiedName();
            }
            return this.parentCommand.getFullyQualifiedName() + " " + this.name;
        }
        return this.name;
    }

    public int countParents() {
        if (this.parentCommand == null) {
            return 0;
        }
        return this.parentCommand.countParents() + 1;
    }

    public void addAliases(String ... aliases) {
        if (this.hasBeenRegistered) {
            throw new IllegalStateException("Cannot add aliases when a command has already completed registration");
        }
        if (this.name == null) {
            throw new IllegalStateException("Cannot add aliases to a command with no name");
        }
        for (String alias : aliases) {
            this.aliases.add(alias.toLowerCase());
        }
    }

    public void addSubCommand(@Nonnull AbstractCommand command) {
        if (this.hasBeenRegistered) {
            throw new IllegalStateException("Cannot add new subcommands when a command has already completed registration");
        }
        if (this.isVariant()) {
            throw new IllegalStateException("Cannot add a subcommand to a variant command, can only add subcommands to named commands");
        }
        if (command.name == null) {
            throw new IllegalArgumentException("Cannot add a subcommand with no name");
        }
        if (command.parentCommand != null) {
            throw new IllegalArgumentException("Cannot re-use subcommands. Only one parent command allowed for each subcommand");
        }
        command.parentCommand = this;
        if (this.subCommands.containsKey(command.name)) {
            throw new IllegalArgumentException("Cannot have multiple subcommands with the same name");
        }
        if (this.subCommandsAliases.containsKey(command.name)) {
            throw new IllegalArgumentException("Command has same name as existing command alias for command: " + command.name);
        }
        this.subCommands.put(command.name, command);
        for (String alias : command.aliases) {
            if (this.subCommandsAliases.containsKey(alias) || this.subCommands.containsKey(alias)) {
                throw new IllegalArgumentException("Cannot specify a subcommand alias with the same name as an existing command or alias: " + alias);
            }
            this.subCommandsAliases.put(alias, command.name);
        }
        command.hasBeenRegistered = true;
    }

    public void addUsageVariant(@Nonnull AbstractCommand command) {
        if (this.hasBeenRegistered) {
            throw new IllegalStateException("Cannot add new variants when a command has already completed registration");
        }
        if (this.isVariant()) {
            throw new IllegalStateException("Cannot add a command variant to a variant command, can only add variants to named commands");
        }
        if (command.name != null) {
            throw new IllegalArgumentException("Cannot add a variant command with a name, use the description-only constructor");
        }
        if (command.parentCommand != null) {
            throw new IllegalArgumentException("Cannot re-use variant commands. Only one parent command allowed for each variant command");
        }
        AbstractCommand variantWithSameNumRequiredParameters = this.variantCommands.put(command.totalNumRequiredParameters, command);
        if (variantWithSameNumRequiredParameters != null) {
            throw new IllegalArgumentException("You have already registered a variant command with " + command.totalNumRequiredParameters + " required parameters. Command's class name: " + variantWithSameNumRequiredParameters.getClass().getName());
        }
        command.parentCommand = this;
        command.hasBeenRegistered = true;
    }

    @Nullable
    public CompletableFuture<Void> acceptCall(@Nonnull CommandSender sender, @Nonnull ParserContext parserContext, @Nonnull ParseResult parseResult) {
        parserContext.convertToSubCommand();
        return this.acceptCall0(sender, parserContext, parseResult);
    }

    @Nullable
    private CompletableFuture<Void> acceptCall0(@Nonnull CommandSender sender, @Nonnull ParserContext parserContext, @Nonnull ParseResult parseResult) {
        int numberOfPreOptionalTokens = parserContext.getNumPreOptionalTokens();
        ObjectBooleanPair<CompletableFuture<Void>> completableFutureBooleanPair = this.checkForExecutingSubcommands(sender, parserContext, parseResult, numberOfPreOptionalTokens);
        if (parseResult.failed()) {
            return null;
        }
        if (completableFutureBooleanPair.rightBoolean()) {
            return (CompletableFuture)completableFutureBooleanPair.left();
        }
        if (!this.hasPermission(sender)) {
            parseResult.fail(MESSAGE_COMMANDS_PARSING_ERROR_NO_PERMISSION_FOR_COMMAND);
            return null;
        }
        if (this instanceof AbstractCommandCollection && numberOfPreOptionalTokens != 0) {
            HashSet<String> commandNames = new HashSet<String>(this.subCommands.keySet());
            commandNames.addAll(this.subCommandsAliases.keySet());
            String firstToken = parserContext.getFirstToken();
            String commandSuggestionPrefix = "\n/" + this.getFullyQualifiedName() + " ";
            String suggestedCommands = commandSuggestionPrefix + String.join((CharSequence)commandSuggestionPrefix, StringUtil.sortByFuzzyDistance(firstToken, commandNames, 5));
            parseResult.fail(Message.translation("server.commands.parsing.error.commandCollectionSubcommandNotFound").param("subcommand", firstToken).param("suggestions", suggestedCommands));
            return null;
        }
        if (parserContext.isHelpSpecified()) {
            sender.sendMessage(this.getUsageString(sender));
            return null;
        }
        if (this.unavailableInSingleplayer && Constants.SINGLEPLAYER) {
            parseResult.fail(MESSAGE_COMMAND_SINGLEPLAYER);
            return null;
        }
        if (this.requiresConfirmation && !parserContext.isConfirmationSpecified()) {
            parseResult.fail(MESSAGE_COMMANDS_PARSING_ERROR_ATTEMPTED_UNSAFE);
            return null;
        }
        if (this.allowsExtraArguments) {
            if (numberOfPreOptionalTokens < this.totalNumRequiredParameters) {
                parseResult.fail(Message.translation("server.commands.parsing.error.wrongNumberRequiredParameters").param("expected", this.totalNumRequiredParameters).param("actual", numberOfPreOptionalTokens).insert("\n").insert(Message.translation("server.commands.help.usagecolon").param("usage", this.getUsageShort(sender, true))).insert("\n").insert(Message.translation("server.commands.help.useHelpToLearnMore").param("command", this.getFullyQualifiedName())));
                return null;
            }
        } else if (this.totalNumRequiredParameters != numberOfPreOptionalTokens) {
            parseResult.fail(Message.translation("server.commands.parsing.error.wrongNumberRequiredParameters").param("expected", this.totalNumRequiredParameters).param("actual", numberOfPreOptionalTokens).insert("\n").insert(Message.translation("server.commands.help.usagecolon").param("usage", this.getUsageShort(sender, true))).insert("\n").insert(Message.translation("server.commands.help.useHelpToLearnMore").param("command", this.getFullyQualifiedName())));
            return null;
        }
        CommandContext commandContext = new CommandContext(this, sender, parserContext.getInputString());
        this.processRequiredArguments(parserContext, parseResult, commandContext);
        if (parseResult.failed()) {
            return null;
        }
        this.processOptionalArguments(parserContext, parseResult, commandContext);
        if (parseResult.failed()) {
            return null;
        }
        return this.execute(commandContext);
    }

    public boolean hasPermission(@Nonnull CommandSender sender) {
        String permission = this.getPermission();
        if (permission == null) {
            return true;
        }
        if (sender.hasPermission(permission)) {
            if (this.parentCommand == null) {
                return true;
            }
            return this.parentCommand.hasPermission(sender);
        }
        return false;
    }

    @Nonnull
    private ObjectBooleanPair<CompletableFuture<Void>> checkForExecutingSubcommands(@Nonnull CommandSender sender, @Nonnull ParserContext parserContext, @Nonnull ParseResult parseResult, int numberOfPreOptionalTokens) {
        if (parserContext.getNumPreOptSingleValueTokensBeforeListTokens() >= 0) {
            if (!this.subCommands.isEmpty()) {
                AbstractCommand subCommand;
                String subCommandName = parserContext.getPreOptionalSingleValueToken(0);
                if (subCommandName != null) {
                    subCommandName = subCommandName.toLowerCase();
                }
                if ((subCommand = this.subCommands.get(subCommandName)) != null) {
                    parserContext.convertToSubCommand();
                    return ObjectBooleanPair.of(subCommand.acceptCall0(sender, parserContext, parseResult), true);
                }
                String alias = this.subCommandsAliases.get(subCommandName);
                if (alias != null) {
                    parserContext.convertToSubCommand();
                    return ObjectBooleanPair.of(this.subCommands.get(alias).acceptCall0(sender, parserContext, parseResult), true);
                }
            }
            AbstractCommand commandVariant = (AbstractCommand)this.variantCommands.get(numberOfPreOptionalTokens);
            if (this.totalNumRequiredParameters != numberOfPreOptionalTokens && commandVariant != null) {
                return ObjectBooleanPair.of(commandVariant.acceptCall0(sender, parserContext, parseResult), true);
            }
        }
        return ObjectBooleanPair.of(null, false);
    }

    private void processRequiredArguments(@Nonnull ParserContext parserContext, @Nonnull ParseResult parseResult, @Nonnull CommandContext commandContext) {
        int currentReqArgIndex = 0;
        for (RequiredArg<?> requiredArgument : this.requiredArguments) {
            if (requiredArgument.getArgumentType().isListArgument() && parserContext.isListToken(currentReqArgIndex)) {
                ParserContext.PreOptionalListContext preOptionalTokenContext = parserContext.getPreOptionalListToken(currentReqArgIndex);
                ++currentReqArgIndex;
                commandContext.appendArgumentData(requiredArgument, preOptionalTokenContext.getTokens(), true, parseResult);
                continue;
            }
            String[] argParameters = new String[requiredArgument.getArgumentType().getNumberOfParameters()];
            for (int i = 0; i < requiredArgument.getArgumentType().getNumberOfParameters(); ++i) {
                if (parserContext.isListToken(currentReqArgIndex)) {
                    parseResult.fail(Message.translation("server.commands.parsing.error.notAList").param("name", requiredArgument.getName()));
                    return;
                }
                argParameters[i] = parserContext.getPreOptionalSingleValueToken(currentReqArgIndex);
                ++currentReqArgIndex;
            }
            commandContext.appendArgumentData(requiredArgument, argParameters, false, parseResult);
            if (!parseResult.failed()) continue;
            return;
        }
    }

    private void processOptionalArguments(@Nonnull ParserContext parserContext, @Nonnull ParseResult parseResult, @Nonnull CommandContext commandContext) {
        for (Map.Entry entry : parserContext.getOptionalArgs()) {
            AbstractOptionalArg<?, ?> optionalArg = this.argumentAbbreviationMap.get((String)entry.getKey());
            if (optionalArg == null) {
                parseResult.fail(Message.translation("server.commands.parsing.error.couldNotFindOptionalArgName").param("input", (String)entry.getKey()));
                return;
            }
            if (optionalArg.getPermission() != null && !commandContext.sender().hasPermission(optionalArg.getPermission())) {
                parseResult.fail(Message.translation("server.commands.parsing.error.noPermissionForOptional").param("argument", (String)entry.getKey()));
                return;
            }
            List optionalArgValues = (List)entry.getValue();
            if (optionalArg.getArgumentType().isListArgument() && optionalArgValues.size() > 1) {
                String[] optionalArgParseValues = new String[optionalArgValues.size() * ((List)optionalArgValues.getFirst()).size()];
                for (int i = 0; i < optionalArgValues.size(); ++i) {
                    List values = (List)optionalArgValues.get(i);
                    for (int k = 0; k < values.size(); ++k) {
                        optionalArgParseValues[i * values.size() + k] = (String)values.get(k);
                    }
                }
                commandContext.appendArgumentData(optionalArg, optionalArgParseValues, true, parseResult);
                continue;
            }
            commandContext.appendArgumentData(optionalArg, optionalArgValues.isEmpty() ? EMPTY_STRING_ARRAY : ((List)optionalArgValues.getFirst()).toArray(EMPTY_STRING_ARRAY), false, parseResult);
        }
        for (AbstractOptionalArg abstractOptionalArg : this.optionalArguments.values()) {
            abstractOptionalArg.verifyArgumentDependencies(commandContext, parseResult);
        }
    }

    @Nullable
    protected abstract CompletableFuture<Void> execute(@Nonnull CommandContext var1);

    @Nonnull
    public Message getUsageString(@Nonnull CommandSender sender) {
        Message requiredArgsMessage = Message.raw("");
        for (RequiredArg<?> requiredArgument : this.requiredArguments) {
            requiredArgsMessage.insert(" ").insert(requiredArgument.getUsageMessageWithoutDescription());
            if (!requiredArgument.getArgumentType().isListArgument()) continue;
            requiredArgsMessage.insert("/...");
        }
        Message requiresConfirmationMessage = this.requiresConfirmation ? MESSAGE_COMMANDS_PARSING_USAGE_REQUIRES_CONFIRMATION : Message.raw("");
        Message requiredArgs = Message.raw("");
        boolean requiredArgsShown = false;
        for (RequiredArg<?> requiredArgument : this.requiredArguments) {
            requiredArgsShown = true;
            requiredArgs.insert("\n    ").insert(requiredArgument.getUsageMessage());
        }
        Message optionalArgs = Message.raw("");
        boolean optionalArgsShown = false;
        Message defaultArgs = Message.raw("");
        boolean defaultArgsShown = false;
        Message flagArgs = Message.raw("");
        boolean flagArgsShown = false;
        for (Map.Entry<String, AbstractOptionalArg<?, ?>> entry : this.optionalArguments.entrySet()) {
            AbstractOptionalArg<?, ?> abstractOptionalArg;
            AbstractOptionalArg<?, ?> arg = entry.getValue();
            if (arg.getPermission() != null && !sender.hasPermission(arg.getPermission())) continue;
            Objects.requireNonNull(arg);
            int n = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{OptionalArg.class, DefaultArg.class, FlagArg.class}, abstractOptionalArg, n)) {
                case 0: {
                    OptionalArg optionalArg = (OptionalArg)abstractOptionalArg;
                    optionalArgsShown = true;
                    optionalArgs.insert("\n    ").insert(arg.getUsageMessage());
                    break;
                }
                case 1: {
                    DefaultArg ignored = (DefaultArg)abstractOptionalArg;
                    defaultArgsShown = true;
                    defaultArgs.insert("\n    ").insert(arg.getUsageMessage());
                    break;
                }
                case 2: {
                    FlagArg ignored = (FlagArg)abstractOptionalArg;
                    flagArgsShown = true;
                    flagArgs.insert("\n    ").insert(arg.getUsageMessage());
                    break;
                }
            }
        }
        if (requiredArgsShown) {
            requiredArgs = Message.translation("server.commands.parsing.usage.requiredArgs").param("args", requiredArgs);
        }
        if (optionalArgsShown) {
            optionalArgs = Message.translation("server.commands.parsing.usage.optionalArgs").param("args", optionalArgs);
        }
        if (flagArgsShown) {
            flagArgs = Message.translation("server.commands.parsing.usage.flagArgs").param("args", flagArgs);
        }
        if (defaultArgsShown) {
            defaultArgs = Message.translation("server.commands.parsing.usage.defaultArgs").param("args", defaultArgs);
        }
        Message variantsMessage = Message.raw("");
        for (Object value : this.variantCommands.values()) {
            if (!((AbstractCommand)value).hasPermission(sender)) continue;
            variantsMessage.insert(Message.translation("server.commands.parsing.usage.subcommands").param("separator", Message.translation("server.commands.parsing.usage.subcommandSeparator")).param("usage", ((AbstractCommand)value).getUsageString(sender)));
        }
        Message subcommandsMessage = Message.raw("");
        for (AbstractCommand value : this.subCommands.values()) {
            if (!value.hasPermission(sender)) continue;
            subcommandsMessage.insert(Message.translation("server.commands.parsing.usage.subcommands").param("separator", Message.translation("server.commands.parsing.usage.subcommandSeparator")).param("usage", value.getUsageString(sender)));
        }
        Message argTypesMessage = Message.raw("\nArgument Types:");
        HashSet allArgumentTypes = new HashSet();
        for (RequiredArg<?> requiredArg : this.requiredArguments) {
            allArgumentTypes.add(requiredArg.getArgumentType());
        }
        for (AbstractOptionalArg abstractOptionalArg : this.optionalArguments.values()) {
            allArgumentTypes.add(abstractOptionalArg.getArgumentType());
        }
        for (ArgumentType argumentType : allArgumentTypes) {
            argTypesMessage.insert("\n    ").insert(argumentType.getName()).insert(": ").insert(argumentType.getArgumentUsage()).insert("\n        Examples: ").insert("'").insert(String.join((CharSequence)"', '", argumentType.getExamples())).insert("'.");
        }
        return Message.translation("server.commands.parsing.usage.header").param("fullyQualifiedName", this.getFullyQualifiedName()).param("description", Message.translation(this.description)).param("listOfRequiredArgs", requiredArgsMessage).param("requiresConfirmation", requiresConfirmationMessage).param("requiredArgs", requiredArgs).param("optionalArgs", optionalArgs).param("defaultArgs", defaultArgs).param("flagArgs", flagArgs).param("argTypes", argTypesMessage).param("variants", variantsMessage).param("subcommands", subcommandsMessage);
    }

    @Nonnull
    public Message getUsageShort(@Nonnull CommandSender sender, boolean fullyQualify) {
        String indent = " ".repeat(this.countParents());
        if (!this.subCommands.isEmpty() || !this.variantCommands.isEmpty()) {
            String prefix = this.parentCommand == null ? "/" : indent;
            Message message = Message.raw(prefix).insert(this.name);
            boolean anyPermissible = false;
            for (AbstractCommand variantCommand : this.variantCommands.values()) {
                if (!variantCommand.hasPermission(sender)) continue;
                message.insert("\n ").insert(indent).insert(variantCommand.getUsageShort(sender, fullyQualify));
                anyPermissible = true;
            }
            for (AbstractCommand subCommand : this.subCommands.values()) {
                if (!subCommand.hasPermission(sender)) continue;
                message.insert("\n ").insert(indent).insert(subCommand.getUsageShort(sender, fullyQualify));
                anyPermissible = true;
            }
            if (!anyPermissible) {
                message.insert("\n ").insert(MESSAGE_COMMANDS_HELP_NO_PERMISSIBLE_SUB_COMMAND);
            }
            return message;
        }
        String fullyQualifiedName = this.getFullyQualifiedName();
        Message message = Message.raw(indent).insert(fullyQualify ? (fullyQualifiedName != null ? fullyQualifiedName : "???") : (this.name != null ? this.name : ""));
        for (RequiredArg<?> requiredArg : this.requiredArguments) {
            message.insert(" ").insert(requiredArg.getUsageOneLiner().color(COLOR_STRING_ARG_REQUIRED));
        }
        for (AbstractOptionalArg abstractOptionalArg : this.optionalArguments.values()) {
            if (!abstractOptionalArg.hasPermission(sender)) continue;
            message.insert(" ").insert(abstractOptionalArg.getUsageOneLiner().color(COLOR_STRING_ARG_OPTIONAL));
        }
        return message;
    }

    @Nonnull
    private <R extends RequiredArg<D>, D> R registerRequiredArg(@Nonnull R requiredArgument) {
        if (this.hasBeenRegistered) {
            throw new IllegalStateException("Cannot add new arguments when a command has already completed registration");
        }
        if (!requiredArgument.getCommandRegisteredTo().equals(this) || this.requiredArguments.contains(requiredArgument)) {
            throw new IllegalArgumentException("Cannot re-use arguments");
        }
        this.totalNumRequiredParameters += requiredArgument.getArgumentType().getNumberOfParameters();
        this.requiredArguments.add(requiredArgument);
        return requiredArgument;
    }

    @Nonnull
    private <R extends AbstractOptionalArg<?, D>, D> R registerOptionalArg(@Nonnull R optionalArgument) {
        if (this.hasBeenRegistered) {
            throw new IllegalStateException("Cannot add new arguments when a command has already completed registration");
        }
        if (!optionalArgument.getCommandRegisteredTo().equals(this) || this.optionalArguments.containsKey(optionalArgument.getName().toLowerCase())) {
            throw new IllegalArgumentException("Cannot re-use arguments");
        }
        this.optionalArguments.put(optionalArgument.getName().toLowerCase(), optionalArgument);
        return optionalArgument;
    }

    @Nonnull
    public <D> RequiredArg<D> withRequiredArg(@Nonnull String name, @Nonnull String description, @Nonnull ArgumentType<D> argType) {
        return this.registerRequiredArg(new RequiredArg<D>(this, name, description, argType));
    }

    public <W extends WrappedArg<D>, D> W withRequiredArg(@Nonnull String name, @Nonnull String description, @Nonnull ArgWrapper<W, D> wrapper) {
        return wrapper.wrapArg(this.registerRequiredArg(new RequiredArg<D>(this, name, description, wrapper.argumentType())));
    }

    @Nonnull
    public <D> RequiredArg<List<D>> withListRequiredArg(@Nonnull String name, @Nonnull String description, @Nonnull ArgumentType<D> argType) {
        return this.registerRequiredArg(new RequiredArg<D>(this, name, description, new ListArgumentType<D>(argType)));
    }

    @Nonnull
    public <D> DefaultArg<D> withDefaultArg(String name, String description, ArgumentType<D> argType, @Nullable D defaultValue, String defaultValueDescription) {
        return this.registerOptionalArg(new DefaultArg<D>(this, name, description, argType, defaultValue, defaultValueDescription));
    }

    public <W extends WrappedArg<D>, D> W withDefaultArg(@Nonnull String name, @Nonnull String description, @Nonnull ArgWrapper<W, D> wrapper, D defaultValue, @Nonnull String defaultValueDescription) {
        return wrapper.wrapArg(this.registerOptionalArg(new DefaultArg<D>(this, name, description, wrapper.argumentType(), defaultValue, defaultValueDescription)));
    }

    @Nonnull
    public <D> DefaultArg<List<D>> withListDefaultArg(@Nonnull String name, @Nonnull String description, @Nonnull ArgumentType<D> argType, List<D> defaultValue, @Nonnull String defaultValueDescription) {
        return this.registerOptionalArg(new DefaultArg<List<D>>(this, name, description, new ListArgumentType<D>(argType), defaultValue, defaultValueDescription));
    }

    @Nonnull
    public <D> OptionalArg<D> withOptionalArg(@Nonnull String name, @Nonnull String description, @Nonnull ArgumentType<D> argType) {
        return this.registerOptionalArg(new OptionalArg<D>(this, name, description, argType));
    }

    public <W extends WrappedArg<D>, D> W withOptionalArg(@Nonnull String name, @Nonnull String description, @Nonnull ArgWrapper<W, D> wrapper) {
        return wrapper.wrapArg(this.registerOptionalArg(new OptionalArg<D>(this, name, description, wrapper.argumentType())));
    }

    @Nonnull
    public <D> OptionalArg<List<D>> withListOptionalArg(@Nonnull String name, @Nonnull String description, @Nonnull ArgumentType<D> argType) {
        return this.registerOptionalArg(new OptionalArg<D>(this, name, description, new ListArgumentType<D>(argType)));
    }

    @Nonnull
    public FlagArg withFlagArg(@Nonnull String name, @Nonnull String description) {
        return this.registerOptionalArg(new FlagArg(this, name, description));
    }

    public boolean isVariant() {
        return this.name == null;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nonnull
    public Set<String> getAliases() {
        return this.aliases;
    }

    public String getDescription() {
        return this.description;
    }

    @Nullable
    public CommandOwner getOwner() {
        return this.owner;
    }

    @Nullable
    public String getPermission() {
        return this.permission;
    }

    @Nonnull
    public Map<String, AbstractCommand> getSubCommands() {
        return this.subCommands;
    }

    @Nonnull
    public List<RequiredArg<?>> getRequiredArguments() {
        return this.requiredArguments;
    }

    public boolean hasBeenRegistered() {
        return this.hasBeenRegistered;
    }
}

