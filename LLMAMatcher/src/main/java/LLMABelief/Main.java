package LLMABelief;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

public class Main {
    public static void main(String[] args) {
        OntModel source = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        source.read("src/main/java/DataSet/human.owl");
        OntModel target = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        target.read("src/main/java/DataSet/mouse.owl");

        Game game = new Game(source, target, "qwen3-235b-a22b");
        game.play();
    }
}
