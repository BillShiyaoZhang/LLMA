package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.Dictionary;
import java.util.Set;

/***
 * This class defines the game, in which two agents negotiate over correspondences (i.e., entity pairs and their beliefs)
 * using LLMs to select correspondences and resolve conflicts.  The negotiation process follows Terry's dialogue.
 */
public class NegotiationGameOverCorrespondence {
    protected Agent source;
    protected Agent target;

    public NegotiationGameOverCorrespondence(Dictionary sourceStringDict, Dictionary targetStringDict, String modelName, double threshold) {
        this.source = new Agent(sourceStringDict, modelName, threshold);
        this.target = new Agent(targetStringDict, modelName, threshold);
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
        // Leave this empty, as Terry's approach assumes that both agents know their private correspondences
        // prior to the encounter.
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
