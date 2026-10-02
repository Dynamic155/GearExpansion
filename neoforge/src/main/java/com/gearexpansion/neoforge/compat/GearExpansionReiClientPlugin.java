package com.gearexpansion.neoforge.compat;

import me.shedaniel.rei.forge.REIPluginClient;

import com.gearexpansion.compat.rei.GearReiClientPlugin;

/** Lets REI find {@link GearReiClientPlugin} on NeoForge clients. Only loaded when REI is installed. */
@REIPluginClient
public final class GearExpansionReiClientPlugin extends GearReiClientPlugin {
}
