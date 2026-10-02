package com.gearexpansion.compat.jade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.ConfigIcon;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

import com.gearexpansion.registry.ModItems;

/**
 * Shows what the Alloy Forge is doing when you look at it: its inputs, fuel, a progress arrow,
 * and the result, plus a line when a boost fuel is burning. The data comes from
 * {@link AlloyForgeJadeProvider}.
 */
public final class AlloyForgeJadeClient implements IBlockComponentProvider {
	public static final AlloyForgeJadeClient INSTANCE = new AlloyForgeJadeClient();

	private AlloyForgeJadeClient() {
	}

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		AlloyForgeJadeProvider.Data data = AlloyForgeJadeProvider.INSTANCE.decodeFromData(accessor).orElse(null);
		if (data == null || data.inventory().stream().allMatch(ItemStack::isEmpty)) {
			return;
		}

		List<LayoutElement> line = new ArrayList<>();
		// Inputs, skipping empty slots so the row stays short.
		for (ItemStack input : data.inventory().subList(0, 3)) {
			if (!input.isEmpty()) {
				line.add(JadeUI.item(input).alignSelfCenter());
			}
		}
		line.add(JadeUI.item(data.inventory().get(3)).alignSelfCenter());
		float progress = data.total() > 0 ? (float) data.progress() / data.total() : 0.0F;
		line.add(JadeUI.progressArrow(progress).alignSelfCenter().settings(settings -> settings.paddingHorizontal(-2)));
		line.add(JadeUI.item(data.inventory().get(4)).alignSelfCenter());

		tooltip.add(line.getFirst());
		for (LayoutElement element : line.subList(1, line.size())) {
			tooltip.append(element);
		}
		if (data.boosted()) {
			tooltip.add(Component.translatable("jade.gearexpansion.alloy_forge.boosted"));
		}
	}

	@Override
	public Identifier getUid() {
		return AlloyForgeJadeProvider.UID;
	}

	@Override
	public ConfigIcon getConfigIcon() {
		return ConfigIcon.item(ModItems.ALLOY_FORGE.get());
	}
}
