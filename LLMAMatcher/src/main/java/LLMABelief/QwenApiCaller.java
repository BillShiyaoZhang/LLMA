package LLMABelief;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class QwenApiCaller implements LLMApiCaller {
    private static String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/";

    private static String apiKey= System.getenv("DASHSCOPE_API_KEY");
    private String modelName;

    /**
     * Constructor that initializes the QwenApiCaller with a specific model name.
     *
     * @param modelName The name of the model to use for the API calls.
     */
    public QwenApiCaller(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Sends a prompt to the Qwen API and returns the response.
     *
     * @param message The message to send to the Qwen API.
     * @return The response from the Qwen API.
     */
    @Override
    public String prompt(String message) {
        message = message.replaceAll("\n", "");

        String requestBody = "{\n" +
                "    \"model\": \""+ modelName +"\",\n" +
                "    \"enable_thinking\": "+ false +",\n" +
                "    \"messages\": [\n" +
                "        {\n" +
                "            \"role\": \"system\",\n" +
                "            \"content\": \"You are a helpful assistant.\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"role\": \"user\", \n" +
                "            \"content\": \""+ message +"\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            if (response.statusCode() != 200) {
                System.err.println("Error: " + response.statusCode() + " - " + response.body());
                return "ERROR";
            }

            var content = JsonParser.parseString(response.body())
                    .getAsJsonObject()
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();

            System.out.println(content);

            return content;

        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public Float[] embed(String text) {
        text = text.replaceAll("\n", "\\n");
        String requestBody = "{\n" +
                "  \"model\": \"text-embedding-v4\",\n" +
                "  \"input\": \"" + text + "\",\n" +
                "  \"dimension\": \"2048\",\n" +
                "  \"encoding_format\": \"float\"\n" +
                "}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "embeddings"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Error: " + response.statusCode() + " - " + response.body());
                return null;
            }
            return extractEmbedding(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Float[] extractEmbedding(String jsonResponse) {
        JsonArray embeddingArray = JsonParser.parseString(jsonResponse)
                .getAsJsonObject()
                .getAsJsonArray("data")
                .get(0)
                .getAsJsonObject()
                .getAsJsonArray("embedding");

        Float[] result = new Float[embeddingArray.size()];
        for (int i = 0; i < embeddingArray.size(); i++) {
            result[i] = embeddingArray.get(i).getAsFloat();
        }
        return result;
    }
}