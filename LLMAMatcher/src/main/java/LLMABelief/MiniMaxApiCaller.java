package LLMABelief;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

public class MiniMaxApiCaller implements LLMApiCaller{

    private static String api_key = System.getenv("MINIMAX");
    private static String base_url = "https://api.minimaxi.com/v1/chat/completions";
    private static String modelName = "MiniMax-M2";

    public MiniMaxApiCaller(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public String prompt(String message) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        // 3. 构建 JSON 请求体 (使用 Jackson)
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", modelName);

        ArrayNode messagesArray = mapper.createArrayNode();
        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("role", "user");
        messageNode.put("content", message);
        messagesArray.add(messageNode);

        requestBody.set("messages", messagesArray);

        try {
            // 将 JSON 对象转换为字符串
            String jsonBody = mapper.writeValueAsString(requestBody);

            // 4. 构建 HTTP Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(base_url))
                    .header("Content-Type", "application/json")
                    // Python 库会自动添加 "Bearer " 前缀
                    .header("Authorization", "Bearer " + api_key)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 5. 发送请求并获取响应
            System.out.println("正在向 Minimax API 发送请求..." + LocalDateTime.now());
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            // 6. 处理响应
            if (response.statusCode() == 200) {
                // 7. 解析 JSON 响应 (使用 Jackson)
                JsonNode rootNode = mapper.readTree(response.body());

                // 导航到 Python 中的 response.choices[0].message.content
                String content = rootNode.path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText();

                if (content.isEmpty()) {
                    System.out.println("无法从响应中解析 content。");
                    System.out.println("完整响应: " + response.body());
                } else {
//                    System.out.println("API 响应:");
                    System.out.println(content);
                    return content;
                }

            } else {
                System.out.println("请求失败，状态码: " + response.statusCode());
                System.out.println("响应体: " + response.body());
            }

        } catch (Exception e) {
            // 处理 InterruptedException 和 IOException
            e.printStackTrace();
        }
        return "";

    }

    @Override
    public Float[] embed(String text) {
        return new Float[0];
    }

    @Override
    public String getUrisOnlyFromStringForThinkingModel(String text) {
        String[] parts = text.split("</think>");
        String removeThinking = "";
        if (parts.length < 2) {
            System.out.println("Warning: LLM response is not formatted correctly. Response: " + text);
            removeThinking = parts[0]; // Return empty string if the response is not formatted correctly
        } else {
            removeThinking = parts[1];
        }
        String prompt = "You are a helpful formatter.  The below is the response from the LLM on the task " +
                "finding the relevant entities regarding a given entity. Please format it to a list of URIs, " +
                "one URI per line, and remove any other text.  " +
                "If there are no URIs, please respond with an empty space only.\n\n" +
                removeThinking;
        String[] formattedResponse = prompt(prompt).split("</think>");
        if (formattedResponse.length < 2) {
            System.out.println("Warning: LLM response is not formatted correctly. Response: " + text);
            return formattedResponse[0]; // Return empty string if the response is not formatted correctly
        }
        return formattedResponse[1];
    }
}
