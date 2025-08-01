package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;

import java.util.Dictionary;

public class NegotiationGameOverLLMSelectCorrespondence extends NegotiationGameOverCorrespondence {
    private String llmSelectedCorrespondencesPath;

    public NegotiationGameOverLLMSelectCorrespondence(
            Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller, String llmSelectedCorrespondencesPath,
            double threshold) {
        super(sourceStringDict, targetStringDict, apiCaller, threshold);
        this.llmSelectedCorrespondencesPath = llmSelectedCorrespondencesPath;
    }

    @Override
    protected void retrieveCorrespondences() {
        retrieveLLMShortListedCorrespondences();
        llmSelectCorrespondences();
    }

    private void retrieveLLMShortListedCorrespondences() {
        source.privateCorrespondences = source.loadSelectedCorrespondencesFromFile(llmSelectedCorrespondencesPath);
        target.privateCorrespondences = target.loadSelectedCorrespondencesFromFile(llmSelectedCorrespondencesPath);
    }

    private void llmSelectCorrespondences() {
    
    }
}
