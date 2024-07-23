package com.github.snaphat.jumptosourcediff

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.Keymap

/**
 * Removes conflicting shortcuts between the RiderEditSource and JumpToSourceDiff actions.
 *
 * This is a workaround for issues with the RiderEditSource action shortcut interfering with
 * the JumpToSourceDiff action shortcut, causing the latter to not work for source files.
 * This activity ensures that any overlapping shortcuts between the custom JumpToSourceDiff
 * action and the RiderEditSource action are resolved by removing any matching shortcuts
 * from the RiderEditSource action.
 *
 * @param keymap The keymap from which conflicting shortcuts should be removed.
 */
fun removeConflictingActionShortcuts(keymap: Keymap?)
{
    keymap ?: return // exit early if null keymap

    val actionManager = ActionManager.getInstance() // Get the action manager for the current project

    // Action IDs
    val riderEditSourceActionID = "RiderEditSource"
    val jumpToSourceDiffActionID = "JumpToSourceDiff"

    // Get shortcuts for RiderEditSourceAction
    val riderEditSourceActionShortcuts = actionManager.getAction(riderEditSourceActionID)
                                             ?.shortcutSet
                                             ?.shortcuts
                                             ?.filterIsInstance<KeyboardShortcut>() ?: return

    // Get shortcuts for JumpToSourceDiffAction
    val jumpToSourceDiffActionShortcuts = actionManager.getAction(jumpToSourceDiffActionID)
                                              ?.shortcutSet
                                              ?.shortcuts
                                              ?.filterIsInstance<KeyboardShortcut>() ?: return

    // Iterate over the shortcuts and remove any matching shortcuts from RiderEditSourceAction
    for (riderEditSourceActionShortcut in riderEditSourceActionShortcuts)
        for (jumpToSourceDiffActionShortcut in jumpToSourceDiffActionShortcuts)
            if (riderEditSourceActionShortcut == jumpToSourceDiffActionShortcut)
                keymap.removeShortcut(riderEditSourceActionID, riderEditSourceActionShortcut)
}