package com.gearexpansion.registry;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

import com.gearexpansion.GearExpansion;

/**
 * Sounds. Each armor set has its own equip sound, defined in {@code assets/gearexpansion/sounds.json}.
 *
 * <p>These are direct holders rather than registry entries: armor materials are built before
 * registries open, and items send direct sound events to clients inline, by id.
 */
public final class ModSounds {
	private ModSounds() {
	}

	/** The equip sound for a material's armor, {@code gearexpansion:item.armor.equip_<material>}. */
	public static Holder<SoundEvent> armorEquip(String material) {
		return Holder.direct(SoundEvent.createVariableRangeEvent(GearExpansion.id("item.armor.equip_" + material)));
	}
}
