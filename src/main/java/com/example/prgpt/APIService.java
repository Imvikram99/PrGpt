package com.example.prgpt;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service
@State(name = "APIService", storages = {@Storage("apiKey.xml")})
public final class APIService implements PersistentStateComponent<APIService.State> {
    static class State {
        public String apiKey = "";
    }

    private static APIService instance;
    private State myState = new State();

    public static APIService getInstance(Project project) {
        if(instance == null) {
            ComponentManager componentManager = project;
            instance= componentManager.getService(APIService.class);
        }
        return instance;
       // return ServiceManager.getService(APIService.class);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public String getApiKey() {
        return myState.apiKey;
    }

    public void setApiKey(String apiKey) {
        myState.apiKey = apiKey;
    }
}
