package LLMABelief;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.lang.reflect.InvocationTargetException;

public class Main {
    public static void main(String[] args) {
        play(NegotiationGameOverEntityBelief.class,
                "src/main/java/DataSet/human.owl",
                "src/main/java/DataSet/mouse.owl",
                "http://human.owl#NCI",
                "http://mouse.owl#MA",
                "qwen3-235b-a22b");
    }

    public static void play(Class type, String sourcePath, String targetPath, String entityURIPrefixS,
                            String entityURIPrefixT, String modelName) {
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
}
