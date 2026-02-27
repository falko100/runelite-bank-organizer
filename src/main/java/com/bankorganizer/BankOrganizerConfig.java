package com.bankorganizer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Alpha;

import java.awt.Color;

@ConfigGroup("bankorganizer")
public interface BankOrganizerConfig extends Config
{
	// --- Sections ---

	@ConfigSection(
		name = "Highlight Colors",
		description = "Configure highlight colors",
		position = 0
	)
	String colorSection = "colorSection";

	@ConfigSection(
		name = "Storable Elsewhere",
		description = "Highlight items that can be stored outside the bank",
		position = 1
	)
	String storableSection = "storableSection";

	@ConfigSection(
		name = "Sorting",
		description = "Configure sorting arrow display",
		position = 2
	)
	String sortingSection = "sortingSection";

	// --- General ---

	@ConfigItem(
		keyName = "organizerEnabled",
		name = "Enable Organizer",
		description = "Master toggle for the bank organizer plugin",
		position = 0
	)
	default boolean organizerEnabled()
	{
		return true;
	}

	// --- Highlight Colors ---

	@Alpha
	@ConfigItem(
		keyName = "withdrawHighlightColor",
		name = "Withdraw Highlight",
		description = "Color to highlight items in main tab that need to be withdrawn",
		position = 1,
		section = colorSection
	)
	default Color withdrawHighlightColor()
	{
		return new Color(255, 255, 0, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "depositHighlightColor",
		name = "Deposit Tab Highlight",
		description = "Color to highlight the target bank tag tab for depositing",
		position = 2,
		section = colorSection
	)
	default Color depositHighlightColor()
	{
		return new Color(0, 255, 0, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "arrowColor",
		name = "Sort Arrow Color",
		description = "Color for sorting direction arrows",
		position = 3,
		section = colorSection
	)
	default Color arrowColor()
	{
		return new Color(255, 100, 0, 200);
	}

	// --- Storable Elsewhere ---

	@ConfigItem(
		keyName = "highlightSeedVault",
		name = "Seed Vault",
		description = "Highlight items storable in the Farming Guild seed vault",
		position = 0,
		section = storableSection
	)
	default boolean highlightSeedVault()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "seedVaultColor",
		name = "Seed Vault Color",
		description = "Highlight color for seed vault storable items",
		position = 1,
		section = storableSection
	)
	default Color seedVaultColor()
	{
		return new Color(0, 200, 0, 80);
	}

	@ConfigItem(
		keyName = "highlightPohStorage",
		name = "POH Storage",
		description = "Highlight items storable in your Player-Owned House costume room / storage",
		position = 2,
		section = storableSection
	)
	default boolean highlightPohStorage()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "pohStorageColor",
		name = "POH Storage Color",
		description = "Highlight color for POH storable items",
		position = 3,
		section = storableSection
	)
	default Color pohStorageColor()
	{
		return new Color(0, 100, 255, 80);
	}

	@ConfigItem(
		keyName = "highlightPotionStorage",
		name = "Potion Storage",
		description = "Highlight items storable in the potion storage from Nex reward",
		position = 4,
		section = storableSection
	)
	default boolean highlightPotionStorage()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "potionStorageColor",
		name = "Potion Storage Color",
		description = "Highlight color for potion storage storable items",
		position = 5,
		section = storableSection
	)
	default Color potionStorageColor()
	{
		return new Color(200, 0, 200, 80);
	}

	@ConfigItem(
		keyName = "highlightFossilStorage",
		name = "Fossil Storage",
		description = "Highlight items storable in the Varrock Museum fossil storage",
		position = 6,
		section = storableSection
	)
	default boolean highlightFossilStorage()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "fossilStorageColor",
		name = "Fossil Storage Color",
		description = "Highlight color for fossil storage storable items",
		position = 7,
		section = storableSection
	)
	default Color fossilStorageColor()
	{
		return new Color(150, 100, 50, 80);
	}

	// --- Sorting ---

	@ConfigItem(
		keyName = "arrowSize",
		name = "Arrow Size",
		description = "Size of the sorting direction arrows in pixels",
		position = 0,
		section = sortingSection
	)
	default int arrowSize()
	{
		return 10;
	}
}
