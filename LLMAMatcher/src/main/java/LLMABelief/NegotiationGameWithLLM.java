package LLMABelief;

import org.apache.jena.ontology.OntModel;

import java.util.Set;

public class NegotiationGameWithLLM extends NegotiationGameOverCorrespondence{
    public NegotiationGameWithLLM(OntModel source, String entityURIPrefixS,
                                  OntModel target, String entityURIPrefixT, String modelName) {
        super(source, entityURIPrefixS, target, entityURIPrefixT, modelName);
    }

    /***
     * Do nothing.
     */
    protected void retrieveCorrespondences() {
    }

    /***
     * LLMA's approach.
     */
    private Set<Belief> correspondencesNegotiation() {
        return null;
    }
}
