package net.deinserver.livingorbis;

public enum PacketType {
    MOVEMENT_INPUT(0x01, "MovementInput"),
    MOVEMENT_UPDATE(0x02, "MovementUpdate"),
    CHAT_MESSAGE(0x03, "ChatMessage"),
    CHAT_BROADCAST(0x04, "ChatBroadcast"),
    BLOCK_INTERACTION(0x05, "BlockInteraction"),
    BLOCK_UPDATE(0x06, "BlockUpdate"),
    ITEM_USE(0x07, "ItemUse"),
    ENTITY_SPAWN(0x08, "EntitySpawn"),
    PLAYER_ACTION(0x09, "PlayerAction"),
    GAME_PHASE_CHANGE(0x0F, "GamePhaseChange");

    private final int id;
    private final String name;

    PacketType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static PacketType fromId(int id) {
        for (PacketType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}
