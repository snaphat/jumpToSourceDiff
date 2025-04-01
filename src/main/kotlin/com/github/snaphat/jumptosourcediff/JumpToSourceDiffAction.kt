package com.github.snaphat.jumptosourcediff

import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.fileEditor.FileEditorWithTextEditors
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx

/**
 * An [AnAction] to jump between source and diff editors.
 */
class JumpToSourceDiffAction : AnAction()
{
    // Retrieves the built-in 'Edit Source' action (typically opens the editor at a selected element)
    private val editSourceAction: AnAction? =
        ActionManager.getInstance().getAction("EditSource")

    // Retrieves the built-in 'Compare with the Same Version' action (used in VCS changes)
    private val compareSameVersionAction: AnAction? =
        ActionManager.getInstance().getAction("Compare.SameVersion")

    /**
     * Handles the action performed event.
     *
     * This method switches between source/diff editors depending on the active editor type:
     * - main navigates to the closest matching diff editor. If none, acts as [compareSameVersionAction]`.
     * - diff acts as [editSourceAction], the action executed by the 'Jump to Source' toolbar button.
     * - others act as [compareSameVersionAction].
     *
     * @param e The [AnActionEvent] containing information about the action event.
     */
    override fun actionPerformed(e: AnActionEvent)
    {
        // Get the file editor manager for the current project, current line in the editor, and type of editor
        val editorManager = e.project?.let { FileEditorManagerEx.getInstanceEx(it) } ?: return
        val line = e.getData(CommonDataKeys.CARET)?.caretModel?.logicalPosition?.line ?: return
        val editorKind = e.getData(CommonDataKeys.EDITOR)?.editorKind ?: return

        when (editorKind)
        {
            EditorKind.MAIN_EDITOR -> getDiffEditor(editorManager)?.let { focusDiffEditor(editorManager, it, line) }
                                      ?: compareSameVersionAction?.actionPerformed(e)
            EditorKind.DIFF        -> editSourceAction?.actionPerformed(e)
            else                   -> compareSameVersionAction?.actionPerformed(e)
        }
    }

    /**
     * Updates the presentation of the action.
     *
     * This method enables and makes the action visible when it is not invoked from an action toolbar.
     * @param e The AnActionEvent containing information about the invocation place and data context.
     */
    override fun update(e: AnActionEvent) =
        e.presentation.run { isEnabledAndVisible = !e.isFromActionToolbar }


    /**
     * Specifies the thread on which the [update] method should be executed.
     *
     * This method returns [ActionUpdateThread.BGT], indicating that the update logic
     * can safely run on a background thread. Use this when the update code does not touch UI
     * components or PSI structures.
     *
     * @return [ActionUpdateThread.BGT] to perform updates off the UI thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    /**
     * Finds the closest diff editor that corresponds to the given source file.
     *
     * This method searches within the currently active editor window first. If no matching editor is found, it then
     * searches through all editors.
     *
     * @param editorManager The [FileEditorManagerEx] instance used to retrieve editors.
     * @return The closest matching [FileEditorWithTextEditors], or null if no matching editor is found.
     */
    private fun getDiffEditor(editorManager: FileEditorManagerEx): FileEditorWithTextEditors? =
        editorManager.currentFile?.let { file ->
            editorManager.currentWindow // Search in the current window's composites
                ?.allComposites?.asSequence()?.flatMap { it.allEditors.asSequence() }
                ?.filterIsInstance<FileEditorWithTextEditors>()
                ?.firstOrNull { it.filesToRefresh.contains(file) }
            ?: editorManager.allEditors // Fallback to search in all editors
                .asSequence()
                .filterIsInstance<FileEditorWithTextEditors>()
                .firstOrNull { it.filesToRefresh.contains(file) }
        }

    /**
     * Focuses the given diff editor and moves the caret to the specified line.
     *
     * @param editorManager The [FileEditorManagerEx] instance used to manage file editors.
     * @param editor The [FileEditorWithTextEditors] to focus.
     * @param line The line number to move the caret to.
     */
    private fun focusDiffEditor(editorManager: FileEditorManagerEx, editor: FileEditorWithTextEditors, line: Int) =
        editor.embeddedEditors.lastOrNull()?.apply {
            editorManager.openFile(editor.file, true) // Focus tab
            DiffUtil.scrollEditor(this, line, false)  // Switch the line number in the embedded editor
            contentComponent.requestFocusInWindow()   // Focus the embedded editor to show the caret
        }
}
