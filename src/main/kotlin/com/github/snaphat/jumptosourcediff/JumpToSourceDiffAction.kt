package com.github.snaphat.jumptosourcediff

import com.intellij.diff.actions.impl.OpenInEditorAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorWithTextEditors
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.actions.AbstractShowDiffAction
import com.intellij.openapi.vcs.actions.DiffActionExecutor
import com.intellij.openapi.vcs.actions.DiffActionExecutor.CompareToCurrentExecutor
import com.intellij.openapi.vcs.diff.DiffProvider
import com.intellij.openapi.vfs.VirtualFile

/**
 * An action to jump between source and diff editors.
 */
class JumpToSourceDiffAction : AbstractShowDiffAction()
{
    /**
     * Handles the action performed event.
     *
     * It checks the type of the current editor and switches between source and diff editors accordingly.
     * - For the main editor, it switches to a diff editor if available. If no diff editor is open, it behaves like `CompareWithTheSameVersionAction`.
     * - For the diff editor, it behaves like `OpenInEditorAction` (this is the action executed by the 'Jump to Source' toolbar button).
     * - For all other editor types, it behaves like `CompareWithTheSameVersionAction`.
     *
     * @param e The AnActionEvent containing information about the action event.
     */
    override fun actionPerformed(e: AnActionEvent)
    {
        val manager    = e.project?.let { FileEditorManagerEx.getInstanceEx(it) } ?: return           // Get the file editor manager for the current project
        val line       = e.getData(CommonDataKeys.CARET)?.caretModel?.logicalPosition?.line ?: return // Get the current line in the editor
        val editorKind = e.getData(CommonDataKeys.EDITOR)?.editorKind ?: return                       // Get the type of editor

        when (editorKind)
        {
            EditorKind.MAIN_EDITOR -> getDiffEditor(manager)?.let { focusDiffEditor(manager, it, line) } // For main editor types, try to switch to a diff editor if available
                                      ?: super.actionPerformed(e)                                        // If no diff editor is open, behave like CompareWithTheSameVersionAction
            EditorKind.DIFF        -> OpenInEditorAction().actionPerformed(e)                            // For diff editor types, behave as OpenInEditorAction would
            else                   -> super.actionPerformed(e)                                           // For all other editor types, behave as CompareWithTheSameVersionAction would
        }
    }

    /**
     * Provides the executor for performing a diff action.
     *
     * This method returns a `DiffActionExecutor` for the given parameters.
     * It behaves like `CompareWithTheSameVersionAction` when performing a diff action.
     *
     * @param diffProvider The DiffProvider used to obtain differences.
     * @param selectedFile The VirtualFile to be compared.
     * @param project The Project within which the action is performed.
     * @param editor The Editor in which the action is invoked, or null if no editor is associated.
     * @return A DiffActionExecutor for performing the diff action.
     */
    override fun getExecutor(diffProvider: DiffProvider, selectedFile: VirtualFile, project: Project, editor: Editor?): DiffActionExecutor =
        CompareToCurrentExecutor(diffProvider, selectedFile, project, editor)

    /**
     * Updates the presentation of the action.
     *
     * This method is called to update the state of the action. It changes the behavior based on the type of editor.
     * For the main editor, it behaves as `CompareWithTheSameVersionAction`.
     * For the diff editor, it behaves as `OpenInEditorAction` (this is the action executed by the 'Jump to Source' toolbar button).
     * For all other editor types, it behaves as `CompareWithTheSameVersionAction`.
     *
     * @param e The AnActionEvent containing information about the invocation place and data context.
     */
    override fun update(e: AnActionEvent)
    {
        val editorKind = e.getData(CommonDataKeys.EDITOR)?.editorKind ?: return // Get the type of editor
        when (editorKind)
        {
            EditorKind.MAIN_EDITOR -> super.update(e)                // For main editor types, behave as CompareWithTheSameVersionAction would
            EditorKind.DIFF        -> OpenInEditorAction().update(e) // For diff editor types, behave as OpenInEditorAction would
            else                   -> super.update(e)                // For all other editor types, behave as CompareWithTheSameVersionAction would
        }
    }

    /**
     * Finds the closest FileEditorWithTextEditors that corresponds to the given source file.
     *
     * This function searches within the currently active editor window first. If no matching editor is found,
     * it then searches through all editors managed by the FileEditorManagerEx.
     *
     * @param manager The FileEditorManagerEx instance used to retrieve editors.
     * @return The closest matching FileEditorWithTextEditors, or null if no matching editor is found.
     */
    private fun getDiffEditor(manager: FileEditorManagerEx): FileEditorWithTextEditors? =
        manager.currentFile?.let { file ->
            manager.currentWindow                                      // Search in the current window's composites
                ?.allComposites?.flatMap { it.allEditors.asSequence() }
                ?.filterIsInstance<FileEditorWithTextEditors>()
                ?.firstOrNull { it.filesToRefresh.firstOrNull() == file }
            ?: manager.allEditors                                      // Fallback to search in all editors managed by FileEditorManagerEx
                .asSequence()
                .filterIsInstance<FileEditorWithTextEditors>()
                .firstOrNull { it.filesToRefresh.firstOrNull() == file }
        }

    /**
     * Focuses the given diff editor and moves the caret to the specified line.
     *
     * @param manager The FileEditorManagerEx instance used to manage file editors.
     * @param editor The FileEditorWithTextEditors to focus.
     * @param line The line number to move the caret to.
     */
    private fun focusDiffEditor(manager: FileEditorManagerEx, editor: FileEditorWithTextEditors, line: Int) =
        editor.embeddedEditors.lastOrNull()?.apply {
            manager.openFile(editor.file, true)                        // Focus tab
            caretModel.removeSecondaryCarets()
            caretModel.moveToLogicalPosition(LogicalPosition(line, 0)) // Switch the line number in the embedded editor
            scrollingModel.scrollToCaret(ScrollType.CENTER)
            selectionModel.removeSelection()
            contentComponent.requestFocusInWindow()                    // Focus the embedded editor to show the caret
        }
}
