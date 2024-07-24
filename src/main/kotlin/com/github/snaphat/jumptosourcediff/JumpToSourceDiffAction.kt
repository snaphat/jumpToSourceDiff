package com.github.snaphat.jumptosourcediff

import com.intellij.diff.actions.impl.OpenInEditorAction
import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.fileEditor.FileEditorWithTextEditors
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.actions.AbstractShowDiffAction
import com.intellij.openapi.vcs.actions.DiffActionExecutor
import com.intellij.openapi.vcs.actions.DiffActionExecutor.CompareToCurrentExecutor
import com.intellij.openapi.vcs.diff.DiffProvider
import com.intellij.openapi.vfs.VirtualFile
/**
 * An [AnAction] to jump between source and diff editors.
 */
class JumpToSourceDiffAction : AbstractShowDiffAction()
{
    /**
     Inherited to avoid [org.jetbrains.annotations.ApiStatus.OverrideOnly] warnings for [OpenInEditorAction.update]
     **/
    private val openInEditorAction = object : OpenInEditorAction()
    {}

    /**
     * Handles the action performed event.
     *
     * This method switches between source/diff editors depending on the active editor type:
     * - main navigates to the closest matching diff editor. If none, acts as [AbstractShowDiffAction]`.
     * - diff acts as [OpenInEditorAction], the action executed by the 'Jump to Source' toolbar button.
     * - others act as [AbstractShowDiffAction].
     *
     * @param e The [AnActionEvent] containing information about the action event.
     */

    override fun actionPerformed(e: AnActionEvent)
    {
        // Get the file editor manager for the current project, current line in the editor, and type of editor
        val editorManager = e.project?.let { FileEditorManagerEx.getInstanceEx(it) } ?: return
        val line          = e.getData(CommonDataKeys.CARET)?.caretModel?.logicalPosition?.line ?: return
        val editorKind    = e.getData(CommonDataKeys.EDITOR)?.editorKind ?: return

        when (editorKind)
        {
            EditorKind.MAIN_EDITOR -> getDiffEditor(editorManager)?.let { focusDiffEditor(editorManager, it, line) }
                                      ?: super.actionPerformed(e)
            EditorKind.DIFF        -> openInEditorAction.actionPerformed(e)
            else                   -> super.actionPerformed(e)
        }
    }

    /**
     * Provides the executor for performing a diff action.
     *
     * This method returns a [DiffActionExecutor] for the given parameters.
     * It acts as [CompareToCurrentExecutor] when performing a diff action.
     *
     * @param diffProvider The DiffProvider used to obtain differences.
     * @param selectedFile The VirtualFile to be compared.
     * @param project The Project within which the action is performed.
     * @param editor The Editor in which the action is invoked, or null if no editor is associated.
     * @return A DiffActionExecutor for performing the diff action.
     */
    override fun getExecutor(diffProvider: DiffProvider,
                             selectedFile: VirtualFile,
                             project: Project,
                             editor: Editor?): DiffActionExecutor =
        CompareToCurrentExecutor(diffProvider, selectedFile, project, editor)

    /**
     * Updates the presentation of the action.
     *
     * This method is called to update the state of the action depending on the active editor type:
     * - main acts as [AbstractShowDiffAction].
     * - diff acts as [OpenInEditorAction], the action executed by the 'Jump to Source' toolbar button.
     * - others act as [AbstractShowDiffAction].
     * @param e The AnActionEvent containing information about the invocation place and data context.
     */
    override fun update(e: AnActionEvent)
    {
        val editorKind = e.getData(CommonDataKeys.EDITOR)?.editorKind ?: return // Get the type of editor
        when (editorKind)
        {
            EditorKind.MAIN_EDITOR -> super.update(e)
            EditorKind.DIFF        -> openInEditorAction.update(e)
            else                   -> super.update(e)
        }
    }

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
