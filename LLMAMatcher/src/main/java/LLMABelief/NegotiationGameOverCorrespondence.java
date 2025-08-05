package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;

import java.util.Dictionary;
import java.util.List;

/***
 * This class defines the game, in which two agents negotiate over correspondences (i.e., entity pairs and their beliefs)
 * using LLMs to select correspondences and resolve conflicts.  The negotiation process follows Terry's dialogue.
 */
public class NegotiationGameOverCorrespondence {
    protected Agent source;
    protected Agent target;

    public NegotiationGameOverCorrespondence(Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller) {
        this.source = new Agent(sourceStringDict, apiCaller);
        this.target = new Agent(targetStringDict, apiCaller);
    }

    public Alignment play() {
        retrieveCorrespondences();
//        return null;
        modifyCorrespondenceConfidencesBeforeNegotiation();
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
     * Allow child classes to modify the confidences of the correspondences before the negotiation.
     */
    protected void modifyCorrespondenceConfidencesBeforeNegotiation() {
        // Leave this empty, as Terry's approach does not require any modification of the confidences before the negotiation.
        // However, child classes can override this method to implement their own logic.
    }

    /***
     * Terry's approach.
     * @return The set of beliefs that both agents can agree on.
     */
    private Alignment correspondencesNegotiation() {
        Alignment alignment = new Alignment();

        for (var c : source.getInitialCorrespondences()) {
            double sourceBelief = 0;
            double targetBelief = 0;
            for (var s : source.privateCorrespondences.getCorrespondencesSource(c.getEntityOne())) {
                if (s.getEntityTwo().equals(c.getEntityTwo())) {
                    sourceBelief = s.getConfidence();
                }
            }
            for (var t : target.privateCorrespondences.getCorrespondencesTarget(c.getEntityTwo())) {
                if (t.getEntityOne().equals(c.getEntityOne())) {
                    targetBelief = t.getConfidence();
                }
            }
            alignment.add(c.getEntityOne(), c.getEntityTwo(), (sourceBelief + targetBelief) / 2, CorrespondenceRelation.EQUIVALENCE);
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
            if (c.getConfidence() < 0.01) {
                // If the confidence of the correspondence is below the threshold, skip it.
                continue;
            }
            output.add(c);
        }

        return output;
    }
}
