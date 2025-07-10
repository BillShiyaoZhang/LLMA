package LLMABelief;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Main {
    public static Dictionary humanStringsDict = new Hashtable();

    public static Dictionary mouseStringsDict = new Hashtable();

    public static Dictionary commonStringsDict = new Hashtable();

    public static void main(String[] args) {
        initStringDictionaries();

        // prepare verboes and embeddings for entities
//        computeVerboes();
//        computeEmbeddings();
//        cosineDistance(humanStringsDict.get("embeddingPath").toString(),
//                mouseStringsDict.get("embeddingPath").toString(),
//                commonStringsDict.get("initCorrespondencesPath").toString(), 0.5);

        // init database
        // NOTE: The below embedding loading loads the embeddings from the "result/" folder.
        // Use the above lines to generate the embeddings first.
//        loadEmbeddings();

        // run the game.
        // NOTE: The below game is dependent on the embeddings loaded to the db above.
        play(NegotiationGameOverLLMGeneratedCorrespondence.class, commonStringsDict, humanStringsDict, mouseStringsDict);
    }

    private static void initStringDictionaries() {
        commonStringsDict.put("llmApiCaller", LLMApiCallers.Ollama);
        commonStringsDict.put("modelName", "qwen3:8b");
        commonStringsDict.put("dataSet", "Anatomy");
        commonStringsDict.put("threshold", 0.9);
        commonStringsDict.put("initCorrespondencesPath", "result/" + commonStringsDict.get("dataSet").toString() + "/init_correspondences/init_correspondences-");
        commonStringsDict.put("DataSetRoot", "src/main/java/DataSet/");

        humanStringsDict.put("ontologyPath", commonStringsDict.get("DataSetRoot").toString() + "/" + commonStringsDict.get("dataSet").toString() + "/human.owl");
        humanStringsDict.put("verbosePath", "result/" + commonStringsDict.get("dataSet").toString() + "/verbos/human_verbo-remove_null-remove_non_nl-remove_properties.txt");
        humanStringsDict.put("entityURIPrefix", "http://human.owl#NCI");
        humanStringsDict.put("propertyUri", "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");
        humanStringsDict.put("embeddingPath", "result/" + commonStringsDict.get("dataSet").toString() + "/embeddings/human_embeddings-remove_null-remove_non_nl-remove_properties.txt");
        humanStringsDict.put("collectionName", "Human");
        humanStringsDict.put("potentialEntityPairsPath", "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/" + humanStringsDict.get("collectionName").toString() + "/potential_pairs/human_mouse_potential_pairs-");
        humanStringsDict.put("llmSelectedCorrespondencesPath", "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/" + humanStringsDict.get("collectionName").toString() + "/llm_selected_correspondences/human_mouse_llm_selected_correspondences-");

        mouseStringsDict.put("ontologyPath", commonStringsDict.get("DataSetRoot").toString() + "/" + commonStringsDict.get("dataSet").toString() + "/mouse.owl");
        mouseStringsDict.put("verbosePath", "result/" + commonStringsDict.get("dataSet").toString() + "/verbos/mouse_verbo-remove_null-remove_non_nl-remove_properties.txt");
        mouseStringsDict.put("entityURIPrefix", "http://mouse.owl#MA");
        mouseStringsDict.put("propertyUri", "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");
        mouseStringsDict.put("embeddingPath", "result/" + commonStringsDict.get("dataSet").toString() + "/embeddings/mouse_embeddings-remove_null-remove_non_nl-remove_properties.txt");
        mouseStringsDict.put("collectionName", "Mouse");
        mouseStringsDict.put("potentialEntityPairsPath", "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/"+ mouseStringsDict.get("collectionName").toString() + "/potential_pairs/mouse_human_potential_pairs-");
        mouseStringsDict.put("llmSelectedCorrespondencesPath", "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/"+ mouseStringsDict.get("collectionName").toString() + "/llm_selected_correspondences/mouse_human_llm_selected_correspondences-");
    }

    private static void play(Class type, Dictionary commonStringsDict, Dictionary sourceStringDict, Dictionary targetStringDict) {
        try {
            LLMApiCaller apiCaller;
            switch ((LLMApiCallers) commonStringsDict.get("llmApiCaller")) {
                case Ollama:
                    apiCaller = new OllamaApiCaller(commonStringsDict.get("modelName").toString());
                    break;
                case Qwen:
                    apiCaller = new QwenApiCaller(commonStringsDict.get("modelName").toString());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported LLM API caller: " + commonStringsDict.get("llmApiCaller"));
            }
            NegotiationGameOverCorrespondence game = (NegotiationGameOverCorrespondence) type
                    .getConstructor(Dictionary.class, Dictionary.class, LLMApiCaller.class, String.class, double.class)
                    .newInstance(sourceStringDict, targetStringDict, apiCaller,
                            commonStringsDict.get("initCorrespondencesPath").toString()
                                    + commonStringsDict.get("threshold").toString() + ".txt",
                            commonStringsDict.get("threshold"));
            game.play();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void computeVerboes() {
        // Anatomy
        verbalize(humanStringsDict.get("ontologyPaht").toString(),
                humanStringsDict.get("verbosePath").toString(),
                humanStringsDict.get("entityURIPrefix").toString(),
                humanStringsDict.get("propertyUri").toString());
        verbalize(mouseStringsDict.get("ontologyPath").toString(),
                mouseStringsDict.get("verbosePath").toString(),
                mouseStringsDict.get("entityURIPrefix").toString(),
                mouseStringsDict.get("propertyUri").toString());
    }

    private static void verbalize(String ontologyPath, String verbosePath, String entityURIPrefix, String propertyUri) {
        OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        ontology.read(ontologyPath);

        ArrayList<String> verbos = new ArrayList<>();
        for (OntClass entity: Agent.extractEntities(ontology, entityURIPrefix)) {
            String verbo = Agent.verbalize(entity, propertyUri);

            verbos.add(verbo);
        }

        // write the verbos to the file
        try (FileWriter writer = createFileWriter(verbosePath)) {
            for (String verbo : verbos) {
                writer.write(verbo + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void computeEmbeddings() {
        // Anatomy
        embed(humanStringsDict.get("verbosePath").toString(), humanStringsDict.get("embeddingPath").toString());
        embed(mouseStringsDict.get("verbosePath").toString(), mouseStringsDict.get("embeddingPath").toString());
    }

    private static void embed(String verboPath, String embeddingPath) {
        Dictionary<String, String> entitiesVerbos = loadEntityVerbos(verboPath);
        if (entitiesVerbos.isEmpty()) {
            System.err.println("No verbos found in the file: " + verboPath);
            return;
        }

        try (FileWriter writer = createFileWriter(embeddingPath)) {
            var it = entitiesVerbos.keys().asIterator();
            while (it.hasNext()) {
                String uri = it.next();
                String verbo = entitiesVerbos.get(uri);
                if (verbo == null || verbo.trim().isEmpty()) {
                    System.err.println("Skipping empty verbo for URI: " + uri);
                    continue;
                }
                LLMApiCaller apiCaller = new QwenApiCaller(commonStringsDict.get("modelName").toString());
                Float[] embedding = apiCaller.embed(verbo);
                writer.write(uri + "\n");
                if (embedding != null) {
                    StringBuilder sb = new StringBuilder();
                    for (Float value : embedding) {
                        sb.append(value).append(",");
                    }
                    sb.deleteCharAt(sb.length() - 1); // remove the last comma
                    writer.write(sb.toString() + "\n");
                } else {
                    writer.write("null\n");
                }
                System.out.println(uri + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Dictionary<String, String> loadEntityVerbos(String verboPath) {
        Dictionary<String, String> entitiesVerbos = new Hashtable<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(verboPath))) {
            String line;
            String verbo = "";
            String uri = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("//")) {
                    continue; // skip comments
                }
                if (line.trim().isEmpty()) {
                    entitiesVerbos.put(uri, verbo);

                    verbo = ""; // reset verbo for the next entity
                    uri = ""; // reset uri for the next entity
                    continue;
                }
                if (line.startsWith("URI: ")) {
                    line = line.substring("URI: ".length());
                    uri = line;
                    continue;
                }
                verbo += line + "\n"; // accumulate the verbos
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entitiesVerbos;
    }

    private static void loadEmbeddings() {
        loadEmbedding(humanStringsDict.get("embeddingPath").toString(), humanStringsDict.get("collectionName").toString());
        loadEmbedding(mouseStringsDict.get("embeddingPath").toString(), mouseStringsDict.get("collectionName").toString());
    }

    private static void loadEmbedding(String embeddingPath, String collectionName) {
        Map<String, Float[]> map = readEmbeddings(embeddingPath);
        Weaviate db = new Weaviate(collectionName);
        for (Map.Entry<String, Float[]> entry : map.entrySet()) {
            db.add(collectionName, entry.getKey(), entry.getValue());
        }
    }

    private static Map<String, Float[]>  readEmbeddings(String embeddingPath) {
        Map<String, Float[]> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(embeddingPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // skip empty lines
                }
                String uri = line.trim();
                line = reader.readLine();
                String[] embedding = line.trim().split(",");
                if (embedding.length > 0 && !embedding[0].equals("null")) {
                    Float[] floatEmbedding = new Float[embedding.length];
                    for (int i = 0; i < embedding.length; i++) {
                        floatEmbedding[i] = Float.parseFloat(embedding[i]);
                    }
                    map.put(uri, floatEmbedding);
                } else {
                    System.out.println("Null embedding for URI: " + uri);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }


    private static void cosineDistance(String embeddingPathS, String embeddingPathT,
                                       String initCorrespondencesPath, double threshold) {
        Map<String, Float[]> mapS = readEmbeddings(embeddingPathS);
        Map<String, Float[]> mapT = readEmbeddings(embeddingPathT);

        try (FileWriter writer = createFileWriter(initCorrespondencesPath + "-" + threshold+".txt")) {
            for (Map.Entry<String, Float[]> entryS : mapS.entrySet()) {
                for (Map.Entry<String, Float[]> entryT : mapT.entrySet()) {
                    Float[] embeddingS = entryS.getValue();
                    Float[] embeddingT = entryT.getValue();

                    if (embeddingS != null && embeddingT != null) {
                        double similarity = cosineSimilarity(embeddingS, embeddingT);
                        if (similarity > threshold) {
//                            Correspondence correspondence = new Correspondence(
//                                    entryS.getKey(), entryT.getKey(), similarity);
                            writer.write(entryS.getKey() + ", " + entryT.getKey() + ", " + similarity + "\n");
                        }
                    } else {
                        System.err.println("Embedding not found for: " + entryS.getKey() + " or " + entryT.getKey());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double cosineSimilarity(Float[] vecA, Float[] vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += Math.pow(vecA[i], 2);
            normB += Math.pow(vecB[i], 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static FileWriter createFileWriter(String filePath) {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            return new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
