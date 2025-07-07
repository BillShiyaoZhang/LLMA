package LLMABelief;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class Main {
    private static String[] humanStrings = new String[]{
            "src/main/java/DataSet/Anatomy/human.owl",                                          // ontology path
            "result/Anatomy/human_verbo.txt",                                                   // verbo file path
            "http://human.owl#NCI",                                                             // entity URI prefix
            "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym",                   // property URI
            "result/Anatomy/human_embeddings-remove_null-remove_non_nl-remove_properties.txt",  // embedding file path
            "Human"                                                                             // collection name
    };
    private static String[] mouseStrings = new String[]{
            "src/main/java/DataSet/Anatomy/mouse.owl",                                          // ontology path
            "result/Anatomy/mouse_verbo.txt",                                                   // verbo file path
            "http://mouse.owl#MA",                                                              // entity URI prefix
            "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym",                   // property URI
            "result/Anatomy/mouse_embeddings-remove_null-remove_non_nl-remove_properties.txt",  // embedding file path
            "Mouse"                                                                             // collection name
    };

    private static String modelName = "qwen3-235b-a22b";

    public static void main(String[] args) {
        // prepare verboes and embeddings for entities
//        computeVerboes();
//        computeEmbeddings();

        // init database
        // NOTE: The below embedding loading loads the embeddings from the "result/" folder.
        // Use the above two lines to generate the embeddings first.
//        loadEmbeddings();

        // run the game.
        // NOTE: The below game is dependent on the embeddings loaded to the db above.
        LLMA();
    }

    private static void LLMA() {
        play(NegotiationGameOverLLMGeneratedCorrespondence.class, modelName, humanStrings[0], humanStrings[2], humanStrings[5],
                mouseStrings[0], mouseStrings[2], mouseStrings[5]);
    }

    private static void play(Class type, String modelName,
                             String sourcePath, String sourceEntityURIPrefix, String sourceCollectionName,
                             String targetPath, String targetEntityURIPrefix, String targetCollectionName) {
        OntModel source = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        source.read(sourcePath);
        OntModel target = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        target.read(targetPath);

        try {
            NegotiationGameOverCorrespondence game = (NegotiationGameOverCorrespondence) type
                    .getConstructor(OntModel.class, String.class, String.class,
                            OntModel.class, String.class, String.class, String.class)
                    .newInstance(source, sourceEntityURIPrefix, sourceCollectionName,
                            target, targetEntityURIPrefix, targetCollectionName, modelName);
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
        verbalize(humanStrings[0], humanStrings[1], humanStrings[2], humanStrings[3]);
        verbalize(mouseStrings[0], mouseStrings[1], mouseStrings[2], mouseStrings[3]);
    }

    private static void verbalize(String ontologyPath, String verbosePath, String entityURIPrefix, String propertyUri) {
        OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        ontology.read(ontologyPath);

        // open the result file for writing
        File verboF = new File(verbosePath);
        if (!verboF.getParentFile().exists()) {
            verboF.getParentFile().mkdirs();
        }
        if (!verboF.exists()) {
            try {
                verboF.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> verbos = new ArrayList<>();
        for (OntClass entity: Agent.extractEntities(ontology, entityURIPrefix)) {
            String verbo = Agent.verbalize(entity, propertyUri);

            verbos.add(verbo);
        }

        // write the verbos to the file
        try (FileWriter writer = new FileWriter(verboF)) {
            for (String verbo : verbos) {
                writer.write(verbo + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void computeEmbeddings() {
        // Anatomy
        embed(humanStrings[1], humanStrings[4]);
        embed(mouseStrings[1], mouseStrings[4]);
    }

    private static void embed(String verboPath, String embeddingPath) {
        // open the result file for writing
        File embeddingF = new File(embeddingPath);
        if (!embeddingF.getParentFile().exists()) {
            embeddingF.getParentFile().mkdirs();
        }
        if (!embeddingF.exists()) {
            try {
                embeddingF.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try (FileWriter writer = new FileWriter(embeddingF)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(verboPath))) {
                String line;
                String verbo = "";
                String uri = "";
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("//")) {
                        continue; // skip comments
                    }
                    if (line.trim().isEmpty()) {
                        Float[] embedding = QwenApiCaller.embed(verbo);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadEmbeddings() {
        loadEmbedding(humanStrings[4], humanStrings[5]);
        loadEmbedding(mouseStrings[4], mouseStrings[5]);
    }

    private static void loadEmbedding(String embeddingPath, String collectionName) {
        Weaviate db = new Weaviate(collectionName);
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
                    db.add(collectionName, floatEmbedding, uri);
                } else {
                    System.out.println("Null embedding for URI: " + uri);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
