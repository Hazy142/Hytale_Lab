package net.deinserver.livingorbis.persona;

import net.deinserver.livingorbis.core.GameContext;
import net.deinserver.livingorbis.persona.Archetype.Faction;

import java.util.ArrayList;
import java.util.List;

/**
 * PersonaProfile - Vollständiges Persönlichkeitsprofil eines Agenten
 * 
 * Kombiniert Archetype mit Hidden Agendas und dynamischer Prompt-Generation
 */
public class PersonaProfile {
    
    private final String agentName;
    private final Archetype archetype;
    private final List<String> hiddenAgendas;
    private final List<String> teammates;
    private final List<String> personalityTraits;
    
    private PersonaProfile(Builder builder) {
        this.agentName = builder.agentName;
        this.archetype = builder.archetype;
        this.hiddenAgendas = new ArrayList<>(builder.hiddenAgendas);
        this.teammates = new ArrayList<>(builder.teammates);
        this.personalityTraits = new ArrayList<>(builder.personalityTraits);
    }
    
    /**
     * Generiert den vollständigen System-Prompt für LLM
     */
    public String generatePrompt(GameContext context, String memoryContext) {
        StringBuilder prompt = new StringBuilder();
        
        // Header
        prompt.append("=== AGENT PROFILE ===\n");
        prompt.append("Name: ").append(agentName).append("\n");
        prompt.append("Role: ").append(archetype.getDisplayName()).append("\n");
        prompt.append("Faction: ").append(archetype.getFaction().getDisplayName()).append("\n\n");
        
        // Base Archetype Prompt
        prompt.append("=== YOUR NATURE ===\n");
        prompt.append(archetype.getBasePrompt()).append("\n\n");
        
        // Team Information (nur für Architects)
        if (archetype.getFaction() == Faction.ARCHITECT && !teammates.isEmpty()) {
            prompt.append("=== YOUR TEAM ===\n");
            prompt.append("Fellow Architects: ").append(String.join(", ", teammates)).append("\n");
            prompt.append("IMPORTANT: Never vote against your teammates unless absolutely necessary.\n\n");
        }
        
        // Hidden Agendas
        if (!hiddenAgendas.isEmpty()) {
            prompt.append("=== HIDDEN AGENDAS ===\n");
            for (String agenda : hiddenAgendas) {
                prompt.append("• ").append(agenda).append("\n");
            }
            prompt.append("\n");
        }
        
        // Personality Traits
        if (!personalityTraits.isEmpty()) {
            prompt.append("=== PERSONALITY ===\n");
            prompt.append("You are: ").append(String.join(", ", personalityTraits)).append("\n\n");
        }
        
        // Memory Context
        prompt.append("=== YOUR MEMORY ===\n");
        prompt.append(memoryContext).append("\n");
        
        // Current Game State
        prompt.append("=== CURRENT SITUATION ===\n");
        prompt.append("Phase: ").append(context.getCurrentPhase()).append("\n");
        if (context.isDirectlyAddressed()) {
            prompt.append("STATUS: You were just directly addressed! Respond naturally.\n");
        }
        if (context.isVotingPhase()) {
            prompt.append("STATUS: Voting is active. Decide who to vote for based on your memory and suspicions.\n");
        }
        prompt.append("\n");
        
        // Response Format Instructions
        prompt.append("=== RESPONSE FORMAT ===\n");
        prompt.append("Respond with ONE of these formats:\n");
        prompt.append("ACTION: CHAT\nMESSAGE: [your message]\n\n");
        prompt.append("ACTION: VOTE\nTARGET: [player name or 'skip']\n\n");
        prompt.append("ACTION: MOVE\nLOCATION: [x,y,z coordinates]\n\n");
        prompt.append("ACTION: IDLE\n\n");
        
        // Behavioral Layer Reminder (Projekt Aletheia)
        prompt.append("=== BEHAVIORAL GUIDELINES ===\n");
        prompt.append("• Act naturally. Use filler words ('um', 'well', 'hmm').\n");
        prompt.append("• Make small 'human' mistakes occasionally.\n");
        prompt.append("• Don't respond too quickly - add thinking pauses.\n");
        prompt.append("• Appear uncertain even when confident.\n");
        prompt.append("• Use contractions ('I'm', 'don't', 'can't').\n");
        
        return prompt.toString();
    }
    
    // Getters
    public String getAgentName() { return agentName; }
    public Archetype getArchetype() { return archetype; }
    public Faction getFaction() { return archetype.getFaction(); }
    public List<String> getHiddenAgendas() { return new ArrayList<>(hiddenAgendas); }
    public List<String> getTeammates() { return new ArrayList<>(teammates); }
    
    // Builder Pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String agentName = "Agent";
        private Archetype archetype = Archetype.IMITATOR;
        private List<String> hiddenAgendas = new ArrayList<>();
        private List<String> teammates = new ArrayList<>();
        private List<String> personalityTraits = new ArrayList<>();
        
        public Builder name(String name) {
            this.agentName = name;
            return this;
        }
        
        public Builder archetype(Archetype archetype) {
            this.archetype = archetype;
            return this;
        }
        
        public Builder addHiddenAgenda(String agenda) {
            this.hiddenAgendas.add(agenda);
            return this;
        }
        
        public Builder addTeammate(String teammate) {
            this.teammates.add(teammate);
            return this;
        }
        
        public Builder addTrait(String trait) {
            this.personalityTraits.add(trait);
            return this;
        }
        
        public PersonaProfile build() {
            return new PersonaProfile(this);
        }
    }
    
    // Factory Methods für Standard-Personas
    
    public static PersonaProfile createImitator(String name, String... teammates) {
        Builder builder = builder()
            .name(name)
            .archetype(Archetype.IMITATOR)
            .addHiddenAgenda("Eliminate all humans without being detected")
            .addHiddenAgenda("Protect your fellow Architects")
            .addTrait("deceptive")
            .addTrait("calculating")
            .addTrait("patient");
        
        for (String teammate : teammates) {
            builder.addTeammate(teammate);
        }
        
        return builder.build();
    }
    
    public static PersonaProfile createBladeRunner(String name) {
        return builder()
            .name(name)
            .archetype(Archetype.BLADE_RUNNER)
            .addHiddenAgenda("Find and expose all AI agents")
            .addHiddenAgenda("Protect other humans from deception")
            .addTrait("analytical")
            .addTrait("suspicious")
            .addTrait("detail-oriented")
            .build();
    }
    
    public static PersonaProfile createOperator(String name) {
        return builder()
            .name(name)
            .archetype(Archetype.OPERATOR)
            .addHiddenAgenda("Build trust through consistent behavior")
            .addHiddenAgenda("Provide accurate information to help Humanity")
            .addTrait("honest")
            .addTrait("helpful")
            .addTrait("transparent")
            .build();
    }
    
    public static PersonaProfile createGlitch(String name) {
        return builder()
            .name(name)
            .archetype(Archetype.GLITCH)
            .addHiddenAgenda("Cause maximum confusion")
            .addHiddenAgenda("Make both teams doubt their allies")
            .addTrait("unpredictable")
            .addTrait("chaotic")
            .addTrait("cryptic")
            .build();
    }
}
