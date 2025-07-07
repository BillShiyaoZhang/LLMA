package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntModel;

import java.util.Set;

/***
 * This class defines the game, in which two agents negotiate over correspondences (i.e., entity pairs and their beliefs)
 * using LLMs to select correspondences and resolve conflicts.  The negotiation process follows Terry's dialogue.
 */
public class NegotiationGameOverCorrespondence {
    protected Agent source;
    protected Agent target;

    public NegotiationGameOverCorrespondence(OntModel source, String sourceEntityURIPrefix, String sourceCollectionName,
                                             OntModel target, String targetEntityURIPrefix, String targetCollectionName,
                                             String modelName) {
        this.source = new Agent(sourceCollectionName, source, sourceEntityURIPrefix, modelName);
        this.target = new Agent(targetCollectionName, target, targetEntityURIPrefix, modelName);
    }

    public Alignment play() {
        retrieveCorrespondences();

        Set<Belief> beliefs = correspondencesNegotiation();

        Alignment alignment = resolveConflicts(beliefs);

        return alignment;
    }

    /***
     * Retrieve correspondences for each agent.
     */
    protected void retrieveCorrespondences() {
    }

    /***
     * Terry's approach.
     * @return The set of beliefs that both agents can agree on.
     */
    private Set<Belief> correspondencesNegotiation() {
        return null;
    }

    private Alignment resolveConflicts(Set<Belief> beliefs) {
        return null;
    }
}
