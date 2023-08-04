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
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        List<Change> changes = (List<Change>) changeListManager.getAllChanges();
        try {
           DiffFinderService diffFinderService = new DiffFinderService();
           Map<String, Map<Integer, List<String>>> mergedChanges = diffFinderService.findChangesInFiles(changes,project);
            String result = OpenAiAdapter.generate(mergedChanges,project);
        } catch (VcsException ex) {
            throw new RuntimeException(ex);
        }
    }















}
