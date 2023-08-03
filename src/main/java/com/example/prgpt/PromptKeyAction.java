package com.example.prgpt;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;

public class PromptKeyAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        APIService apiService = APIService.getInstance(e.getProject());
        String apiKey = apiService.getApiKey();
        if (apiKey==null || apiKey.isEmpty()) {
            apiKey = Messages.showInputDialog(
                    "Please enter your API Key:",
                    "API Key Required",
                    Messages.getQuestionIcon()
            );

            if (apiKey != null && !apiKey.isEmpty()) {
                // Save the key
                apiService.setApiKey(apiKey);
            }
        }
        // Use the apiKey here.
    }
}

