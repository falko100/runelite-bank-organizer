package com.bankorganizer;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BankSortSolverTest
{
	@Test
	public void testAlreadySorted()
	{
		// Items already in correct position → 0 swaps
		Map<Integer, Integer> current = new HashMap<>();
		current.put(100, 0);
		current.put(200, 1);
		current.put(300, 2);

		Map<Integer, Integer> desired = new HashMap<>();
		desired.put(100, 0);
		desired.put(200, 1);
		desired.put(300, 2);

		List<BankSortSolver.Swap> swaps = BankSortSolver.solve(current, desired);
		assertTrue("Already sorted should need 0 swaps", swaps.isEmpty());
	}

	@Test
	public void testSimpleSwap()
	{
		// Two items need to swap → 1 swap
		Map<Integer, Integer> current = new HashMap<>();
		current.put(100, 0); // item 100 at slot 0
		current.put(200, 1); // item 200 at slot 1

		Map<Integer, Integer> desired = new HashMap<>();
		desired.put(100, 1); // item 100 should be at slot 1
		desired.put(200, 0); // item 200 should be at slot 0

		List<BankSortSolver.Swap> swaps = BankSortSolver.solve(current, desired);
		assertEquals("Simple swap should need 1 swap", 1, swaps.size());
	}

	@Test
	public void testCycleOfThree()
	{
		// Three items in a cycle: A→B→C→A → needs 2 swaps
		Map<Integer, Integer> current = new HashMap<>();
		current.put(100, 0);
		current.put(200, 1);
		current.put(300, 2);

		Map<Integer, Integer> desired = new HashMap<>();
		desired.put(100, 1); // 100 should go to slot 1
		desired.put(200, 2); // 200 should go to slot 2
		desired.put(300, 0); // 300 should go to slot 0

		List<BankSortSolver.Swap> swaps = BankSortSolver.solve(current, desired);
		assertEquals("3-cycle needs 2 swaps", 2, swaps.size());

		// Verify the swaps actually produce the desired result
		Map<Integer, Integer> state = new HashMap<>(current);
		Map<Integer, Integer> slotToItem = new HashMap<>();
		for (Map.Entry<Integer, Integer> e : state.entrySet())
		{
			slotToItem.put(e.getValue(), e.getKey());
		}

		for (BankSortSolver.Swap swap : swaps)
		{
			int itemA = slotToItem.get(swap.fromSlot);
			int itemB = slotToItem.get(swap.toSlot);

			slotToItem.put(swap.fromSlot, itemB);
			slotToItem.put(swap.toSlot, itemA);
			state.put(itemA, swap.toSlot);
			state.put(itemB, swap.fromSlot);
		}

		for (Map.Entry<Integer, Integer> e : desired.entrySet())
		{
			assertEquals("Item " + e.getKey() + " should be at slot " + e.getValue(),
				e.getValue(), state.get(e.getKey()));
		}
	}

	@Test
	public void testReversal()
	{
		// Reverse 4 items: needs 2 swaps (two independent 2-cycles)
		Map<Integer, Integer> current = new HashMap<>();
		current.put(1, 0);
		current.put(2, 1);
		current.put(3, 2);
		current.put(4, 3);

		Map<Integer, Integer> desired = new HashMap<>();
		desired.put(1, 3);
		desired.put(2, 2);
		desired.put(3, 1);
		desired.put(4, 0);

		List<BankSortSolver.Swap> swaps = BankSortSolver.solve(current, desired);
		assertEquals("Reversing 4 items (two 2-cycles) needs 2 swaps", 2, swaps.size());
	}

	@Test
	public void testEmptyInputs()
	{
		List<BankSortSolver.Swap> swaps = BankSortSolver.solve(new HashMap<>(), new HashMap<>());
		assertTrue("Empty inputs should produce no swaps", swaps.isEmpty());
	}
}
