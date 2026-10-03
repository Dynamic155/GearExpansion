package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/**
 * Silence: while sneaking, the wearer makes no vibrations at all, so sculk sensors, shriekers, and
 * wardens can't hear them; and hostile mobs notice them from shorter range.
 *
 * <p>Also decides the per-item Echo traits: Muffled tools and Soft Step boots.
 */
public final class EchoSetBonus extends SetBonus {
	public EchoSetBonus() {
		super(ModMaterials.ECHO);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().echoSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(
			Component.translatable("set_bonus.gearexpansion.echo.silence"),
			Component.translatable("set_bonus.gearexpansion.echo.unseen", GearExpansionConfig.get().echoDetectionReduction));
	}

	/**
	 * Whether a vibration caused by {@code source} should be silenced: mining with an echo tool,
	 * footsteps in echo boots, or anything at all while sneaking in the full set.
	 */
	public boolean muffles(Entity source, Holder<GameEvent> event) {
		if (!(source instanceof ServerPlayer player)) {
			return false;
		}
		MaterialSet echo = ModMaterials.ECHO;
		if (event.is(GameEvent.BLOCK_DESTROY.key()) && echo.tools().stream().anyMatch(tool -> player.getMainHandItem().is(tool.get()))) {
			return true;
		}
		if ((event.is(GameEvent.STEP.key()) || event.is(GameEvent.HIT_GROUND.key()))
				&& player.getItemBySlot(EquipmentSlot.FEET).is(echo.boots.get())) {
			return true;
		}
		return player.isShiftKeyDown() && isActive(player);
	}

	/** Scales how visible a wearer is to a hostile mob, which shortens the range it notices them from. */
	public double modifyVisibility(Player player, double visibility) {
		if (!isActive(player)) {
			return visibility;
		}
		return visibility * (1.0 - Mth.clamp(GearExpansionConfig.get().echoDetectionReduction, 0, 90) / 100.0);
	}
}
