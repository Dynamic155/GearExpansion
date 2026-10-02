package com.gearexpansion.compat.jade;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.block.entity.AlloyForgeBlockEntity;

/**
 * Sends the Alloy Forge's slots and progress from the server to Jade, like Jade does for furnaces.
 * {@link AlloyForgeJadeClient} draws them.
 */
public final class AlloyForgeJadeProvider implements StreamServerDataProvider<BlockAccessor, AlloyForgeJadeProvider.Data> {
	public static final AlloyForgeJadeProvider INSTANCE = new AlloyForgeJadeProvider();
	public static final Identifier UID = GearExpansion.id("alloy_forge");

	private AlloyForgeJadeProvider() {
	}

	@Override
	public boolean shouldRequestData(BlockAccessor accessor) {
		return accessor.getBlockEntity() instanceof AlloyForgeBlockEntity;
	}

	@Override
	public Data streamData(BlockAccessor accessor) {
		AlloyForgeBlockEntity forge = (AlloyForgeBlockEntity) accessor.getBlockEntity();
		List<ItemStack> inventory = List.of(
			forge.getItem(AlloyForgeBlockEntity.FIRST_INPUT_SLOT),
			forge.getItem(AlloyForgeBlockEntity.FIRST_INPUT_SLOT + 1),
			forge.getItem(AlloyForgeBlockEntity.FIRST_INPUT_SLOT + 2),
			forge.getItem(AlloyForgeBlockEntity.FUEL_SLOT),
			forge.getItem(AlloyForgeBlockEntity.RESULT_SLOT));
		return new Data(forge.cookingTimer(), forge.cookingTotalTime(), forge.isBoosted(), inventory);
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
		return Data.STREAM_CODEC;
	}

	@Override
	public Identifier getUid() {
		return UID;
	}

	/** The forge's slots in order (three inputs, fuel, result) and how far the current alloy is. */
	public record Data(int progress, int total, boolean boosted, List<ItemStack> inventory) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Data::progress,
			ByteBufCodecs.VAR_INT, Data::total,
			ByteBufCodecs.BOOL, Data::boosted,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, Data::inventory,
			Data::new);
	}

	/**
	 * Hides Jade's generic container contents for the forge, since {@link AlloyForgeJadeClient}
	 * already shows the slots laid out like the forge's screen.
	 */
	public static final class HideItemStorage implements IServerExtensionProvider<ItemStack> {
		public static final HideItemStorage INSTANCE = new HideItemStorage();

		private HideItemStorage() {
		}

		@Override
		public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
			return List.of();
		}

		@Override
		public Identifier getUid() {
			return UID;
		}
	}
}
