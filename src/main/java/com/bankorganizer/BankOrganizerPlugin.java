package com.bankorganizer;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "Bank Organizer",
	description = "Guides you through organizing your bank: withdraw → deposit into tag tab → sort with arrows",
	tags = {"bank", "organizer", "sort", "tag"}
)
public class BankOrganizerPlugin extends Plugin
{
	static final int BANK_COLUMNS = 8;

	@Inject
	private Client client;

	@Inject
	private BankOrganizerConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BankItemOverlay bankItemOverlay;

	@Inject
	private BankTagTabOverlay bankTagTabOverlay;

	@Getter
	private final BankOrganizerSession session = new BankOrganizerSession();

	@Getter
	private boolean bankOpen;

	@Provides
	BankOrganizerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankOrganizerConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(bankItemOverlay);
		overlayManager.add(bankTagTabOverlay);
		log.info("Bank Organizer started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(bankItemOverlay);
		overlayManager.remove(bankTagTabOverlay);
		session.reset();
		bankOpen = false;
		log.info("Bank Organizer stopped");
	}

	// ------------------------------------------------------------------
	// Event handlers
	// ------------------------------------------------------------------

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANK)
		{
			bankOpen = true;

			if (session.getState() == OrganizerState.IDLE && config.organizerEnabled())
			{
				// Auto-start a session: scan the main tab for items to organize
				session.setState(OrganizerState.WITHDRAWING);
				rebuildWithdrawList();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			session.reset();
			bankOpen = false;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK.getId())
		{
			if (session.getState() == OrganizerState.WITHDRAWING)
			{
				rebuildWithdrawList();
			}
			else if (session.getState() == OrganizerState.SORTING)
			{
				rebuildCurrentOrder();
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!bankOpen || !config.organizerEnabled())
		{
			return;
		}

		// Add right-click menu entries on bank items during organize workflow
		if (session.getState() == OrganizerState.WITHDRAWING)
		{
			addWithdrawMenuEntries(event);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!config.organizerEnabled())
		{
			return;
		}

		String option = event.getMenuOption();

		if (option == null)
		{
			return;
		}

		// Handle our custom menu entries
		if (option.equals("Organizer: Mark"))
		{
			int itemId = event.getItemId();
			if (itemId != -1 && !session.getItemsToWithdraw().contains(itemId))
			{
				session.getItemsToWithdraw().add(itemId);
			}
			event.consume();
		}
		else if (option.equals("Organizer: Unmark"))
		{
			int itemId = event.getItemId();
			session.getItemsToWithdraw().remove(Integer.valueOf(itemId));
			event.consume();
		}
		else if (option.equals("Organizer: Next Step"))
		{
			advanceState();
			event.consume();
		}
		else if (option.equals("Organizer: Set Target"))
		{
			// Target tab is set via the tag tab overlay click
			session.setTargetTagTab(event.getMenuTarget());
			event.consume();
		}
		else if (option.equals("Organizer: Done"))
		{
			session.reset();
			event.consume();
		}
	}

	// ------------------------------------------------------------------
	// Public helpers used by overlays
	// ------------------------------------------------------------------

	/**
	 * Returns the set of storable-elsewhere item IDs that should be highlighted,
	 * based on which config options are toggled on.
	 */
	public Map<Integer, java.awt.Color> getStorableHighlights()
	{
		Map<Integer, java.awt.Color> highlights = new LinkedHashMap<>();

		if (config.highlightSeedVault())
		{
			for (int id : StorableItems.SEED_VAULT)
			{
				highlights.put(id, config.seedVaultColor());
			}
		}
		if (config.highlightPohStorage())
		{
			for (int id : StorableItems.POH_STORAGE)
			{
				highlights.put(id, config.pohStorageColor());
			}
		}
		if (config.highlightPotionStorage())
		{
			for (int id : StorableItems.POTION_STORAGE)
			{
				highlights.put(id, config.potionStorageColor());
			}
		}
		if (config.highlightFossilStorage())
		{
			for (int id : StorableItems.FOSSIL_STORAGE)
			{
				highlights.put(id, config.fossilStorageColor());
			}
		}

		return highlights;
	}

	/**
	 * Advance the organizer to the next state.
	 */
	public void advanceState()
	{
		switch (session.getState())
		{
			case IDLE:
				session.setState(OrganizerState.WITHDRAWING);
				rebuildWithdrawList();
				break;
			case WITHDRAWING:
				session.setState(OrganizerState.DEPOSITING);
				break;
			case DEPOSITING:
				session.setState(OrganizerState.SORTING);
				rebuildDesiredOrder();
				rebuildCurrentOrder();
				break;
			case SORTING:
				session.reset();
				break;
		}
	}

	/**
	 * Returns the bank item container widget's children, or null if bank isn't open.
	 */
	public Widget[] getBankItemWidgets()
	{
		Widget bankItemContainer = client.getWidget(ComponentID.BANK_ITEM_CONTAINER);
		if (bankItemContainer == null)
		{
			return null;
		}
		return bankItemContainer.getDynamicChildren();
	}

	// ------------------------------------------------------------------
	// Internal helpers
	// ------------------------------------------------------------------

	private void rebuildWithdrawList()
	{
		session.getItemsToWithdraw().clear();

		ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
		if (bankContainer == null)
		{
			return;
		}

		Item[] items = bankContainer.getItems();
		for (Item item : items)
		{
			if (item.getId() != -1)
			{
				session.getItemsToWithdraw().add(item.getId());
			}
		}
	}

	private void rebuildCurrentOrder()
	{
		session.getCurrentOrder().clear();

		Widget[] children = getBankItemWidgets();
		if (children == null)
		{
			return;
		}

		for (int i = 0; i < children.length; i++)
		{
			Widget child = children[i];
			if (child != null && child.getItemId() != -1)
			{
				session.getCurrentOrder().put(child.getItemId(), i);
			}
		}

		// Recompute the optimal swap sequence from current state
		session.recomputeSwaps();
	}

	private void rebuildDesiredOrder()
	{
		// The desired order is the order items appear in the withdraw list.
		// This assumes the user arranged them in the list in the order they want.
		session.getDesiredOrder().clear();
		List<Integer> items = session.getItemsToWithdraw();
		for (int i = 0; i < items.size(); i++)
		{
			session.getDesiredOrder().put(items.get(i), i);
		}
	}

	private void addWithdrawMenuEntries(MenuEntryAdded event)
	{
		int itemId = event.getItemId();
		if (itemId == -1)
		{
			return;
		}

		// Only add entries to bank item widgets
		Widget widget = event.getMenuEntry().getWidget();
		if (widget == null)
		{
			return;
		}

		int parentId = widget.getParentId();
		if (parentId != ComponentID.BANK_ITEM_CONTAINER)
		{
			return;
		}

		boolean isMarked = session.getItemsToWithdraw().contains(itemId);
		String menuOption = isMarked ? "Organizer: Unmark" : "Organizer: Mark";

		client.createMenuEntry(-1)
			.setOption(menuOption)
			.setTarget(event.getMenuEntry().getTarget())
			.setType(MenuAction.RUNELITE)
			.setItemId(itemId)
			.setWidget(widget);
	}
}
