package com.github.snaphat.jumptosourcediff

import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * A post startup activity to manage and modify shortcuts for the JumpToSourceDiff action.
 */
class PostStartupProjectActivity : ProjectActivity
{
    /**
     * Executes the post startup activity, removing any conflicting shortcuts.
     *
     * @param project The project for which the activity is executed.
     */
    override suspend fun execute(project: Project) =
        removeConflictingActionShortcuts(KeymapManager.getInstance().activeKeymap)
}