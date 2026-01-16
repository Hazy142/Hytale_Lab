package net.deinserver.livingorbis.roles;

public interface IGameRole {
    String getRoleName();
    void onActivate();
    void onDeactivate();
}
