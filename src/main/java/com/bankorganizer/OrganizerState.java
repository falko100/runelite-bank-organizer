package com.bankorganizer;

/**
 * Tracks which phase of the bank organizer workflow the user is in.
 */
public enum OrganizerState
{
	/** Not actively organizing – plugin is idle. */
	IDLE,
	/** Phase 1: Items in the main tab are highlighted for withdrawal. */
	WITHDRAWING,
	/** Phase 2: A target bank tag tab is highlighted for deposit-all. */
	DEPOSITING,
	/** Phase 3: Items inside the tag tab show sorting arrows. */
	SORTING
}
