package LLMABelief;

import java.util.Dictionary;

public class NegotiationGameOverLLMShortListedCorrespondence extends NegotiationGameOverCorrespondence{
    private String initCorrespondencesPath;

    public NegotiationGameOverLLMShortListedCorrespondence(
            Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller, String initCorrespondencesPath,
            double threshold) {
        super(sourceStringDict, targetStringDict, apiCaller, threshold);
        this.initCorrespondencesPath = initCorrespondencesPath;
    }

    /***
     * Calculate the beliefs for all possible correspondences.
     * Store correspondences that LLM agrees on.
     * This normally would lead to two different sets of correspondences, one for each agent, as the LLM will select
     * the correspondences that it believes are relevant based on the ontology each agent has.
     */
    @Override
    protected void retrieveCorrespondences() {
        // NOTE: the results of the below codes are stored in the "result/" folder.
        // Only use the below codes if you want to generate the correspondences for each agent.
        source.shortListCorrespondences(target.entityVerbos);
        target.shortListCorrespondences(source.entityVerbos);
    }
}
