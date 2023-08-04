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
            Map<String, Map<Integer, List<String>>> mergedChanges = mergeOverlappingChanges(allChanges);

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

        // This variable will keep track of the line number relative to the afterContent string
        int lineIndex = 0;

        for (DiffMatchPatch.Diff diff : diffs) {
            int linesInDiff = countNewlines(diff.text);

            if (diff.operation == DiffMatchPatch.Operation.INSERT) {
                int prevLineStart = lineIndex - lineToIncludeAboveChange;
                if (prevLineStart < 0) prevLineStart = 0;

                int nextLineEnd = lineIndex + linesInDiff + lineToIncludeBelowChange - 1;

                List<String> paragraph = new ArrayList<>();
                String[] lines = afterContent.split("\n");
                for (int i = prevLineStart; i <= nextLineEnd && i < lines.length; i++) {
                    paragraph.add(lines[i]);
                }
                changes.put(prevLineStart, paragraph);
            }

            // Only update the line index if the operation is EQUAL or INSERT
            if (diff.operation != DiffMatchPatch.Operation.DELETE) {
                lineIndex += linesInDiff;
            }

            // If the operation is EQUAL or DELETE, we need to update the current line number relative to beforeContent
            if (diff.operation != DiffMatchPatch.Operation.INSERT) {
                currentLineNo += linesInDiff;
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

    private Map<String, Map<Integer, List<String>>> mergeOverlappingChanges(Map<String, Map<Integer, List<String>>> allChanges) {
        Map<String, Map<Integer, List<String>>> mergedChanges = new LinkedHashMap<>();

        for (Map.Entry<String, Map<Integer, List<String>>> fileChanges : allChanges.entrySet()) {
            String fileName = fileChanges.getKey();
            Map<Integer, List<String>> lineChanges = fileChanges.getValue();

            // Sort the changes by line number
            TreeMap<Integer, List<String>> sortedLineChanges = new TreeMap<>(lineChanges);

            Map<Integer, List<String>> mergedLineChanges = new LinkedHashMap<>();
            Integer lastEndLine = null;
            List<String> currentParagraph = null;

            for (Map.Entry<Integer, List<String>> change : sortedLineChanges.entrySet()) {
                Integer startLine = change.getKey();
                List<String> paragraph = change.getValue();
                int endLine = startLine + paragraph.size() - 1;

                // Check if this change overlaps with the previous one
                if (lastEndLine != null && startLine <= lastEndLine + 1) {
                    currentParagraph.addAll(paragraph.subList(lineToIncludeAboveChange, paragraph.size()));
                    lastEndLine = endLine;
                } else {
                    if (currentParagraph != null) {
                        mergedLineChanges.put(lastEndLine - currentParagraph.size() + 1, currentParagraph);
                    }
                    currentParagraph = new ArrayList<>(paragraph);
                    lastEndLine = endLine;
                }
            }

            if (currentParagraph != null) {
                mergedLineChanges.put(lastEndLine - currentParagraph.size() + 1, currentParagraph);
            }

            mergedChanges.put(fileName, mergedLineChanges);
        }

        return mergedChanges;
    }

}
