package cn.edu.xjtlu.iot.syzhang.LLMA;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.types.OllamaModelType;
import io.github.ollama4j.models.embeddings.OllamaEmbedRequestModel;
import io.github.ollama4j.models.embeddings.OllamaEmbedResponseModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Ollama {
    public static List<Double> embed(String query) throws IOException, OllamaBaseException, InterruptedException {
        String host = "http://localhost:11434/";
        OllamaAPI ollamaAPI = new OllamaAPI(host);
        OllamaEmbedResponseModel embeddings = ollamaAPI.embed("nomic-embed-text", Arrays.asList(query));
        return embeddings.getEmbeddings().get(0);
    }

}
