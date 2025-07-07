package LLMABelief;

import cn.edu.xjtlu.iot.syzhang.LLMA.Ollama;
import cn.edu.xjtlu.iot.syzhang.LLMA.Weaviate;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class NegotiationGameWithLLM extends NegotiationGameOverCorrespondence{
    public NegotiationGameWithLLM(OntModel source, String entityURIPrefixS,
                                  OntModel target, String entityURIPrefixT, String modelName) {
        super(source, entityURIPrefixS, target, entityURIPrefixT, modelName);
    }

    @Override
    protected void retrieveCorrespondences() {
        // Embedding all entities for each agent
        embeddingEntities(source);
        embeddingEntities(target);

        // Calculate the similarity between all possible correspondences

        // Only store correspondences that LLM agrees on.
        // This may lead to two different sets of correspondences, one for each agent, as the LLM will select
        // the correspondences that it believes are relevant based on the ontology each agent has.
    }

    private void embeddingEntities(Agent agent) {
        // open the result file for writing
        File embeddingF = new File("result/Anatomy/" + agent.name + "_embeddings.txt");
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

        File verboF = new File("result/Anatomy/" + agent.name + "_verbo.txt");
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
        ArrayList<Float[]> embeddings = new ArrayList<>();
        for (OntClass entity: agent.entities) {
            String verbo = agent.verbalize(entity, "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");
//            Float[] embedding = agent.llm.embed(verbo);

            verbos.add(verbo);
//            embeddings.add(embedding);
        }

        // write the verbos to the file
        try (FileWriter writer = new FileWriter(verboF)) {
            for (String verbo : verbos) {
                writer.write(verbo + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // write the embeddings to the file
//        try (FileWriter writer = new FileWriter(embeddingF)) {
//            for (Float[] embedding : embeddings) {
//                StringBuilder sb = new StringBuilder();
//                for (Float value : embedding) {
//                    sb.append(value).append(",");
//                }
//                sb.deleteCharAt(sb.length() - 1); // remove the last comma
//                writer.write(sb.toString() + "\n");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        System.out.println(agent.name + " finished embedding.");
    }

}
