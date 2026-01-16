package net.deinserver.livingorbis.roles;

public class GlitchWalker implements IGameRole {
    @Override
    public String getRoleName() {
        return "GlitchWalker";
    }

    @Override
    public void onActivate() {
        // Logic for GlitchWalker activation
        System.out.println("GlitchWalker activated. Anomalies detected.");
    }

    @Override
    public void onDeactivate() {
        System.out.println("GlitchWalker deactivated.");
    }
}
