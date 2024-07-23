package com.github.snaphat.jumptosourcediff

import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManagerListener

/**
 * A listener for keymap changes to manage and modify shortcuts for the JumpToSourceDiff action.
 */
class KeymapManagerListener : KeymapManagerListener
{
    /**
     * Called when the active keymap changes, removing any conflicting shortcuts.
     *
     * @param keymap The new active keymap.
     */
    override fun activeKeymapChanged(keymap: Keymap?) =
        removeConflictingActionShortcuts(keymap)
}