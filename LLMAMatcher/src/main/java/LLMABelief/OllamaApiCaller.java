package LLMABelief;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.embeddings.OllamaEmbedResponseModel;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class OllamaApiCaller implements LLMApiCaller{
    private static final OllamaAPI ollamaAPI = new OllamaAPI();
    private String modelName;

    public OllamaApiCaller(String modelName) {
        this.modelName = modelName;
        ollamaAPI.setRequestTimeoutSeconds(1000000); // Set default timeout to 1000 seconds
    }

    @Override
    public String prompt(String message) {
        message = message + "/no_think";
        try {
            OllamaResult result = ollamaAPI.generate(modelName,
                    message, false, new OptionsBuilder().setNumCtx(8000).build());
            return result.getResponse();
        } catch (OllamaBaseException e) {
            System.out.println("OllamaBaseException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("InterruptedException: " + e.getMessage());
        }
        return "";
    }

    @Override
    public Float[] embed(String text) {
        try {
            OllamaEmbedResponseModel embeddings = ollamaAPI.embed("bge-m3", Arrays.asList(text));
            List<Double> doubleList = embeddings.getEmbeddings().get(0);
            Float[] floatList = new Float[doubleList.size()];
            for (int i = 0; i < doubleList.size(); i++) {
                floatList[i] = doubleList.get(i).floatValue();
            }
            return floatList;
        }catch (IOException e){
            System.out.println(e.getMessage());
        }catch (OllamaBaseException e) {
            System.out.println(e.getMessage());
        }catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        return new Float[0];
    }
}
