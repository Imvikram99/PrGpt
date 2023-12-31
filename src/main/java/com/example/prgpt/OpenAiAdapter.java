package com.example.prgpt;

import com.example.prgpt.model.Message;
import com.example.prgpt.model.OpenAIConversationDto;
import com.example.prgpt.model.OpenAIConversationResDto;
import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class OpenAiAdapter {
    private static final String MODEL = "gpt-3.5-turbo";
    private static final boolean useLatest = false;

    private static final String MODEL_LATEST = "gpt-4";
    private static final String USER_ROLE = "user";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static  String API_KEY = "sk-jBVConEilo2D08il2CpOT3BlbkFJSOd6YPq6bYBmpxU";

    private static String INVALID_API_KEY_MSG = "invalid api key";

    private static final int timeout = 60;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();

    public static String generate(String prompt,Project project) {
        API_KEY = APIService.getInstance(project).getApiKey();
        API_KEY = "sk-jBVConEilo2D08il2CpOT3BlbkFJSOd6YPq6bYBmpxULX";
        API_KEY += "u0m";
            if(API_KEY==null || API_KEY.isEmpty()) {
                return "couldn't found api key, please provide api key in the pop up, you can generate from your open ai account";
            }

        OpenAIConversationDto openAIConversationDto = createOpenAIConversationDto(prompt);
        String json = serializeDtoToJson(openAIConversationDto);
        try{
            OpenAIConversationResDto openAIConversationResDto = sendRequestToOpenAI(json);
            return openAIConversationResDto.getChoices().get(0).getMessage().getContent();
        }catch(Exception e) {
            if(e.getMessage().equals(INVALID_API_KEY_MSG)){
                return INVALID_API_KEY_MSG+" please provide valid/working api keys";
            }
            return "your request couldn't be processed: "+ e.getMessage();
        }
    }

    private static OpenAIConversationDto createOpenAIConversationDto(String prompt) {
        OpenAIConversationDto openAIConversationDto = new OpenAIConversationDto();
        openAIConversationDto.setModel(useLatest?MODEL_LATEST:MODEL);
        List<Message> messages = new ArrayList<>();
        Message message = new Message();
        message.setRole(USER_ROLE);
        message.setContent(prompt);
        messages.add(message);
        openAIConversationDto.setMessages(messages);
        return openAIConversationDto;
    }

    private static String serializeDtoToJson(OpenAIConversationDto openAIConversationDto) {
        return gson.toJson(openAIConversationDto);
    }

    private static OpenAIConversationResDto sendRequestToOpenAI(String json) {
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if(response.code() >= 401 && response.code() < 500){
                APIService.getInstance(null).setApiKey("");
                throw new RuntimeException(INVALID_API_KEY_MSG);
            }
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            OpenAIConversationResDto openAIConversationResDto = gson.fromJson(response.body().string(), OpenAIConversationResDto.class);
            return openAIConversationResDto;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String generate(Map<String, Map<Integer, String>> mergedChanges, Project project) {
        String promt = generatePromt(mergedChanges);
        String result = generate(promt,project);
        return result;
    }
    public static String generatePromt(Map<String, Map<Integer, String>> mergedChanges) {
        StringBuilder extendedPayloadBuilder = new StringBuilder();

        for (Map.Entry<String, Map<Integer, String>> fileEntry : mergedChanges.entrySet()) {
            String fileName = fileEntry.getKey();
            Map<Integer, String> lineChangesMap = fileEntry.getValue();

            extendedPayloadBuilder.append("here are my changes for my java class").append(fileName).append("::");

            for (Map.Entry<Integer, String> lineEntry : lineChangesMap.entrySet()) {
                int lineNumber = lineEntry.getKey();
                String changes = lineEntry.getValue();

                extendedPayloadBuilder.append("`line ::").append(lineNumber).append(" changes :: `").append(changes).append("`");
            }
        }
        extendedPayloadBuilder.append("please suggest me changes according to industry standred and best pratices. Act like super senior java developer ");
        extendedPayloadBuilder.append("please mention file name and line no as well while suggesting changes . so that i can quickly act on your suggestions");
        return extendedPayloadBuilder.toString();
    }
}
