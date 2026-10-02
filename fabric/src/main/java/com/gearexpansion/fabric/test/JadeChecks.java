package com.gearexpansion.fabric.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import snownee.jade.impl.WailaClientRegistration;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.block.entity.AlloyForgeBlockEntity;
import com.gearexpansion.compat.jade.AlloyForgeJadeProvider;
import com.gearexpansion.material.ModMaterials;

/**
 * Checks the Jade plugin is registered, and takes a screenshot of Jade looking at a working
 * Alloy Forge. The Jade code is in a nested class, which only loads when Jade is installed.
 */
final class JadeChecks {
	private static final BlockPos FORGE = new BlockPos(0, -60, 2);

	private JadeChecks() {
	}

	static void run(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			Jade.run(ctx, server, checker);
		}
	}

	private static final class Jade {
		static void run(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
			checker.check(ctx.computeOnClient(mc -> WailaClientRegistration.instance().getConfigKeys(GearExpansion.MOD_ID).contains(AlloyForgeJadeProvider.UID)),
				"Jade registers the Alloy Forge plugin");

			server.runCommand("clear @p");
			server.runCommand("setblock " + FORGE.getX() + " " + FORGE.getY() + " " + FORGE.getZ() + " gearexpansion:alloy_forge[facing=north]");
			server.runOnServer(s -> {
				AlloyForgeBlockEntity forge = (AlloyForgeBlockEntity) s.overworld().getBlockEntity(FORGE);
				forge.setItem(AlloyForgeBlockEntity.FIRST_INPUT_SLOT, new ItemStack(Items.COPPER_INGOT, 6));
				forge.setItem(AlloyForgeBlockEntity.FIRST_INPUT_SLOT + 1, new ItemStack(ModMaterials.ZINC.ingot.get(), 2));
				forge.setItem(AlloyForgeBlockEntity.FUEL_SLOT, new ItemStack(Items.BLAZE_POWDER, 4));
			});
			// Look down at the forge, partway through the first batch.
			server.runCommand("tp @p 0 -60 0 0 40");
			ctx.waitTicks(60);
			ctx.takeScreenshot("jade_alloy_forge");

			server.runOnServer(s -> ((AlloyForgeBlockEntity) s.overworld().getBlockEntity(FORGE)).clearContent());
			server.runCommand("setblock " + FORGE.getX() + " " + FORGE.getY() + " " + FORGE.getZ() + " minecraft:air");
			server.runCommand("kill @e[type=item]");
		}
	}
}
