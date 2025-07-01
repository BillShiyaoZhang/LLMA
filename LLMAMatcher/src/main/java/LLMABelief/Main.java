package LLMABelief;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

public class Main {
    public static void main(String[] args) {
        // Initialize the ontology model and LLM API caller
        OntModel source = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        source.read("src/main/java/DataSet/human.owl");

        QwenApiCaller llm = new QwenApiCaller("qwen-plus");

        // Create the agent with the ontology model, LLM API caller, and initial state
        Agent agent = new Agent(source, llm);

        StateMessage input = new StateMessage();
        input.simpleMessage = "hi";
        // Start the agent's operation (this could be a method in Agent class)
         agent.start(input);
    }
}
