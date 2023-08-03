package com.example.prgpt;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PrGptAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();

        // Get the ChangeListManager instance for the current project
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);

        // Get all local changes
        List<Change> changes = (List<Change>) changeListManager.getAllChanges();

        // Apply uppercase transformation to the changes
        try {
            makeChangesUpperCase(changes, project);
        } catch (VcsException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void makeChangesUpperCase(List<Change> changes, Project project) throws VcsException {
        for (Change change : changes) {
            // Get the 'after' revision
            ContentRevision afterRevision = change.getAfterRevision();
            ContentRevision beforeRevision = change.getBeforeRevision();

            // Get the file path
            com.intellij.openapi.vcs.FilePath filePath = afterRevision.getFile();

            // Check if the file is inside the src folder
            if (!filePath.getPath().contains("/src/")) {
                continue; // Skip if not inside src folder
            }

            // Convert FilePath to VirtualFile
            VirtualFile virtualFile = filePath.getVirtualFile();

            // Read the file content
            String originalContent;
            try {
                originalContent = new String(virtualFile.contentsToByteArray(), virtualFile.getCharset());
            } catch (IOException ex) {
                System.out.println("Error reading file: " + filePath.getPath());
                continue;
            }

            // Get before and after content for the diff
            String beforeContent = beforeRevision != null ? beforeRevision.getContent() : "";
            String afterContent = afterRevision != null ? afterRevision.getContent() : "";

            // Apply uppercase transformation to the changes
            String transformedContent = applyUppercaseToChanges(beforeContent, afterContent);

            // Write the transformed content back to the file
            WriteAction.run(() -> {
                try {
                    virtualFile.setBinaryContent(transformedContent.getBytes(virtualFile.getCharset()));
                } catch (IOException ex) {
                    System.out.println("Error writing to file: " + filePath.getPath());
                }
            });
        }
    }

    private String applyUppercaseToChanges(String beforeContent, String afterContent) {
        DiffMatchPatch dmp = new DiffMatchPatch();
        LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(beforeContent, afterContent);

        StringBuilder modifiedContent = new StringBuilder();
        int currentIndex = 0;

        for (DiffMatchPatch.Diff diff : diffs) {
            switch (diff.operation) {
                case EQUAL:
                    modifiedContent.append(afterContent.substring(currentIndex, currentIndex + diff.text.length()));
                    break;
                case DELETE:
                    break;
                case INSERT:
                    modifiedContent.append(diff.text.toUpperCase());
                    break;
            }
            currentIndex += diff.text.length();
        }

        return modifiedContent.toString();
    }
}
