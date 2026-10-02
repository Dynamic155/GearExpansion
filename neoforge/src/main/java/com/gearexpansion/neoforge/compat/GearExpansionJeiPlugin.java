package com.gearexpansion.neoforge.compat;

import mezz.jei.api.JeiPlugin;

import com.gearexpansion.compat.jei.GearJeiPlugin;

/** Lets JEI find {@link GearJeiPlugin} on NeoForge. Only loaded when JEI is installed. */
@JeiPlugin
public final class GearExpansionJeiPlugin extends GearJeiPlugin {
}
