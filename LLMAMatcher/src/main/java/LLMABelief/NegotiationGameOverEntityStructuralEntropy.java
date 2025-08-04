package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;

import java.util.Dictionary;

public class NegotiationGameOverEntityStructuralEntropy extends NegotiationGameOverCorrespondence {

    public NegotiationGameOverEntityStructuralEntropy(Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller, double threashold) {
        super(sourceStringDict, targetStringDict, apiCaller, threashold);
    }

    @Override
    protected void retrieveCorrespondences(){
        // Calculate the structural entropy for each entity in the source and target ontologies.
        calculateStructuralEntropy(source);
        calculateStructuralEntropy(target);

        // Calculate the confidence of each correspondence based on the structural entropy.
        calculateConfidence(source, target);
    }

    private void calculateStructuralEntropy(Agent agent) {
    }

    private void calculateConfidence(Agent source, Agent target) {

    }

}
