package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;

import java.util.Set;

public class Game {
    private Agent source;
    private Agent target;
    private Agent nextProposer;

    public Game(OntModel source, OntModel target, String modelName) {
        this.source = new Agent(source, modelName);
        this.target = new Agent(target, modelName);
    }

    public Alignment play() {
        selectFirstProposer();

        correspondencesSelection();

        Set<Belief> beliefs = correspondencesNegotiation();

        Alignment alignment = resolveConflicts(beliefs);

        return alignment;
    }

    private void selectFirstProposer() {
        // Randomly select the first proposer
        if (Math.random() < 0.5) {
            nextProposer = source;
            System.out.println("Agent 1 is selected as the first proposer.");
        } else {
            nextProposer = target;
            System.out.println("Agent 2 is selected as the first proposer.");
        }
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
    private void correspondencesSelection(){

        while (source.hasEntityLeft() || target.hasEntityLeft()) {
            if (nextProposer.hasEntityLeft()) {
                Agent proposer = nextProposer;
                Agent receiver = (proposer == source) ? target : source;
                OntClass entity = proposer.selectEntityForPairing();
                Set<OntClass> entities = receiver.pair(entity);

                // Each agent examines received entities and updates private correspondences.
                // If correspondences with self-confidence above the threshold are found, the corresponding entities
                // are shared to the other agent.
                while(true) {
                    if (entities == null) {
                        System.out.println("No entities found for pairing by the receiver.");
                        break;
                    }

                    // The Proposer receives entities from the receiver, and updates its correspondences.
                    entities = proposer.pair(entities);
                    if (entities == null) {
                        System.out.println("No entities found for pairing by the proposer.");
                        break;
                    }
                    // The Receiver receives entities from the proposer, and updates its correspondences.
                    entities = receiver.pair(entities);
                }

                switchProposer();
            } else {
                switchProposer();
            }
        }
        System.out.println("Game over: both agents have no entities left to propose.");
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
