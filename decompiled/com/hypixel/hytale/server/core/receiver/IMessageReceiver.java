/*
 * Decompiled with CFR 0.152.
 */
package com.hypixel.hytale.server.core.receiver;

import com.hypixel.hytale.server.core.Message;
import javax.annotation.Nonnull;

public interface IMessageReceiver {
    public void sendMessage(@Nonnull Message var1);
}

