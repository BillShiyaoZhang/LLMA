package LLMABelief;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        // prepare data
        verbalizeEntities();

        // run the game
//        LLMA();
    }

    private static void LLMA() {
        play(NegotiationGameWithLLM.class,
                "src/main/java/DataSet/Anatomy/human.owl",
                "src/main/java/DataSet/Anatomy/mouse.owl",
                "http://human.owl#NCI",
                "http://mouse.owl#MA",
                "qwen3-235b-a22b");
    }

    private static void play(Class type,
                             String sourcePath, String targetPath,
                             String entityURIPrefixS, String entityURIPrefixT,
                             String modelName) {
        OntModel source = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        source.read(sourcePath);
        OntModel target = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        target.read(targetPath);

        try {
            NegotiationGameOverCorrespondence game = (NegotiationGameOverCorrespondence) type
                    .getConstructor(OntModel.class, String.class, OntModel.class, String.class, String.class)
                    .newInstance(source, entityURIPrefixS, target, entityURIPrefixT, modelName);
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

    private static void verbalizeEntities() {
        verbalize("src/main/java/DataSet/Anatomy/human.owl",
                "result/Anatomy/human_verbo.txt",
                "http://human.owl#NCI",
                "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");
        verbalize("src/main/java/DataSet/Anatomy/mouse.owl",
                "result/Anatomy/mouse_verbo.txt",
                "http://mouse.owl#MA",
                "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");    }

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
}
