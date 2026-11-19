package com.binaris.wizardry.api.content.event;

import com.binaris.wizardry.api.content.event.abstr.WizardryCancelableEvent;
import net.minecraft.server.level.ServerPlayer;

/// Fired on the server before a Resurrection changes a dead player or pays for a self-cast.
public final class EBPlayerResurrectionEvent extends WizardryCancelableEvent {
    private final ServerPlayer target;
    private final ServerPlayer caster;

    public EBPlayerResurrectionEvent(ServerPlayer target, ServerPlayer caster) {
        this.target = target;
        this.caster = caster;
    }

    public ServerPlayer getTarget() { return target; }
    public ServerPlayer getCaster() { return caster; }
}
