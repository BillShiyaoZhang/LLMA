package LLMABelief;

import org.apache.jena.ontology.OntModel;

public class NegotiationGameOverLLMGeneratedCorrespondence extends NegotiationGameOverCorrespondence{
    private float cosineSimilarityThreshold = 0.8f;

    public NegotiationGameOverLLMGeneratedCorrespondence(
            OntModel source, String sourceEntityURIPrefix, String sourceCollectionName,
            OntModel target, String targetEntityURIPrefix, String targetCollectionName,
            String modelName, float cosineSimilarityThreshold) {
        super(source, sourceEntityURIPrefix, sourceCollectionName,
                target, targetEntityURIPrefix, targetCollectionName, modelName);
        this.cosineSimilarityThreshold = cosineSimilarityThreshold;
    }

    /***
     * Calculate the beliefs for all possible correspondences.
     * Store correspondences that LLM agrees on.
     * This normally would lead to two different sets of correspondences, one for each agent, as the LLM will select
     * the correspondences that it believes are relevant based on the ontology each agent has.
     */
    @Override
    protected void retrieveCorrespondences() {
    }
}
