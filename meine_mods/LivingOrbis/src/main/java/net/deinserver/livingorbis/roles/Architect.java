package net.deinserver.livingorbis.roles;

public class Architect implements IGameRole {
    @Override
    public String getRoleName() {
        return "Architect";
    }

    @Override
    public void onActivate() {
        // Logic for Architect activation
        System.out.println("Architect activated. Blueprint loaded.");
    }

    @Override
    public void onDeactivate() {
        System.out.println("Architect deactivated.");
    }
}
