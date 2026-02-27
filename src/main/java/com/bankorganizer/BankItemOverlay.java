package com.bankorganizer;

import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Map;

/**
 * Draws highlights on bank items:
 * <ul>
 *   <li>Yellow/configurable overlay on items to withdraw (phase 1)</li>
 *   <li>Storable-elsewhere tint (always, when enabled in config)</li>
 *   <li>Next-swap pair highlight + directional arrows for sorting (phase 3)</li>
 * </ul>
 */
public class BankItemOverlay extends Overlay
{
	private static final Color SWAP_HIGHLIGHT_A = new Color(255, 50, 50, 120);
	private static final Color SWAP_HIGHLIGHT_B = new Color(50, 50, 255, 120);
	private static final Stroke SWAP_BORDER_STROKE = new BasicStroke(2f);

	private final BankOrganizerPlugin plugin;
	private final BankOrganizerConfig config;

	@Inject
	BankItemOverlay(BankOrganizerPlugin plugin, BankOrganizerConfig config)
	{
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

		Widget[] items = plugin.getBankItemWidgets();
		if (items == null)
		{
			return null;
		}

		BankOrganizerSession session = plugin.getSession();
		Map<Integer, Color> storableHighlights = plugin.getStorableHighlights();

		// Determine the next swap pair for highlighting
		BankSortSolver.Swap nextSwap = null;
		if (session.getState() == OrganizerState.SORTING)
		{
			nextSwap = session.getNextSwap();
		}

		for (int slot = 0; slot < items.length; slot++)
		{
			Widget item = items[slot];
			if (item == null || item.isHidden() || item.getItemId() == -1)
			{
				continue;
			}

			int itemId = item.getItemId();
			Rectangle bounds = item.getBounds();
			if (bounds == null || bounds.width <= 0)
			{
				continue;
			}

			// --- Phase 1: Withdraw highlights ---
			if (session.getState() == OrganizerState.WITHDRAWING)
			{
				if (session.getItemsToWithdraw().contains(itemId))
				{
					fillRect(graphics, bounds, config.withdrawHighlightColor());
				}
			}

			// --- Storable-elsewhere highlights (always active when toggled) ---
			Color storableColor = storableHighlights.get(itemId);
			if (storableColor != null)
			{
				fillRect(graphics, bounds, storableColor);
				// Draw a small "S" indicator in the corner
				graphics.setFont(graphics.getFont().deriveFont(9f));
				graphics.setColor(storableColor.brighter());
				graphics.drawString("S", bounds.x + 2, bounds.y + bounds.height - 2);
			}

			// --- Phase 3: Sorting ---
			if (session.getState() == OrganizerState.SORTING)
			{
				// Highlight the next swap pair with distinct colors
				if (nextSwap != null)
				{
					if (slot == nextSwap.fromSlot)
					{
						drawSwapHighlight(graphics, bounds, SWAP_HIGHLIGHT_A, "A");
					}
					else if (slot == nextSwap.toSlot)
					{
						drawSwapHighlight(graphics, bounds, SWAP_HIGHLIGHT_B, "B");
					}
				}

				// Draw directional arrow for items not yet in place
				SortDirection dir = session.getDirectionForItem(itemId, BankOrganizerPlugin.BANK_COLUMNS);
				if (dir != SortDirection.NONE)
				{
					drawArrow(graphics, bounds, dir, config.arrowColor(), config.arrowSize());
				}
			}
		}

		// --- Status bar ---
		drawStatusBar(graphics, session);

		return null;
	}

	private void fillRect(Graphics2D g, Rectangle r, Color c)
	{
		g.setColor(c);
		g.fillRect(r.x, r.y, r.width, r.height);
	}

	private void drawSwapHighlight(Graphics2D g, Rectangle bounds, Color color, String label)
	{
		// Translucent fill
		g.setColor(color);
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

		// Thick border
		Stroke oldStroke = g.getStroke();
		g.setStroke(SWAP_BORDER_STROKE);
		g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 220));
		g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		g.setStroke(oldStroke);

		// Label in corner
		g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
		g.setColor(Color.WHITE);
		g.drawString(label, bounds.x + 3, bounds.y + 12);
	}

	private void drawArrow(Graphics2D g, Rectangle bounds, SortDirection dir, Color color, int size)
	{
		int cx = bounds.x + bounds.width / 2;
		int cy = bounds.y + bounds.height / 2;

		Polygon arrow = new Polygon();

		switch (dir)
		{
			case LEFT:
				arrow.addPoint(cx - size, cy);
				arrow.addPoint(cx + size / 2, cy - size);
				arrow.addPoint(cx + size / 2, cy + size);
				break;
			case RIGHT:
				arrow.addPoint(cx + size, cy);
				arrow.addPoint(cx - size / 2, cy - size);
				arrow.addPoint(cx - size / 2, cy + size);
				break;
			case UP:
				arrow.addPoint(cx, cy - size);
				arrow.addPoint(cx - size, cy + size / 2);
				arrow.addPoint(cx + size, cy + size / 2);
				break;
			case DOWN:
				arrow.addPoint(cx, cy + size);
				arrow.addPoint(cx - size, cy - size / 2);
				arrow.addPoint(cx + size, cy - size / 2);
				break;
			default:
				return;
		}

		g.setColor(color);
		g.fillPolygon(arrow);

		// Outline for contrast
		g.setColor(Color.BLACK);
		g.drawPolygon(arrow);
	}

	private void drawStatusBar(Graphics2D g, BankOrganizerSession session)
	{
		String statusText;
		Color statusColor;

		switch (session.getState())
		{
			case WITHDRAWING:
				statusText = "Step 1/3: Withdraw highlighted items  |  Right-click to mark/unmark";
				statusColor = config.withdrawHighlightColor();
				break;
			case DEPOSITING:
				String tab = session.getTargetTagTab() != null ? session.getTargetTagTab() : "(select tab)";
				statusText = "Step 2/3: Deposit-all into tag tab: " + tab;
				statusColor = config.depositHighlightColor();
				break;
			case SORTING:
				int remaining = session.getSwapsRemaining();
				if (remaining == 0)
				{
					statusText = "Step 3/3: Sorting complete! Right-click \u2192 Done";
					statusColor = new Color(0, 255, 0);
				}
				else
				{
					statusText = "Step 3/3: Swap A \u2194 B  |  " + remaining
						+ " swap" + (remaining != 1 ? "s" : "") + " remaining";
					statusColor = config.arrowColor();
				}
				break;
			default:
				return;
		}

		g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
		int textWidth = g.getFontMetrics().stringWidth(statusText);
		int x = 30;
		int y = 30;

		g.setColor(new Color(0, 0, 0, 180));
		g.fillRoundRect(x - 5, y - 14, textWidth + 10, 20, 5, 5);
		g.setColor(statusColor);
		g.drawString(statusText, x, y);
	}
}
