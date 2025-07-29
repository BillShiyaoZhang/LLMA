package LLMABelief;

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
        source.privateCorrespondences = loadSelectedCorrespondencesFromFile(llmSelectedCorrespondencesPath);
        target.privateCorrespondences = loadSelectedCorrespondencesFromFile(llmSelectedCorrespondencesPath);
    }

    private void llmSelectCorrespondences() {
    
    }
}
