package com.gearexpansion.fabric.test;

import java.util.List;

import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.book.error.BookErrorManager;
import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.button.ArrowButton;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.klikli_dev.modonomicon.item.ModonomiconItem;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/**
 * Checks the optional Modonomicon guidebook. Without Modonomicon, the guide's recipe must not load.
 * With it, the book must load without errors, have an entry for every material, and be craftable,
 * and every page is opened and saved as a screenshot. The Modonomicon code is in a nested class,
 * which only loads when Modonomicon is installed.
 */
final class GuidebookChecks {
	private static final Identifier BOOK = GearExpansion.id("guide");
	private static final ResourceKey<Recipe<?>> RECIPE = ResourceKey.create(Registries.RECIPE, GearExpansion.id("guide_book"));
	// A safety limit on page turns per entry.
	private static final int MAX_SPREADS = 10;

	private GuidebookChecks() {
	}

	static void run(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		boolean installed = FabricLoader.getInstance().isModLoaded("modonomicon");
		boolean recipe = server.computeOnServer(s -> s.getRecipeManager().byKey(RECIPE).isPresent());
		checker.check(recipe == installed, installed
			? "the guidebook recipe loads with Modonomicon"
			: "the guidebook recipe is skipped without Modonomicon");
		if (installed) {
			Modonomicon.run(ctx, server, checker);
		}
	}

	private static final class Modonomicon {
		static void run(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
			// A book and a piece of raw zinc craft the guide.
			boolean crafts = server.computeOnServer(s -> {
				var input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(ModMaterials.ZINC.rawItem.get())));
				return s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, s.overworld())
					.map(holder -> holder.value().assemble(input))
					.map(result -> result.getItem() instanceof ModonomiconItem && BOOK.equals(ModonomiconItem.getBookId(result)))
					.orElse(false);
			});
			checker.check(crafts, "a book and raw zinc craft the Gear Expansion Guide");

			Book book = ctx.computeOnClient(mc -> BookDataManager.get().getBook(BOOK));
			checker.check(book != null, "Modonomicon loads the Gear Expansion Guide");
			if (book == null) {
				return;
			}
			boolean errors = ctx.computeOnClient(mc -> BookErrorManager.get().hasErrors(BOOK));
			if (errors) {
				ctx.runOnClient(mc -> BookErrorManager.get().getErrors(BOOK).getErrors()
					.forEach(error -> GearExpansion.LOGGER.error("[GameTest] Guidebook error: {}", error)));
			}
			checker.check(!errors, "the Gear Expansion Guide has no errors");
			for (MaterialSet set : ModMaterials.ALL) {
				checker.check(book.getEntry(GearExpansion.id("materials/" + set.name)) != null,
					"the guidebook has an entry for " + set.name);
			}

			// Each category's overview, then every two-page spread of every entry. Long text flows
			// onto extra pages, so the pages are turned with the book's own arrow button.
			for (var category : book.getCategoriesSorted()) {
				ctx.runOnClient(mc -> BookGuiManager.get().openEntry(BOOK, category.getId(), null, 0));
				ctx.waitTicks(10);
				ctx.takeScreenshot("guidebook_" + category.getId().getPath());
				for (var entry : category.getEntries().values()) {
					ctx.runOnClient(mc -> BookGuiManager.get().openEntry(BOOK, category.getId(), entry.getId(), 0));
					String name = "guidebook_" + entry.getId().getPath().replace('/', '_');
					for (int spread = 1; spread <= MAX_SPREADS; spread++) {
						ctx.waitTicks(3);
						ctx.takeScreenshot(name + "_" + spread);
						if (!ctx.computeOnClient(mc -> turnPage())) {
							break;
						}
					}
				}
			}
			ctx.runOnClient(mc -> BookGuiManager.get().closeAll());
			ctx.waitTicks(5);
		}

		/** Shows the next two pages of the open entry, if there are any. */
		private static boolean turnPage() {
			BookEntryScreen screen = BookGuiManager.get().openBookEntryScreen;
			if (screen == null || !screen.canSeeArrowButton(false)) {
				return false;
			}
			return screen.children().stream()
				.filter(child -> child instanceof ArrowButton arrow && !arrow.left)
				.findFirst()
				.map(arrow -> {
					screen.handleArrowButton((ArrowButton) arrow);
					return true;
				})
				.orElse(false);
		}
	}
}
