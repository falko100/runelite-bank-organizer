package com.bankorganizer;

import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * During the DEPOSITING phase, highlights the bank tag tab that the user
 * should deposit items into.
 * <p>
 * Bank tag tabs are rendered by the Bank Tags plugin as child widgets of the
 * bank tab bar container. This overlay draws a pulsing border around the target tab.
 */
public class BankTagTabOverlay extends Overlay
{
	private final Client client;
	private final BankOrganizerPlugin plugin;
	private final BankOrganizerConfig config;

	@Inject
	BankTagTabOverlay(Client client, BankOrganizerPlugin plugin, BankOrganizerConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.organizerEnabled() || !plugin.isBankOpen())
		{
			return null;
		}

		BankOrganizerSession session = plugin.getSession();

		if (session.getState() != OrganizerState.DEPOSITING)
		{
			return null;
		}

		// Highlight the bank tab bar to prompt the user to pick a tag tab.
		// The RuneLite bank tags plugin uses the tab container at ComponentID.BANK_TAB_CONTAINER.
		Widget tabContainer = client.getWidget(ComponentID.BANK_TAB_CONTAINER);
		if (tabContainer == null)
		{
			return null;
		}

		Widget[] tabs = tabContainer.getDynamicChildren();
		if (tabs == null)
		{
			return null;
		}

		Color highlightColor = config.depositHighlightColor();

		// Create a pulsing effect using system time
		float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0));
		int alpha = (int) (highlightColor.getAlpha() * (0.4 + 0.6 * pulse));
		Color pulsingColor = new Color(
			highlightColor.getRed(),
			highlightColor.getGreen(),
			highlightColor.getBlue(),
			alpha
		);

		for (Widget tab : tabs)
		{
			if (tab == null || tab.isHidden())
			{
				continue;
			}

			Rectangle bounds = tab.getBounds();
			if (bounds == null || bounds.width <= 0)
			{
				continue;
			}

			// If a specific tab is the target, only highlight that one.
			// Otherwise highlight all tabs to prompt a selection.
			if (session.getTargetTagTab() != null)
			{
				String tabName = tab.getName();
				if (tabName != null && tabName.equals(session.getTargetTagTab()))
				{
					drawTabHighlight(graphics, bounds, pulsingColor);
				}
			}
			else
			{
				drawTabHighlight(graphics, bounds, pulsingColor);
			}
		}

		// Draw instruction text
		graphics.setFont(graphics.getFont().deriveFont(java.awt.Font.BOLD, 12f));
		String msg = session.getTargetTagTab() != null
			? "Deposit-all into: " + session.getTargetTagTab()
			: "Click a tag tab to set as target";
		int textWidth = graphics.getFontMetrics().stringWidth(msg);

		Rectangle tabBounds = tabContainer.getBounds();
		int x = tabBounds != null ? tabBounds.x : 30;
		int y = tabBounds != null ? tabBounds.y - 5 : 50;

		graphics.setColor(new Color(0, 0, 0, 180));
		graphics.fillRoundRect(x - 2, y - 13, textWidth + 8, 18, 5, 5);
		graphics.setColor(highlightColor);
		graphics.drawString(msg, x + 2, y);

		return null;
	}

	private void drawTabHighlight(Graphics2D g, Rectangle bounds, Color color)
	{
		// Fill with translucent color
		g.setColor(color);
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

		// Draw a bright border
		g.setColor(new Color(
			color.getRed(),
			color.getGreen(),
			color.getBlue(),
			Math.min(255, color.getAlpha() + 80)
		));
		g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
	}
}
