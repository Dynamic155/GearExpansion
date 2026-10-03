package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.gearexpansion.network.GearHudPayload;

/** The set bonus for every material, plus the hooks that drive them. */
public final class SetBonuses {
	public static final SetBonus ZINC = new ZincSetBonus();
	public static final VerdigrisSetBonus VERDIGRIS = new VerdigrisSetBonus();
	public static final SetBonus ROSE_GOLD = new RoseGoldSetBonus();
	public static final SetBonus ALUMINUM = new AluminumSetBonus();
	public static final SetBonus BRASS = new BrassSetBonus();
	public static final SetBonus SILVER = new SilverSetBonus();
	public static final SetBonus EMERALD = new EmeraldSetBonus();
	public static final SetBonus AMETHYST = new AmethystSetBonus();
	public static final SetBonus SAKURA = new SakuraSetBonus();
	public static final SetBonus STEEL = new SteelSetBonus();
	public static final SetBonus TITANIUM = new TitaniumSetBonus();
	public static final CobaltSetBonus COBALT = new CobaltSetBonus();
	public static final SetBonus TUNGSTEN = new TungstenSetBonus();
	public static final ObsidianSetBonus OBSIDIAN = new ObsidianSetBonus();
	public static final SetBonus PRISMARINE = new PrismarineSetBonus();
	public static final EchoSetBonus ECHO = new EchoSetBonus();
	public static final SetBonus INFERNIUM = new InferniumSetBonus();

	public static final List<SetBonus> ALL = List.of(ZINC, VERDIGRIS, ROSE_GOLD, ALUMINUM, BRASS, SILVER, EMERALD, AMETHYST, SAKURA, STEEL,
		TITANIUM, COBALT, TUNGSTEN, OBSIDIAN, PRISMARINE, ECHO, INFERNIUM);

	/** The HUD meters last sent to each player, so unchanged values aren't resent. */
	private static final Map<UUID, GearHudPayload> SENT_HUD = new WeakHashMap<>();

	private SetBonuses() {
	}

	public static void init() {
		TickEvent.PLAYER_POST.register(player -> {
			if (player instanceof ServerPlayer serverPlayer) {
				GearHudPayload.Builder hud = new GearHudPayload.Builder();
				for (SetBonus bonus : ALL) {
					boolean active = bonus.isActive(serverPlayer);
					if (active) {
						bonus.tick(serverPlayer);
						bonus.fillHud(serverPlayer, hud);
					}
					syncAttributes(serverPlayer, bonus, active);
				}
				sendHud(serverPlayer, hud.build());
			}
		});
	}

	/** The Set Ability key was pressed: use the ability of whichever full set the player wears. */
	public static void useAbility(ServerPlayer player) {
		for (SetBonus bonus : ALL) {
			if (bonus.hasAbility() && bonus.isActive(player)) {
				bonus.useAbility(player);
				return;
			}
		}
		player.sendSystemMessage(Component.translatable("ability.gearexpansion.none").withStyle(ChatFormatting.GRAY), true);
	}

	private static void sendHud(ServerPlayer player, GearHudPayload hud) {
		if (!hud.equals(SENT_HUD.get(player.getUUID())) && NetworkManager.canPlayerReceive(player, GearHudPayload.TYPE)) {
			SENT_HUD.put(player.getUUID(), hud);
			NetworkManager.sendToPlayer(player, hud);
		}
	}

	/** After {@code player} breaks a block, for set bonuses that react to mining. */
	public static void onBlockBroken(ServerPlayer player, BlockState state, BlockPos pos) {
		for (SetBonus bonus : ALL) {
			if (bonus.isActive(player)) {
				bonus.onBlockBroken(player, state, pos);
			}
		}
	}

	/** Called from the durability mixin whenever an item held or worn by {@code wearer} is about to lose durability. */
	public static int modifyDurabilityLoss(int amount, LivingEntity wearer, ServerLevel level) {
		if (amount <= 0 || wearer == null) {
			return amount;
		}
		for (SetBonus bonus : ALL) {
			if (bonus.isActive(wearer)) {
				amount = bonus.modifyDurabilityLoss(amount, wearer, level);
			}
		}
		return amount;
	}

	/** Called from the effect mixin whenever a potion effect is about to be applied to {@code entity}. */
	public static MobEffectInstance modifyNewEffect(MobEffectInstance effect, LivingEntity entity) {
		for (SetBonus bonus : ALL) {
			if (bonus.isActive(entity)) {
				effect = bonus.modifyNewEffect(effect, entity);
			}
		}
		return effect;
	}

	/** Called from the experience orb mixin when {@code player} picks up an orb worth {@code amount}. */
	public static int modifyExperience(int amount, Player player) {
		for (SetBonus bonus : ALL) {
			if (bonus.isActive(player)) {
				amount = bonus.modifyExperience(amount, player);
			}
		}
		return amount;
	}

	/** Extra bookshelves an enchanting table counts for {@code player}, from their set bonuses. */
	public static int extraEnchantingBookshelves(Player player) {
		int extra = 0;
		for (SetBonus bonus : ALL) {
			if (bonus.isActive(player)) {
				extra += bonus.extraEnchantingBookshelves(player);
			}
		}
		return extra;
	}

	/** Adds a bonus's attribute modifiers while it's active and removes them once it isn't. */
	private static void syncAttributes(ServerPlayer player, SetBonus bonus, boolean active) {
		for (SetBonus.AttributeBonus attribute : bonus.attributeBonuses(player)) {
			AttributeInstance instance = player.getAttribute(attribute.attribute());
			if (instance == null) {
				continue;
			}
			AttributeModifier current = instance.getModifier(attribute.id());
			if (active && attribute.amount() != 0) {
				if (current == null || current.amount() != attribute.amount()) {
					instance.addOrUpdateTransientModifier(attribute.modifier());
				}
			} else if (current != null) {
				instance.removeModifier(attribute.id());
			}
		}
	}

	public static Optional<SetBonus> forPiece(ItemStack stack) {
		return ALL.stream().filter(bonus -> bonus.isPiece(stack)).findFirst();
	}
}
