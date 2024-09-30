package core;


import de.uni_mannheim.informatik.dws.melt.demomatcher.Weaviate;
import org.apache.jena.ontology.OntModel;

public abstract class AbstractAgent implements Agent{
    private OntModel ontologyKnowledge;
//    private OpenAI ai;  // OpenAI API helper functions

    // Embedding based approach
    private Weaviate db;
    private static double similarity_threshold = 0.95;

    @Override
    public void init(OntModel ontology, Weaviate db, double threshold) {
        // Initialize the agent
        this.ontologyKnowledge = ontology;
//        this.ai = new OpenAI();
        this.db = db;
        this.similarity_threshold = threshold;
    }

    public OntModel getOntologyKnowledge() {
        return ontologyKnowledge;
    }
}
