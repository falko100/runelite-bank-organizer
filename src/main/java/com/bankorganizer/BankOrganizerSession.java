package com.bankorganizer;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the mutable state for one organizer session (one "organize run").
 * A session begins when the user clicks "Start Organizing" and ends when they
 * click "Done" or close the bank.
 */
public class BankOrganizerSession
{
	@Getter @Setter
	private OrganizerState state = OrganizerState.IDLE;

	/**
	 * Item IDs the user has selected to withdraw from the main tab.
	 * Preserved across the WITHDRAWING → DEPOSITING transition so the overlay
	 * can keep highlighting them until they are actually withdrawn.
	 */
	@Getter
	private final List<Integer> itemsToWithdraw = new ArrayList<>();

	/**
	 * The tag tab name the user should deposit into (set when entering DEPOSITING).
	 */
	@Getter @Setter
	private String targetTagTab;

	/**
	 * The desired sort order of item IDs inside the target tag tab.
	 * Key = item ID, Value = desired slot index (0-based).
	 * Built when entering SORTING state by reading the layout the user wants.
	 */
	@Getter
	private final Map<Integer, Integer> desiredOrder = new LinkedHashMap<>();

	/**
	 * Current slot positions of items in the tag tab (item ID → current slot index).
	 * Updated every tick while in SORTING state.
	 */
	@Getter
	private final Map<Integer, Integer> currentOrder = new LinkedHashMap<>();

	/**
	 * The computed optimal swap sequence. Recalculated whenever
	 * current positions change during the SORTING phase.
	 */
	@Getter
	private List<BankSortSolver.Swap> swapSequence = Collections.emptyList();

	/**
	 * Index into {@link #swapSequence} pointing at the next swap the user should perform.
	 * Advances as the user completes swaps.
	 */
	@Getter @Setter
	private int swapIndex;

	public void reset()
	{
		state = OrganizerState.IDLE;
		itemsToWithdraw.clear();
		targetTagTab = null;
		desiredOrder.clear();
		currentOrder.clear();
		swapSequence = Collections.emptyList();
		swapIndex = 0;
	}

	/**
	 * Recompute the swap sequence from the current and desired orders.
	 * Should be called after updating currentOrder.
	 */
	public void recomputeSwaps()
	{
		swapSequence = BankSortSolver.solve(currentOrder, desiredOrder);
		swapIndex = 0;
	}

	/**
	 * Returns the next swap the user should perform, or null if sorting is complete.
	 */
	public BankSortSolver.Swap getNextSwap()
	{
		if (swapSequence.isEmpty() || swapIndex >= swapSequence.size())
		{
			return null;
		}
		return swapSequence.get(swapIndex);
	}

	/**
	 * Returns the total number of swaps remaining.
	 */
	public int getSwapsRemaining()
	{
		return Math.max(0, swapSequence.size() - swapIndex);
	}

	/**
	 * Returns the direction an item needs to move to reach its desired position.
	 *
	 * @param itemId the item to check
	 * @param columns number of columns in the bank grid (usually 8)
	 * @return a {@link SortDirection} indicating which way the item should move,
	 *         or {@code NONE} if it is already in the correct place.
	 */
	public SortDirection getDirectionForItem(int itemId, int columns)
	{
		Integer desired = desiredOrder.get(itemId);
		Integer current = currentOrder.get(itemId);
		if (desired == null || current == null || desired.equals(current))
		{
			return SortDirection.NONE;
		}

		int currentRow = current / columns;
		int desiredRow = desired / columns;
		int currentCol = current % columns;
		int desiredCol = desired % columns;

		// Prefer horizontal movement first if on a different column
		if (desiredCol < currentCol)
		{
			return SortDirection.LEFT;
		}
		if (desiredCol > currentCol)
		{
			return SortDirection.RIGHT;
		}
		// Same column, different row
		if (desiredRow < currentRow)
		{
			return SortDirection.UP;
		}
		if (desiredRow > currentRow)
		{
			return SortDirection.DOWN;
		}

		return SortDirection.NONE;
	}
}
