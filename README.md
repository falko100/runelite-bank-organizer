# Bank Organizer

A [RuneLite](https://runelite.net/) plugin that walks you through reorganizing your Old School RuneScape bank step by step. Instead of manually figuring out which items go where, the plugin highlights what to withdraw, where to deposit, and exactly which swaps to make — using the mathematically fewest drag-and-drops possible.

## How It Works

The organizer runs as a guided **three-step workflow** that appears automatically when you open your bank (toggleable via config).

### Step 1 — Withdraw

Items in the main bank tab are highlighted with a configurable color overlay. Right-click any item to **Mark** or **Unmark** it. Marked items are the ones you should withdraw into your inventory.

```
+--------+--------+--------+
| [GLOW] | [GLOW] |        |   Yellow highlight = withdraw this
| Rune   | Dragon |  Coins |
| plate  | scim   |        |
+--------+--------+--------+
```

A **status bar** at the top of the screen reads:
> Step 1/3: Withdraw highlighted items | Right-click to mark/unmark

### Step 2 — Deposit into Tag Tab

After advancing to step 2, all bank tag tabs pulse with a green highlight. Click one to set it as the target. The status bar updates to show the selected tab name. Use the in-game **Deposit-all** button to move your inventory into that tag tab.

```
 ╔══════════╗  ╔══════════╗
 ║ Melee    ║  ║ Range    ║   <-- pulsing green highlight
 ╚══════════╝  ╚══════════╝
```

### Step 3 — Sort with Arrows

The plugin computes the **optimal swap sequence** and shows you exactly what to do:

- The next two items to swap are highlighted as **A** (red) and **B** (blue) with thick borders.
- Every out-of-place item gets a **directional arrow** (left, right, up, or down) pointing toward its target slot.
- The status bar shows how many swaps remain: `Step 3/3: Swap A ↔ B | 4 swaps remaining`
- When every item is in place, the status turns green: `Sorting complete! Right-click → Done`

```
+--------+--------+--------+--------+
|  [A]   |        |  [B]   |        |
|  ←──   |   ✓    |  ──→   |   ↑    |
| Rune   | Coins  | Dragon | Arrows |
+--------+--------+--------+--------+
```

## Smart Sort Algorithm

Sorting a bank tab in OSRS is done by dragging one item onto another, which **swaps** their positions. The plugin uses **permutation cycle decomposition** to calculate the absolute minimum number of swaps needed:

- A cycle of length _k_ in the permutation requires exactly _k − 1_ swaps.
- For example, if items need to rotate A → B → C → A, that's a 3-cycle needing 2 swaps — not 3.
- The solver recomputes after every bank change, so it always shows the optimal next move even if you make a different swap than suggested.

This is implemented in [`BankSortSolver.java`](src/main/java/com/bankorganizer/BankSortSolver.java) and covered by unit tests in [`BankSortSolverTest.java`](src/test/java/com/bankorganizer/BankSortSolverTest.java).

## Storable Elsewhere Highlights

Independently from the organize workflow, the plugin can highlight items that don't need to live in your bank at all because they can be stored in dedicated in-game storage. Each category is a separate toggle with its own configurable color:

| Storage | Examples | Default Color |
|---------|----------|---------------|
| **Seed Vault** | All seeds, saplings, spores (Farming Guild) | Green |
| **POH Storage** | Fire cape, diary gear, holiday items, clue uniques | Blue |
| **Potion Storage** | All standard & divine potions, all dose variants | Purple |
| **Fossil Storage** | All fossil types including unidentified (Varrock Museum) | Brown |

Items from these sets get a translucent tint plus a small **"S"** indicator in the bottom-left corner. This works at all times while the bank is open, regardless of which organize step you're on.

## Configuration

All settings are in the RuneLite config panel under **Bank Organizer**:

### General
| Setting | Description | Default |
|---------|-------------|---------|
| Enable Organizer | Master toggle for the entire plugin | On |

### Highlight Colors
| Setting | Description | Default |
|---------|-------------|---------|
| Withdraw Highlight | Color overlay on items to withdraw (step 1) | Yellow, 40% opacity |
| Deposit Tab Highlight | Color for pulsing tag tab highlight (step 2) | Green, 40% opacity |
| Sort Arrow Color | Color for directional arrows and status bar (step 3) | Orange, 80% opacity |

### Storable Elsewhere
| Setting | Description | Default |
|---------|-------------|---------|
| Seed Vault | Highlight seed vault items | Off |
| Seed Vault Color | Tint color for seed vault items | Green, 30% opacity |
| POH Storage | Highlight POH-storable items | Off |
| POH Storage Color | Tint color for POH items | Blue, 30% opacity |
| Potion Storage | Highlight potion storage items | Off |
| Potion Storage Color | Tint color for potion items | Purple, 30% opacity |
| Fossil Storage | Highlight fossil storage items | Off |
| Fossil Storage Color | Tint color for fossils | Brown, 30% opacity |

### Sorting
| Setting | Description | Default |
|---------|-------------|---------|
| Arrow Size | Size of directional arrows in pixels | 10 |

## Right-Click Menu Entries

The plugin adds context menu entries to bank items during the workflow:

| Menu Option | When | What It Does |
|-------------|------|--------------|
| `Organizer: Mark` | Step 1, on unmarked items | Adds the item to the withdraw list |
| `Organizer: Unmark` | Step 1, on marked items | Removes the item from the withdraw list |
| `Organizer: Next Step` | Any step | Advances to the next phase |
| `Organizer: Set Target` | Step 2, on tag tabs | Sets the clicked tab as the deposit target |
| `Organizer: Done` | Step 3 | Ends the session and clears all overlays |

## Project Structure

```
src/main/java/com/bankorganizer/
├── BankOrganizerPlugin.java    # Main plugin class — lifecycle, events, state transitions
├── BankOrganizerConfig.java    # All config options (colors, toggles, arrow size)
├── BankOrganizerSession.java   # Mutable session state — items, orders, swap tracking
├── BankSortSolver.java         # Optimal swap solver via cycle decomposition
├── BankItemOverlay.java        # Renders item highlights, arrows, swap pair labels, status bar
├── BankTagTabOverlay.java      # Renders pulsing tab highlights during deposit phase
├── StorableItems.java          # Curated item ID sets (seed vault, POH, potions, fossils)
├── OrganizerState.java         # Enum: IDLE, WITHDRAWING, DEPOSITING, SORTING
└── SortDirection.java          # Enum: NONE, LEFT, RIGHT, UP, DOWN

src/test/java/com/bankorganizer/
└── BankSortSolverTest.java     # Unit tests for the sort algorithm
```

## Building

Requires **Java 11+** and uses the RuneLite plugin conventions.

```bash
./gradlew build
```

The built JAR will be in `build/libs/`. To use it, place the JAR in your RuneLite external plugins directory or load it via the RuneLite developer tools.

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| RuneLite Client | 1.10.44 | Game client API |
| Lombok | 1.18.30 | Boilerplate reduction |
| PF4J | 3.10.0 | Plugin framework |
| JUnit 4 | 4.13.2 | Unit testing |
| Guava (`ImmutableSet`) | (via RuneLite) | Immutable collections for item ID sets |

## License

This plugin is open source. See the repository for license details.
