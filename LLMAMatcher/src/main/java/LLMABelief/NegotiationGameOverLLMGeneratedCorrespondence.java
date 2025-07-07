package LLMABelief;

import org.apache.jena.ontology.OntModel;

public class NegotiationGameOverLLMGeneratedCorrespondence extends NegotiationGameOverCorrespondence{
    public NegotiationGameOverLLMGeneratedCorrespondence(OntModel source, String sourceEntityURIPrefix, String sourceCollectionName,
                                                         OntModel target, String targetEntityURIPrefix, String targetCollectionName,
                                                         String modelName) {
        super(source, sourceEntityURIPrefix, sourceCollectionName,
                target, targetEntityURIPrefix, targetCollectionName, modelName);
    }

    @Override
    protected void retrieveCorrespondences() {
        // Calculate the similarity between all possible correspondences

        // Only store correspondences that LLM agrees on.
        // This may lead to two different sets of correspondences, one for each agent, as the LLM will select
        // the correspondences that it believes are relevant based on the ontology each agent has.
    }
}
