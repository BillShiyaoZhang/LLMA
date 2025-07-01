package LLMABelief;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class QwenApiCaller {
    private static String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private String apiKey;
//        apiKey = "sk-6a0b60e02a8847fe8b15cbfae9b4fa4f";
    private String modelName;

    /**
     * Default constructor that initializes the QwenApiCaller with the API key from environment variables
     * and the default model name "qwen-plus".
     */
    public QwenApiCaller() {
        this(System.getenv("DASHSCOPE_API_KEY"), "qwen-plus");
    }

    /**
     * Constructor that initializes the QwenApiCaller with a specific API key and model name.
     *
     * @param apiKey    The API key for accessing the Qwen API.
     * @param modelName The name of the model to use for the API calls.
     */
    public QwenApiCaller(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    /**
     * Sends a prompt to the Qwen API and returns the response.
     *
     * @param message The message to send to the Qwen API.
     * @return The response from the Qwen API.
     */
    public String prompt(String message) {
        String requestBody = "{\n" +
                "    \"model\": \""+ modelName +"\",\n" +
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
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            return response.body();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }
}