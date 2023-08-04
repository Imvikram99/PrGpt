package com.example.prgpt;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import java.io.IOException;
import java.util.*;

public class DiffFinderService {
    private static Integer lineToIncludeAboveChange = 3;
    private static Integer lineToIncludeBelowChange = 2;

    private void printMergedChanges(Map<String, Map<Integer, List<String>>> mergedChanges, String whatAreWePrinting) {
        System.out.println(whatAreWePrinting);
        for (Map.Entry<String, Map<Integer, List<String>>> fileEntry : mergedChanges.entrySet()) {
            String fileName = fileEntry.getKey();
            System.out.println("File Name: " + fileName);

            Map<Integer, List<String>> lineChanges = fileEntry.getValue();
            for (Map.Entry<Integer, List<String>> lineEntry : lineChanges.entrySet()) {
                Integer lineNo = lineEntry.getKey();
                List<String> changes = lineEntry.getValue();
                System.out.println("  Line No: " + lineNo);

                for (String change : changes) {
                    System.out.println("    Change: " + change);
                }
            }
        }
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

                if (lastEndLine != null && startLine <= lastEndLine + 1) {
                    int overlap = (lastEndLine + 1) - startLine;
                    int startIndex = overlap > 0 ? overlap : 0;

                    currentParagraph.addAll(paragraph.subList(startIndex, paragraph.size()));
                    lastEndLine = endLine;
                } else {
                    if (currentParagraph != null) {
                        mergedLineChanges.put(lastEndLine - currentParagraph.size() + 1, currentParagraph);
                    }
                    currentParagraph = new ArrayList<>(paragraph);
                    lastEndLine = endLine;
                }
            }

            // Add the last collected paragraph, if any
            if (currentParagraph != null) {
                mergedLineChanges.put(lastEndLine - currentParagraph.size() + 1, currentParagraph);
            }

            mergedChanges.put(fileName, mergedLineChanges);
        }

        return mergedChanges;
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

    public Map<String, Map<Integer, List<String>>> findChangesInFiles(List<Change> changes, Project project) throws VcsException {
        Map<String, Map<Integer, List<String>>> allChanges = getListOfChangedCodePara(changes, project);
        Map<String, Map<Integer, List<String>>> mergedChanges = mergeOverlappingChanges(allChanges);
        printMergedChanges(allChanges,"allchanges");
        printMergedChanges(mergedChanges,"mergedChanges");
        return mergedChanges;
    }

}
