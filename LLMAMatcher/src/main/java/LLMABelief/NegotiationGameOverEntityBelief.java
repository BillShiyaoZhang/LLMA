package LLMABelief;

import java.util.Dictionary;

/***
 * This class defines the game, in which two agents negotiate over entities' beliefs, which are calculated by LLMs.
 * The game amends the calculation of entity beliefs and correspondences process to Terry's model as the means of
 * obtaining correspondences apriori to the encounter defined in Terry's model.
 */
public class NegotiationGameOverEntityBelief extends NegotiationGameOverCorrespondence {

    protected Agent nextProposer;

    public NegotiationGameOverEntityBelief(Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller, double threashold) {
        super(sourceStringDict, targetStringDict, apiCaller, threashold);
    }

    /***
     * Each agent becomes a proposer in turn until all of its entities above the threshold of entity belief
     * are revealed (to the other agent).
     *
     * For each round of selection, the entity, which is above the threshold and has the highest belief,
     * is selected from the proposer at the start.
     * The receiver agent pairs the entity with its own entities, revealing all entities determined to be
     * in the correspondences that the agent has confidence above the threshold of self-confidence
     * (which vary for different algorithms).
     * Then, each of the two agents in turn amends entities if more correspondences found
     * after that the other agent reveals more entities.
     * The selection continues until both agents have no entities with belief above the threshold left to propose.
     *
     * In the end, each of the two agents stores their correspondences privately.
     */
    @Override
    protected void retrieveCorrespondences(){
    }

}
