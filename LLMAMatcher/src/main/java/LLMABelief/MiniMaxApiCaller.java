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

    // TODO: Fill your MiniMax Token Plan API key (Subscription Key) here if not set in environment variable MINIMAX_TOKEN_PLAN
    private static String api_key = "sk-cp-SggurI2t_-eOgi71Y2XU50C3ALHuRO34Id6xeoAKh2kxjXMErAWirp2UIkFe4H4lDfjCsrY7QjIMeqk3iTTy8vS_oSwMk93WTV_xFYoKtOT8aF3Qd9GoR7s";
    private static String base_url = "https://api.minimaxi.com/v1/chat/completions";
    private static String modelName = "MiniMax-M3";

    private String errorLogPath;

    public MiniMaxApiCaller(String modelName) {
        this.modelName = modelName;
    }

    public MiniMaxApiCaller(String modelName, String errorLogPath) {
        this.modelName = modelName;
        this.errorLogPath = errorLogPath;
    }

    private String resolveApiKey() {
        if (api_key != null && !api_key.isEmpty() && !api_key.startsWith("YOUR_")) {
            return api_key;
        }
        String envKey = System.getenv("MINIMAX_TOKEN_PLAN");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        return System.getenv("MINIMAX");
    }

    private long getRemainsTime() {
        String key = resolveApiKey();
        if (key == null || key.isEmpty()) {
            System.out.println("Warning: API Key is empty. Cannot query remains time.");
            return -1;
        }

        // Try both new endpoint and legacy endpoint for resilience
        String[] endpoints = {
            "https://api.minimaxi.com/v1/api/openplatform/coding_plan/remains",
            "https://api.minimaxi.com/v1/token_plan/remains"
        };

        for (String endpoint : endpoints) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Authorization", "Bearer " + key)
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(response.body());
                    
                    // Verify base_resp status
                    JsonNode baseResp = rootNode.path("base_resp");
                    if (!baseResp.isMissingNode() && baseResp.path("status_code").asInt() != 0) {
                        System.out.println("Quota query failed for " + endpoint + ": " + baseResp.path("status_msg").asText());
                        continue;
                    }

                    JsonNode modelRemains = rootNode.path("model_remains");
                    if (modelRemains.isMissingNode() || !modelRemains.isArray()) {
                        modelRemains = rootNode.path("modelRemains");
                    }

                    if (modelRemains.isArray() && modelRemains.size() > 0) {
                        // Look for current model (M3)
                        for (JsonNode model : modelRemains) {
                            String mName = model.path("model_name").asText();
                            if (mName.isEmpty()) {
                                mName = model.path("modelName").asText();
                            }
                            if (mName.equalsIgnoreCase(modelName) || mName.contains("M3") || mName.contains("m3")) {
                                long remainsTime = model.path("remains_time").asLong();
                                if (remainsTime == 0) {
                                    remainsTime = model.path("remainsTime").asLong();
                                }
                                return remainsTime;
                            }
                        }
                        // Default to first model in list if modelName matches none
                        JsonNode firstModel = modelRemains.get(0);
                        long remainsTime = firstModel.path("remains_time").asLong();
                        if (remainsTime == 0) {
                            remainsTime = firstModel.path("remainsTime").asLong();
                        }
                        return remainsTime;
                    }
                }
            } catch (Exception e) {
                System.out.println("Error querying remains time from " + endpoint + ": " + e.getMessage());
            }
        }
        return -1;
    }

    private void handleQuotaExceeded() {
        System.out.println("正在查询 Token Plan 刷新剩余时间..." + LocalDateTime.now());
        long remainsTime = getRemainsTime();
        long sleepMs = 5 * 60 * 1000; // 5 minutes default
        if (remainsTime > 0) {
            // Check if remainsTime is in seconds or milliseconds
            // If remainsTime < 86400 (1 day in seconds), it's probably in seconds
            if (remainsTime < 86400) {
                sleepMs = remainsTime * 1000;
            } else {
                sleepMs = remainsTime;
            }
            // Add a 10-second buffer
            sleepMs += 10_000;
            System.out.println("Token Plan 刷新剩余时间: " + (sleepMs / 1000) + " 秒 (" + (sleepMs / 3600000.0) + " 小时)。程序将暂停等待并重试...");
        } else {
            System.out.println("无法获取准确的刷新剩余时间，将使用默认等待时间: " + (sleepMs / 1000) + " 秒。");
        }

        try {
            long remainingSleep = sleepMs;
            while (remainingSleep > 0) {
                long chunk = Math.min(remainingSleep, 60000); // Wait in 1-minute chunks
                Thread.sleep(chunk);
                remainingSleep -= chunk;
                if (remainingSleep > 0) {
                    System.out.println("还在等待中... 剩余等待时间: " + (remainingSleep / 1000) + " 秒 (" + (remainingSleep / 60000.0) + " 分钟)");
                }
            }
            System.out.println("等待结束，正在重新尝试发送请求..." + LocalDateTime.now());
        } catch (InterruptedException e) {
            System.out.println("等待被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public static class MiniMaxQuotaUsage {
        private final long intervalTotal;
        private final long intervalRemains;
        private final long weeklyTotal;
        private final long weeklyRemains;
        private final String modelName;

        public MiniMaxQuotaUsage(String modelName, long intervalTotal, long intervalRemains, long weeklyTotal, long weeklyRemains) {
            this.modelName = modelName;
            this.intervalTotal = intervalTotal;
            this.intervalRemains = intervalRemains;
            this.weeklyTotal = weeklyTotal;
            this.weeklyRemains = weeklyRemains;
        }

        public String getModelName() { return modelName; }
        public long getIntervalTotal() { return intervalTotal; }
        public long getIntervalRemains() { return intervalRemains; }
        public long getWeeklyTotal() { return weeklyTotal; }
        public long getWeeklyRemains() { return weeklyRemains; }

        @Override
        public String toString() {
            return "MiniMaxQuotaUsage{" +
                    "model='" + modelName + '\'' +
                    ", intervalTotal=" + intervalTotal +
                    ", intervalRemains=" + intervalRemains +
                    ", weeklyTotal=" + weeklyTotal +
                    ", weeklyRemains=" + weeklyRemains +
                    '}';
        }
    }

    public MiniMaxQuotaUsage getQuotaUsage() {
        String key = resolveApiKey();
        if (key == null || key.isEmpty()) {
            System.out.println("Warning: API Key is empty. Cannot query remains.");
            return null;
        }

        String[] endpoints = {
            "https://api.minimaxi.com/v1/api/openplatform/coding_plan/remains",
            "https://api.minimaxi.com/v1/token_plan/remains"
        };

        for (String endpoint : endpoints) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Authorization", "Bearer " + key)
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(response.body());
                    
                    JsonNode baseResp = rootNode.path("base_resp");
                    if (!baseResp.isMissingNode() && baseResp.path("status_code").asInt() != 0) {
                        continue;
                    }

                    JsonNode modelRemains = rootNode.path("model_remains");
                    if (modelRemains.isMissingNode() || !modelRemains.isArray()) {
                        modelRemains = rootNode.path("modelRemains");
                    }

                    if (modelRemains.isArray() && modelRemains.size() > 0) {
                        for (JsonNode model : modelRemains) {
                            String mName = model.path("model_name").asText();
                            if (mName.isEmpty()) {
                                mName = model.path("modelName").asText();
                            }
                            if (mName.equalsIgnoreCase(modelName) || mName.contains("M3") || mName.contains("m3")) {
                                long intervalTotal = model.path("current_interval_total_count").asLong();
                                long intervalRemains = model.path("current_interval_usage_count").asLong();
                                long weeklyTotal = model.path("current_weekly_total_count").asLong();
                                long weeklyRemains = model.path("current_weekly_usage_count").asLong();
                                
                                return new MiniMaxQuotaUsage(mName, intervalTotal, intervalRemains, weeklyTotal, weeklyRemains);
                            }
                        }
                        // Default to first model
                        JsonNode firstModel = modelRemains.get(0);
                        String mName = firstModel.path("model_name").asText();
                        long intervalTotal = firstModel.path("current_interval_total_count").asLong();
                        long intervalRemains = firstModel.path("current_interval_usage_count").asLong();
                        long weeklyTotal = firstModel.path("current_weekly_total_count").asLong();
                        long weeklyRemains = firstModel.path("current_weekly_usage_count").asLong();
                        return new MiniMaxQuotaUsage(mName, intervalTotal, intervalRemains, weeklyTotal, weeklyRemains);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error querying remains from " + endpoint + ": " + e.getMessage());
            }
        }
        return null;
    }

    private boolean isQuotaExceeded(String bodyStr) {
        if (bodyStr == null || bodyStr.isEmpty()) {
            return false;
        }
        String lower = bodyStr.toLowerCase();
        
        // If it's a rate limit or concurrency limit, it's not a quota exhaustion
        if (lower.contains("rate limit") || lower.contains("1002") || lower.contains("conn limit") || lower.contains("1041") || lower.contains("rate_limit")) {
            return false;
        }
        
        // Quota or balance exhaustion indicator
        if (lower.contains("balance") || lower.contains("1008") || lower.contains("quota") || lower.contains("2056") || lower.contains("usage limit") || lower.contains("usage_limit")) {
            return true;
        }
        
        return false;
    }

    @Override
    public String prompt(String message) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", modelName);

        ArrayNode messagesArray = mapper.createArrayNode();
        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("role", "user");
        messageNode.put("content", message);
        messagesArray.add(messageNode);

        requestBody.set("messages", messagesArray);

        long backoffMs = 2000;
        while (true) {
            try {
                String jsonBody = mapper.writeValueAsString(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(base_url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + resolveApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                System.out.println("正在向 Minimax API 发送请求..." + LocalDateTime.now());
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode rootNode = mapper.readTree(response.body());

                    // Check if response contains an error indicating quota limit inside standard response
                    JsonNode errorNode = rootNode.path("error");
                    if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                        String errorMsg = errorNode.path("message").asText();
                        logError("Status Code: 200 (Business Error)\nResponse Body: " + response.body());
                        if (isQuotaExceeded(response.body())) {
                            System.out.println("检测到 API 返回的配额超限错误: " + errorMsg);
                            handleQuotaExceeded();
                            backoffMs = 2000;
                            continue;
                        }
                    }

                    String content = rootNode.path("choices")
                            .path(0)
                            .path("message")
                            .path("content")
                            .asText();

                    if (content.isEmpty()) {
                        String bodyStr = response.body();
                        logError("Status Code: 200 (Empty Content)\nResponse Body: " + bodyStr);
                        if (isQuotaExceeded(bodyStr)) {
                            System.out.println("检测到响应体为空且包含额度超限信息: " + bodyStr);
                            handleQuotaExceeded();
                            backoffMs = 2000;
                            continue;
                        }
                        System.out.println("无法从响应中解析 content。");
                        System.out.println("完整响应: " + bodyStr);
                    } else {
                        System.out.println(content);
                        return content;
                    }

                } else if (response.statusCode() == 429 || response.statusCode() == 402 || response.statusCode() == 400 || response.statusCode() == 403) {
                    String bodyStr = response.body();
                    System.out.println("请求返回状态码: " + response.statusCode() + "，响应体: " + bodyStr);
                    logError("Status Code: " + response.statusCode() + "\nResponse Body: " + bodyStr);
                    if (isQuotaExceeded(bodyStr)) {
                        handleQuotaExceeded();
                        backoffMs = 2000;
                        continue;
                    }
                } else {
                    System.out.println("请求失败，状态码: " + response.statusCode());
                    System.out.println("响应体: " + response.body());
                    logError("Status Code: " + response.statusCode() + "\nResponse Body: " + response.body());
                }

            } catch (Exception e) {
                System.out.println("发送请求发生异常: " + e.getMessage());
                logError("Exception: " + e.getMessage() + "\n" + getStackTraceAsString(e));
                e.printStackTrace();
            }

            // Exponential backoff and retry indefinitely to prevent skipping experimental data
            System.out.println("[WARNING] API 请求失败。为防止实验数据丢失，程序将等待 " + (backoffMs / 1000) + " 秒后重试。");
            System.out.println("请检查网络状况、VPN 代理或 API Key 是否正确配置。");
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return "";
            }
            backoffMs = Math.min(backoffMs * 2, 120000); // Backoff limit: 2 minutes
        }
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

    private void logError(String errorDetail) {
        if (errorLogPath == null || errorLogPath.isEmpty()) {
            return;
        }
        try {
            java.io.File file = new java.io.File(errorLogPath);
            java.io.File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (java.io.FileWriter fw = new java.io.FileWriter(file, true);
                 java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                pw.println("=== Error Occurred at " + LocalDateTime.now() + " ===");
                pw.println(errorDetail);
                pw.println();
            }
        } catch (Exception e) {
            System.err.println("Failed to log error to file " + errorLogPath + ": " + e.getMessage());
        }
    }

    private String getStackTraceAsString(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
