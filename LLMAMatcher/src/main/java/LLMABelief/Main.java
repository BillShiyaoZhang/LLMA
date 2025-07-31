package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.xml.sax.SAXException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Main {
    public static Dictionary sourceStringsDict = new Hashtable();

    public static Dictionary targetStringsDict = new Hashtable();

    public static Dictionary commonStringsDict = new Hashtable();

    public static void main(String[] args) {
        initStringDictionaries();

        // prepare verboes and embeddings for entities
//        computeVerboes();
//        computeEmbeddings();
//        cosineDistance(humanStringsDict.get("embeddingPath").toString(),
//                mouseStringsDict.get("embeddingPath").toString(),
//                commonStringsDict.get("initCorrespondencesPath").toString(), 0.5);

//        computePotentialPairs();

        // run the game.
        // NOTE: The below game is dependent on the embeddings loaded to the db above.
        play(NegotiationGameOverLLMShortListedCorrespondence.class, commonStringsDict, sourceStringsDict, targetStringsDict);
    }

    private static void computePotentialPairs() {
        Alignment alignment = Helper.loadInitCorrespondences(commonStringsDict.get("initCorrespondencesPath").toString()
                + commonStringsDict.get("threshold").toString() + ".txt");
        Set<String> selfURIs = new HashSet<>();
        Set<String> selfURIS = alignment.getDistinctSourcesAsSet();
        Set<String> selfURIT = alignment.getDistinctTargetsAsSet();

        // Dict: selfURI -> Set of Belief<otherEntityURI>
        Dictionary<String, Set<Belief<String>>> potentialEntityPairsDictS = new Hashtable<>();
        Dictionary<String, Set<Belief<String>>> potentialEntityPairsDictT = new Hashtable<>();
        for (String selfURI : selfURIS) {
            potentialEntityPairsDictS.put(selfURI, new HashSet<>());
        }
        for (String selfURI : selfURIT) {
            potentialEntityPairsDictT.put(selfURI, new HashSet<>());
        }

        // Populate the potentialEntityPairsDict with beliefs based on the alignment
        for (Correspondence c : alignment) {
                potentialEntityPairsDictS.get(c.getEntityOne()).add(new Belief<>(c.getEntityTwo(), c.getConfidence()));
                potentialEntityPairsDictT.get(c.getEntityTwo()).add(new Belief<>(c.getEntityOne(), c.getConfidence()));
        }

        writePotentialEntityPairsToFile(potentialEntityPairsDictS, sourceStringsDict.get("collectionName").toString());
        writePotentialEntityPairsToFile(potentialEntityPairsDictT, targetStringsDict.get("collectionName").toString());
    }

    private static void writePotentialEntityPairsToFile(Dictionary<String, Set<Belief<String>>> potentialEntityPairsDict, String postfix) {
        FileWriter fw = Helper.createFileWriter(commonStringsDict.get("potentiCorrespondencesPath").toString() + commonStringsDict.get("threshold").toString() + "-" + postfix + ".txt");
        try {
            for (String selfURI : ((Hashtable<String, Set<Belief<String>>>) potentialEntityPairsDict).keySet()) {
                Set<Belief<String>> beliefs = potentialEntityPairsDict.get(selfURI);
                if (beliefs.isEmpty()) {
                    continue;
                }
                fw.write("Self URI: " + selfURI + "\n");
                for (Belief<String> belief : beliefs) {
                    String otherEntityURI = belief.obj;
                    double confidence = belief.value;
                    fw.write("  - Other Entity URI: " + otherEntityURI + ", Confidence: " + confidence + "\n");
                }
            }
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initStringDictionaries() {
        commonStringsDict.put("llmApiCaller", LLMApiCallers.LMStudio);
        commonStringsDict.put("modelName", "qwen/qwen3-8b");
        commonStringsDict.put("dataSet", "Anatomy");
        commonStringsDict.put("threshold", 0.6);
        commonStringsDict.put("initCorrespondencesPath", "result/" + commonStringsDict.get("dataSet").toString() + "/init_correspondences/init_correspondences-");
        commonStringsDict.put("DataSetRoot", "src/main/java/DataSet/");
        commonStringsDict.put("reference", "reference.rdf");
        commonStringsDict.put("potentiCorrespondencesPath", "result/" + commonStringsDict.get("dataSet").toString() + "/potential_pairs/init_correspondences-");

        sourceStringsDict.put("ontologyPath", commonStringsDict.get("DataSetRoot").toString() + "/" + commonStringsDict.get("dataSet").toString() + "/human.owl");
        sourceStringsDict.put("verbosePath", "result/" + commonStringsDict.get("dataSet").toString() + "/verbos/human_verbo-remove_null-remove_non_nl-remove_properties.txt");
        sourceStringsDict.put("entityURIPrefix", "http://human.owl#NCI");
        sourceStringsDict.put("propertyUri", "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");
        sourceStringsDict.put("embeddingPath", "result/" + commonStringsDict.get("dataSet").toString() + "/embeddings/human_embeddings-remove_null-remove_non_nl-remove_properties.txt");
        sourceStringsDict.put("collectionName", "Human");
        sourceStringsDict.put("potentialEntityPairsPath", "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/" + sourceStringsDict.get("collectionName").toString() + "/potential_pairs/human_mouse_potential_pairs-");
        sourceStringsDict.put("llmSelectedCorrespondencesPath", "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/" + sourceStringsDict.get("collectionName").toString() + "/llm_selected_correspondences/human_mouse_llm_selected_correspondences-");

        targetStringsDict.put("ontologyPath", commonStringsDict.get("DataSetRoot").toString() + "/" + commonStringsDict.get("dataSet").toString() + "/mouse.owl");
        targetStringsDict.put("verbosePath", "result/" + commonStringsDict.get("dataSet").toString() + "/verbos/mouse_verbo-remove_null-remove_non_nl-remove_properties.txt");
        targetStringsDict.put("entityURIPrefix", "http://mouse.owl#MA");
        targetStringsDict.put("propertyUri", "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");
        targetStringsDict.put("embeddingPath", "result/" + commonStringsDict.get("dataSet").toString() + "/embeddings/mouse_embeddings-remove_null-remove_non_nl-remove_properties.txt");
        targetStringsDict.put("collectionName", "Mouse");
        targetStringsDict.put("potentialEntityPairsPath", "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/"+ targetStringsDict.get("collectionName").toString() + "/potential_pairs/mouse_human_potential_pairs-");
        targetStringsDict.put("llmSelectedCorrespondencesPath", "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/"+ targetStringsDict.get("collectionName").toString() + "/llm_selected_correspondences/mouse_human_llm_selected_correspondences-");
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
                case LMStudio:
                    apiCaller = new LMStudioApiCaller(commonStringsDict.get("modelName").toString());
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
            Alignment alignment = game.play();

            String path = "result/" + commonStringsDict.get("dataSet").toString() + "/" + commonStringsDict.get("modelName").toString() + "/";
            String alignmentPath = path + "alignment-" + commonStringsDict.get("threshold").toString() + ".txt";
            FileWriter fw = Helper.createFileWriter(alignmentPath);
            for (Correspondence c : alignment) {
                fw.write(c.getEntityOne() + ", " + c.getEntityTwo() + ", " + c.getConfidence() + "\n");
            }
            fw.flush();
            fw.close();
            String staticsPath = path + "alignment_statistics-" + commonStringsDict.get("threshold").toString() + ".txt";
            compareWithReference(alignment, staticsPath);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void compareWithReference(Alignment alignment, String statisticsPath) {
        try {
            Alignment reference = new Alignment(new File(
                    commonStringsDict.get("DataSetRoot").toString() + commonStringsDict.get("dataSet").toString() + "/" +  commonStringsDict.get("reference").toString()));
            System.out.println("=========================================");
            System.out.println("Reference size: " + reference.size());
            System.out.println("Alignment size: " + alignment.size());

            int alignmentInReference = 0;
            int referenceInAlignment = 0;
            for (var c : alignment) {
                if (reference.getCorrespondence(c.getEntityTwo(), c.getEntityOne(), CorrespondenceRelation.EQUIVALENCE) != null) {
                    alignmentInReference++;
                }
            }
            for (var c : reference) {
                if (alignment.getCorrespondence(c.getEntityTwo(), c.getEntityOne(), CorrespondenceRelation.EQUIVALENCE) != null) {
                    referenceInAlignment++;
                }
            }

            FileWriter fw = Helper.createFileWriter(statisticsPath);
            fw.write("Alignment in reference: " + alignmentInReference);
            fw.write("\nAlignment not in reference: " + (alignment.size() - alignmentInReference));
            fw.write("\nReference in alignment: " + referenceInAlignment);
            fw.write("\nReference not in alignment: " + (reference.size() - referenceInAlignment));
            fw.flush();
            fw.close();
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }

    }

    private static void computeVerboes() {
        // Anatomy
        verbalize(sourceStringsDict.get("ontologyPaht").toString(),
                sourceStringsDict.get("verbosePath").toString(),
                sourceStringsDict.get("entityURIPrefix").toString(),
                sourceStringsDict.get("propertyUri").toString());
        verbalize(targetStringsDict.get("ontologyPath").toString(),
                targetStringsDict.get("verbosePath").toString(),
                targetStringsDict.get("entityURIPrefix").toString(),
                targetStringsDict.get("propertyUri").toString());
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
        try (FileWriter writer = Helper.createFileWriter(verbosePath)) {
            for (String verbo : verbos) {
                writer.write(verbo + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void computeEmbeddings() {
        // Anatomy
        embed(sourceStringsDict.get("verbosePath").toString(), sourceStringsDict.get("embeddingPath").toString());
        embed(targetStringsDict.get("verbosePath").toString(), targetStringsDict.get("embeddingPath").toString());
    }

    private static void embed(String verboPath, String embeddingPath) {
        Dictionary<String, String> entitiesVerbos = loadEntityVerbos(verboPath);
        if (entitiesVerbos.isEmpty()) {
            System.err.println("No verbos found in the file: " + verboPath);
            return;
        }

        try (FileWriter writer = Helper.createFileWriter(embeddingPath)) {
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

        try (FileWriter writer = Helper.createFileWriter(initCorrespondencesPath + "-" + threshold+".txt")) {
            for (Map.Entry<String, Float[]> entryS : mapS.entrySet()) {
                for (Map.Entry<String, Float[]> entryT : mapT.entrySet()) {
                    Float[] embeddingS = entryS.getValue();
                    Float[] embeddingT = entryT.getValue();

                    if (embeddingS != null && embeddingT != null) {
                        double similarity = cosineSimilarity(embeddingS, embeddingT);
                        if (similarity > threshold) {
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
}
