package com.example.prgpt;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import java.io.IOException;
import java.util.*;

public class PrGptAction extends AnAction {
    private static Integer lineToIncludeAboveChange = 3;
    private static Integer lineToIncludeBelowChange = 2;

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        List<Change> changes = (List<Change>) changeListManager.getAllChanges();

        try {
            Map<String, Map<Integer, List<String>>> allChanges = getListOfChangedCodePara(changes, project);
            // Additional code...
            int k = 0;
        } catch (VcsException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Map<String, Map<Integer, List<String>>> getListOfChangedCodePara(List<Change> changes, Project project) throws VcsException {
        Map<String, Map<Integer, List<String>>> fileNameToChanges = new LinkedHashMap<>();
        for (Change change : changes) {
            ContentRevision afterRevision = change.getAfterRevision();
            ContentRevision beforeRevision = change.getBeforeRevision();
            com.intellij.openapi.vcs.FilePath filePath = afterRevision.getFile();

            if (!filePath.getPath().contains("/src/")) continue;

            VirtualFile virtualFile = filePath.getVirtualFile();
            String originalContent;

            try {
                originalContent = new String(virtualFile.contentsToByteArray(), virtualFile.getCharset());
            } catch (IOException ex) {
                System.out.println("Error reading file: " + filePath.getPath());
                continue;
            }

            String beforeContent = beforeRevision != null ? beforeRevision.getContent() : "";
            String afterContent = afterRevision != null ? afterRevision.getContent() : "";
            LinkedHashMap<Integer, List<String>> listOfChangesLineWise = getListOfChanges(beforeContent, afterContent);

            fileNameToChanges.put(filePath.getName(), listOfChangesLineWise);
        }

        return fileNameToChanges;
    }

    private LinkedHashMap<Integer, List<String>> getListOfChanges(String beforeContent, String afterContent) {
        DiffMatchPatch dmp = new DiffMatchPatch();
        LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(beforeContent, afterContent);
        LinkedHashMap<Integer, List<String>> changes = new LinkedHashMap<>();
        int currentLineNo = 0;
        String[] lines = afterContent.split("\n");

        for (DiffMatchPatch.Diff diff : diffs) {
            if (diff.operation == DiffMatchPatch.Operation.INSERT) {
                int lineEnd = currentLineNo;
                int prevLineStart = lineEnd - lineToIncludeAboveChange;
                if (prevLineStart < 0) prevLineStart = 0;
                int changeStartLineNo = lineEnd - lineToIncludeAboveChange;
                if (changeStartLineNo < 0) changeStartLineNo = 0;
                int nextLineEnd = lineEnd + lineToIncludeBelowChange;
                List<String> paragraph = new ArrayList<>();
                for (int i = prevLineStart; i <= nextLineEnd && i < lines.length; i++) {
                    paragraph.add(lines[i]);
                }
                changes.put(changeStartLineNo, paragraph);
                currentLineNo += countNewlines(diff.text);
            } else if (diff.operation == DiffMatchPatch.Operation.EQUAL) {
                currentLineNo += countNewlines(diff.text);
            }
        }

        return changes;
    }

    private int countNewlines(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                count++;
            }
        }
        return count;
    }
}
