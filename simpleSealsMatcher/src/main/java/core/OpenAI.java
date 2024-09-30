package core;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.exception.HttpResponseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpenAI {
    private static final String azureOpenaiKey = "";
    private static final String endpoint = "";
    private static final String DEPLOYMENT_OR_MODEL_NAME = "gpt-4-32k";
    private static final OpenAIClient client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(azureOpenaiKey))
            .buildClient();

    public int[] comepareComponenties(String source, String[] targets) {
        String prompt = "<Problem Definition>  \n" +
                "In this task, we are giving a) one subject entity, " +
                "and b) a set of entities for potential alignment in the form of " +
                "Relation(Subject, EntitiesOfOtherAgent), which consist of URIs and labels.  \n" +
                "<Subject Entity>  \n %s \n \n" + // subject ontology
                "%s \n \n" +    // ontology in sets
                "Among all entities of other ontology, select all entities that you think " +
                "having a possibility aligning with the subject entity? Please only answer " +
                "with the index of entity (just the index, for example \"1, 2, 4\"). Answer \"no\" " +
                "if you think none of them aligns with the subject entity.";
        String targetsString = "";
        for (int i = 0; i < targets.length; i++) {
            targetsString += String.format("<Entity %d of other ontology>  \n %s \n \n", i + 1, targets[i]);
        }
        String input = String.format(prompt, source, targetsString);
        String thought = think(input);

        if (thought.toLowerCase().contains("no")) {
            return new int[0];
        }

        try{
            String[] results = thought.split(",");
            int[] result = new int[results.length];
            for (int i = 0; i < results.length; i++) {
                result[i] = Integer.parseInt(results[i].trim()) - 1;
            }

            return result;
        } catch (NumberFormatException e){
            System.out.println("Azure: The result is not a number. Thought is: " + thought);
            return new int[0];
        }
    }

    public boolean comepareComponenties(String component1, String component2){
        String prompt = "<Problem Definition>\n" +
                "In this task, we are given two entities in the form of Relation(Subject, Object), which\n" +
                "consist of URIs and labels.\n" +
                "<Entity Triples>\n" +
                "[Entity 1:Entity2]:%s\n" +
                "Do you think these two entities are aligned? If so, please output:yes, " +
                "otherwise, please output:no(just\"yes\" or \"no\", small character no other symbols required) ";

        String ontologies = String.format("[%s,%s]", component1, component2);
        String input = String.format(prompt, ontologies);
        String thought = think(input);

        // check if the thought is yes or no
        if (thought.toLowerCase().contains("yes")){
            return true;
        }
        return false;
    }

    public int whichComponentIsBetter(String source, String[] targets, int expertBeliefIndex, String[] relevantEntities){
        String thought = think(getWhichIsBetterPrompt(source, targets, expertBeliefIndex, relevantEntities));

        // format the result into an integer
        try{
            int result = Integer.parseInt(thought);
            if (result > 0 && result <= targets.length){
                return result - 1;
            }
        } catch (NumberFormatException e){
            System.out.println("Azure: The result is not a number. Thought is: " + thought);
            return -1;
        }
        return -1;
    }

    public int resolveAttack(String source1, String target1, String source2, String target2){
        String prompt = "<Problem Definition>\n" +
                "In this task, we are giving two correspondences of entity. There is one entity " +
                "shared by both correspondences. The correspondences are represented in the form of " +
                "Relation(Correspondence1, Correspondence2), which consists of URIs and labels " +
                "of entities of the correspondences.\n \n" +
                "<Correspondence 1>  \n" +
                "%s \n" +   // source1
                "%s \n \n" +    // target1
                "<Correspondence 2>  \n" +
                "%s" +  // source2
                "%s" +  // target2
                "To remove situation where one entity shared by both correspondences, one correspondence " +
                "should be removed. Which one do you think should be removed? Please only answer with the index of " +
                "entity (just the index, for example \"1\", \"2\"). No explain needed.";
        String input = String.format(prompt, source1, target1, source2, target2);
        String thought = think(input);
        try {
            int result = Integer.parseInt(thought);
            if (result == 1 || result == 2){
                return result;
            }
        } catch (NumberFormatException e){
            System.out.println("Azure: The result is not a number. Thought is: " + thought);
            return 0;
        }

        return 0;
    }

    private String getWhichIsBetterPrompt(String source, String[] targets, int expertBeliefIndex, String[] relevantEntities){
        String prompt = "<Problem Definition>\n" +
                "In this task, we are giving a) one subject entity, " +
                "and b) a set of entities for potential alignment in the form of " +
                "Relation(Subject, EntitiesOfOtherAgent), which consists of URIs and labels.\n \n" +
                "<Subject Entity>  \n %s \n \n" + // subject ontology
                "%s" +  //"<Entity 1 of other ontology>  \n %s \n \n" + ...
                "%s" +  // expert belief
                "%s" +  // relevant entities
                "Among all entities of other ontology, which one do you think aligns " +
                "with the subject entity best? Please only answer with the index of " +
                "entity (just the index, for example \"1\", \"2\", \"3\",... \n )";

        String targetsString = "";
        for (int i = 0; i < targets.length; i++) {
            targetsString += String.format("<Entity %d of other ontology>  \n %s \n \n", i + 1, targets[i]);
        }
        String expertBeliefString = "";
        if (expertBeliefIndex > 0) {
            expertBeliefString = String.format("Some experts believe that entity %d in set is a better alignment " +
                    "with the subject entity among other entities in set. ", expertBeliefIndex);
        }
        String relevantEntitiesString = "";
        if (relevantEntities != null){
            relevantEntitiesString = "We provide you following entities that are relevant to the subject entity for reference. \n";
            for (int i = 0; i < relevantEntities.length; i++) {
                relevantEntitiesString += String.format("<Entity %d relevant to the subject entity>  \n %s \n \n", i + 1, relevantEntities[i]);
            }
        }

        return String.format(prompt, source, targetsString, expertBeliefString, relevantEntitiesString);
    }

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

    private String think(String prompt) {
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


