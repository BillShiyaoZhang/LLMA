package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.Dictionary;
import java.util.List;
import java.util.Set;

/***
 * This class defines the game, in which two agents negotiate over correspondences (i.e., entity pairs and their beliefs)
 * using LLMs to select correspondences and resolve conflicts.  The negotiation process follows Terry's dialogue.
 */
public class NegotiationGameOverCorrespondence {
    protected Agent source;
    protected Agent target;

    public NegotiationGameOverCorrespondence(Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller, double threshold) {
        this.source = new Agent(sourceStringDict, apiCaller, threshold);
        this.target = new Agent(targetStringDict, apiCaller, threshold);
    }

    public Alignment play() {
        retrieveCorrespondences();
//        return null;
        Alignment correspondencesJointBelief = correspondencesNegotiation();

        Alignment alignment = resolveConflicts(correspondencesJointBelief);

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
    private Alignment correspondencesNegotiation() {
        Alignment alignment = new Alignment();

        for (var c : source.initialCorrespondences) {
            boolean sourceMatched = false;
            boolean targetMatched = false;
            double sourceBelief = 0;
            double targetBelief = 0;
            for (var s : source.privateCorrespondences.getCorrespondencesSource(c.getEntityOne())) {
                if (s.getEntityTwo().equals(c.getEntityTwo())) {
                    sourceMatched = true;
                    sourceBelief = s.getConfidence();
                }
            }
            for (var t : target.privateCorrespondences.getCorrespondencesTarget(c.getEntityTwo())) {
                if (t.getEntityOne().equals(c.getEntityOne())) {
                    targetMatched = true;
                    targetBelief = t.getConfidence();
                }
            }
            if (sourceMatched && targetMatched) {
                // Both agents have the same correspondence, so they can agree on it.
                alignment.add(c.getEntityOne(), c.getEntityTwo(), (sourceBelief + targetBelief) / 2.0, CorrespondenceRelation.EQUIVALENCE);
            } else if (sourceMatched) {
                // Only source agent has the correspondence, so it is added to the joint belief.
                alignment.add(c.getEntityOne(), c.getEntityTwo(), sourceBelief / 2, CorrespondenceRelation.EQUIVALENCE);
            } else if (targetMatched) {
                // Only target agent has the correspondence, so it is added to the joint belief.
                alignment.add(c.getEntityOne(), c.getEntityTwo(), targetBelief / 2, CorrespondenceRelation.EQUIVALENCE);
            }
        }

        return alignment;
    }

    private Alignment resolveConflicts(Alignment input) {
        List<Correspondence> sortedAlignment = input.getConfidenceOrderedMapping();
        Alignment output = new Alignment();
        while (sortedAlignment.size() > 0) {
            Correspondence c = sortedAlignment.remove(sortedAlignment.size() - 1);
            if (output.getCorrespondencesSource(c.getEntityOne()).iterator().hasNext()) {
                // If the source of the correspondence is already in the output, skip it.
                continue;
            }
            if (output.getCorrespondencesTarget(c.getEntityTwo()).iterator().hasNext()) {
                // If the target of the correspondence is already in the output, skip it.
                continue;
            }
            output.add(c);
        }

        return output;
    }
}
