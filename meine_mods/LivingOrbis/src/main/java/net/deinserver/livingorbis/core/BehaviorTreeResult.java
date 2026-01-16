package net.deinserver.livingorbis.core;

import net.deinserver.livingorbis.core.BipedalAgent.Location;

/**
 * BehaviorTreeResult - Ergebnis einer System 1 (Reflex) Entscheidung
 */
public class BehaviorTreeResult {
    
    public enum ActionType {
        IDLE,
        MOVE,
        DODGE,
        LOOK,
        INTERACT
    }
    
    private final ActionType actionType;
    private Location targetLocation;
    private Location dodgeDirection;
    private Location lookTarget;
    
    public BehaviorTreeResult(ActionType actionType) {
        this.actionType = actionType;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public Location getTargetLocation() {
        return targetLocation;
    }
    
    public BehaviorTreeResult withTargetLocation(Location location) {
        this.targetLocation = location;
        return this;
    }
    
    public Location getDodgeDirection() {
        return dodgeDirection;
    }
    
    public BehaviorTreeResult withDodgeDirection(Location direction) {
        this.dodgeDirection = direction;
        return this;
    }
    
    public Location getLookTarget() {
        return lookTarget;
    }
    
    public BehaviorTreeResult withLookTarget(Location target) {
        this.lookTarget = target;
        return this;
    }
    
    public static BehaviorTreeResult idle() {
        return new BehaviorTreeResult(ActionType.IDLE);
    }
    
    public static BehaviorTreeResult moveTo(Location location) {
        return new BehaviorTreeResult(ActionType.MOVE).withTargetLocation(location);
    }
    
    public static BehaviorTreeResult dodge(Location direction) {
        return new BehaviorTreeResult(ActionType.DODGE).withDodgeDirection(direction);
    }
    
    public static BehaviorTreeResult lookAt(Location target) {
        return new BehaviorTreeResult(ActionType.LOOK).withLookTarget(target);
    }
}
