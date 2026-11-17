package com.binaris.wizardry.attachments.stack;

import com.binaris.wizardry.api.content.data.ConjureData;
import com.binaris.wizardry.setup.registries.EBDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Here for loading/storing conjure item data. CCA components rely on NBT storage, so this could be easily synced and
 * persisted without any problems. This is also why there's no fields here; all data is stored in the ItemStack's NBT.
 */
public class ConjureDataHolder extends StackDataHolder implements ConjureData {
    public ConjureDataHolder(ItemStack stack) {
        super(stack, EBDataComponents.CONJURE, "ebwizardry:conjure");
    }

    @Override
    public long getExpireTime() {
        var data = read();
        return data.contains("expire_time") ? data.getLong("expire_time") : -1L;
    }

    @Override
    public void setExpireTime(long expireTime) {
        update(data -> data.putLong("expire_time", expireTime));
    }

    @Override
    public int getDuration() {
        return read().getInt("duration");
    }

    @Override
    public void setDuration(int duration) {
        update(data -> data.putInt("duration", duration));
    }

    @Override
    public boolean isSummoned() {
        return read().getBoolean("is_summoned");
    }

    @Override
    public void setSummoned(boolean summoned) {
        update(data -> data.putBoolean("is_summoned", summoned));
    }
}
