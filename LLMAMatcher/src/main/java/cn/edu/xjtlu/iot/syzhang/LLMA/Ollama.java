package cn.edu.xjtlu.iot.syzhang.LLMA;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.embeddings.OllamaEmbedResponseModel;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ollama extends LLM {
//    private static final String host = "http://localhost:11434/";
    private static final OllamaAPI ollamaAPI = new OllamaAPI("http://localhost:11434/");

    public static List<Double> embed(String query) {
//        String host = "http://localhost:11434/";
//        OllamaAPI ollamaAPI = new OllamaAPI(host);
        try {
            OllamaEmbedResponseModel embeddings = ollamaAPI.embed("nomic-embed-text", Arrays.asList(query));
            return embeddings.getEmbeddings().get(0);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }catch (OllamaBaseException e) {
            System.out.println(e.getMessage());
        }catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public List<Float> getEmbeddings(String prompt) {
        List<Double> doubleList = embed(prompt);
        List<Float> floatList = new ArrayList<>();
        for (Double d : doubleList) {
            floatList.add(d.floatValue());
        }
        return floatList;
    }

    @Override
    public String think(String prompt) {
        try {
            OllamaResult result = ollamaAPI.generate("qwen2.5:32b-instruct", prompt, true, new OptionsBuilder().build());
            return result.getResponse();
        } catch (OllamaBaseException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        return "";
    }
}
