package cn.edu.xjtlu.iot.syzhang.LLMA;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.azure.core.exception.HttpResponseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class OpenAI extends LLM {
    private static final String azureOpenaiKey = "";
    private static final String endpoint = "";
    private static final String DEPLOYMENT_OR_MODEL_NAME = "gpt-4-32k";
    private static final OpenAIClient client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(azureOpenaiKey))
            .buildClient();

    public List<Float> getEmbeddings(String prompt) {
        EmbeddingsOptions embeddingsOptions = new EmbeddingsOptions(Arrays.asList(prompt));
        Embeddings embeddings = client.getEmbeddings("text-embedding-ada-002", embeddingsOptions);
        List<Double> var =  embeddings.getData().get(0).getEmbedding();

        ArrayList<Float> vector = new ArrayList<>();
        for (Double value : var) {
            vector.add(value.floatValue());
        }
        return vector;
    }

    public String think(String prompt) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.USER, prompt));
        ChatCompletions chatCompletions = null;
        boolean flag = false;
        while(!flag){
            try{
                chatCompletions = client.getChatCompletions(DEPLOYMENT_OR_MODEL_NAME, new ChatCompletionsOptions(chatMessages));
                flag = true;
            } catch (HttpResponseException e){
//                System.out.println(e.getMessage());
//                System.out.println("Azure: Waiting for the server to be ready...");
            }
        }
        String result = chatCompletions.getChoices().get(0).getMessage().getContent();
//        System.out.println(result);
        return result;
    }
}


