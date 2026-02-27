package com.bankorganizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the minimum-swap sequence to transform the current item order
 * into the desired order. Uses cycle decomposition on the permutation:
 * a cycle of length k requires exactly (k-1) swaps.
 *
 * <p>In OSRS, sorting a bank tab is done by dragging one item onto another,
 * which swaps their positions. This solver finds the fewest swaps needed
 * and returns them in a practical order (the first swap in the list should
 * be performed first).</p>
 */
public final class BankSortSolver
{
	private BankSortSolver() {}

	/**
	 * A single swap instruction: swap the item at slot {@code fromSlot}
	 * with the item at slot {@code toSlot}.
	 */
	public static class Swap
	{
		public final int fromSlot;
		public final int toSlot;
		public final int fromItemId;
		public final int toItemId;

		Swap(int fromSlot, int toSlot, int fromItemId, int toItemId)
		{
			this.fromSlot = fromSlot;
			this.toSlot = toSlot;
			this.fromItemId = fromItemId;
			this.toItemId = toItemId;
		}

		@Override
		public String toString()
		{
			return "Swap(slot " + fromSlot + " <-> slot " + toSlot + ")";
		}
	}

	/**
	 * Given the current layout and a desired layout, compute the minimum
	 * sequence of swaps to transform current into desired.
	 *
	 * @param currentSlots maps item ID → current slot index
	 * @param desiredSlots maps item ID → desired slot index
	 * @return ordered list of swaps; performing them in sequence sorts the tab
	 */
	public static List<Swap> solve(Map<Integer, Integer> currentSlots, Map<Integer, Integer> desiredSlots)
	{
		List<Swap> swaps = new ArrayList<>();

		if (currentSlots.isEmpty() || desiredSlots.isEmpty())
		{
			return swaps;
		}

		// Build a working copy: slot → itemId for current state
		Map<Integer, Integer> slotToItem = new HashMap<>();
		Map<Integer, Integer> itemToSlot = new HashMap<>();

		for (Map.Entry<Integer, Integer> entry : currentSlots.entrySet())
		{
			int itemId = entry.getKey();
			int slot = entry.getValue();
			slotToItem.put(slot, itemId);
			itemToSlot.put(itemId, slot);
		}

		// For each item, determine which slot it should go to
		// Build the permutation: for each slot, what slot should its current item go to?
		// perm[currentSlot] = desiredSlot for the item currently at currentSlot
		Map<Integer, Integer> perm = new HashMap<>();
		for (Map.Entry<Integer, Integer> entry : desiredSlots.entrySet())
		{
			int itemId = entry.getKey();
			int desiredSlot = entry.getValue();
			Integer currentSlot = itemToSlot.get(itemId);
			if (currentSlot != null)
			{
				perm.put(currentSlot, desiredSlot);
			}
		}

		// Cycle decomposition of the permutation
		boolean[] visited = new boolean[getMaxSlot(perm) + 1];

		for (int startSlot : perm.keySet())
		{
			if (startSlot >= visited.length || visited[startSlot])
			{
				continue;
			}

			// Trace the cycle starting at startSlot
			List<Integer> cycle = new ArrayList<>();
			int current = startSlot;
			while (!visited[current])
			{
				visited[current] = true;
				cycle.add(current);
				Integer next = perm.get(current);
				if (next == null || next == current)
				{
					break;
				}
				current = next;
			}

			// A cycle of length k needs (k-1) swaps.
			// We swap the first element of the cycle into position by swapping
			// it with each subsequent element in reverse order.
			if (cycle.size() <= 1)
			{
				continue;
			}

			for (int i = cycle.size() - 1; i >= 1; i--)
			{
				int slotA = cycle.get(0);
				int slotB = cycle.get(i);

				int itemA = slotToItem.getOrDefault(slotA, -1);
				int itemB = slotToItem.getOrDefault(slotB, -1);

				swaps.add(new Swap(slotA, slotB, itemA, itemB));

				// Update our working state
				slotToItem.put(slotA, itemB);
				slotToItem.put(slotB, itemA);
				itemToSlot.put(itemA, slotB);
				itemToSlot.put(itemB, slotA);
			}
		}

		return swaps;
	}

	private static int getMaxSlot(Map<Integer, Integer> perm)
	{
		int max = 0;
		for (Map.Entry<Integer, Integer> entry : perm.entrySet())
		{
			max = Math.max(max, entry.getKey());
			max = Math.max(max, entry.getValue());
		}
		return max;
	}
}
