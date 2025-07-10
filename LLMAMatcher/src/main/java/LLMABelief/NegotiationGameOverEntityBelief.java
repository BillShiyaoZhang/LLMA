package LLMABelief;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;

import java.util.Dictionary;
import java.util.Set;

/***
 * This class defines the game, in which two agents negotiate over entities' beliefs, which are calculated by LLMs.
 * The game amends the calculation of entity beliefs and correspondences process to Terry's model as the means of
 * obtaining correspondences apriori to the encounter defined in Terry's model.
 */
public class NegotiationGameOverEntityBelief extends NegotiationGameOverCorrespondence {

    protected Agent nextProposer;

    public NegotiationGameOverEntityBelief(Dictionary sourceStringDict, Dictionary targetStringDict, String modelName, double threashold) {
        super(sourceStringDict, targetStringDict, modelName, threashold);
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
//        selectFirstProposer();
//
//        while (source.hasUnrevealedEntity() || target.hasUnrevealedEntity()) {
//            if (nextProposer.hasUnrevealedEntity()) {
//                Agent proposer = nextProposer;
//                Agent receiver = (proposer == source) ? target : source;
//                OntClass entity = proposer.nextUnrevealedEntity();
//                Set<OntClass> entities = receiver.pair(entity);
//
//                // Each agent examines received entities and updates private correspondences.
//                // If correspondences with self-confidence above the threshold are found, the corresponding entities
//                // are shared to the other agent.
//                while(true) {
//                    if (entities == null) {
//                        System.out.println("No entities found for pairing by the receiver.");
//                        break;
//                    }
//
//                    // The Proposer receives entities from the receiver, and updates its correspondences.
//                    entities = proposer.pair(entities);
//                    if (entities == null) {
//                        System.out.println("No entities found for pairing by the proposer.");
//                        break;
//                    }
//                    // The Receiver receives entities from the proposer, and updates its correspondences.
//                    entities = receiver.pair(entities);
//                }
//
//                switchProposer();
//            } else {
//                switchProposer();
//            }
//        }
//        System.out.println("Game over: both agents have no entities left to propose.");
    }

    private void selectFirstProposer() {
        if (Math.random() < 0.5) {
            nextProposer = source;
            System.out.println("Agent 1 is selected as the first proposer.");
        } else {
            nextProposer = target;
            System.out.println("Agent 2 is selected as the first proposer.");
        }
    }

    private void switchProposer() {
        if (nextProposer == source) {
            nextProposer = target; // Switch to the other agent
            System.out.println("Switching to Agent 2 for the next proposal.");
        } else {
            nextProposer = source; // Switch back to the first agent
            System.out.println("Switching to Agent 1 for the next proposal.");
        }
    }


}
