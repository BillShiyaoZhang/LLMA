package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Agent {
    private String name;
    private QwenApiCaller llm;
    private OntModel ontology;

    private AgentState initState;

    private List<OntClass> entities;
    private List<Belief<OntClass>> entityBeliefs;
    private List<Belief<OntClass>> unrevealedEntitiesWithDescendingBelief;

    private Set<Belief<Correspondence>> correspondenceBeliefs;

    public Agent(String agentName, OntModel ontology, String entityURIPrefix, String modelName) {
        this.name = agentName;
        this.llm = new QwenApiCaller(modelName);
        this.ontology = ontology;

        entities = extractEntities(ontology, entityURIPrefix);
        entityBeliefs = initConfidence(entities);
        unrevealedEntitiesWithDescendingBelief = descending(entityBeliefs);

        correspondenceBeliefs = new HashSet<>();

        this.initState = initStateGraph();
    }

    private List<OntClass> extractEntities(OntModel ontology, String entityURIPrefix) {
        // get all OntClass from ontology.listClasses()
        List<OntClass> entities = new ArrayList<>();
//        ontology.listClasses().forEachRemaining(entities::add);
        for (OntClass ontClass : ontology.listClasses().toList()) {
            if (ontClass.isURIResource()
                    && !ontClass.getURI().isEmpty()
                    && ontClass.getURI().startsWith(entityURIPrefix)) {
                entities.add(ontClass);
            }
        }

        System.out.println("Agent " +  "finds " + entities.size() + " entities.");
        return entities;
    }

    private List<Belief<OntClass>> initConfidence(List<OntClass> entities) {
        return null;
    }

    private List<Belief<OntClass>> descending(List<Belief<OntClass>> entityConfidences) {
        return null;
    }

    public boolean hasUnrevealedEntity(){
        if (unrevealedEntitiesWithDescendingBelief.isEmpty()){
            return false;
        }
        return true;
    }

    private AgentState initStateGraph() {
        AgentState initState = new AgentState("init") {
            @Override
            public StateMessage task(StateMessage input) {
                // Implement the task logic here
                System.out.println("Processing input: " + input);

                StateMessage output = new StateMessage();
                output.simpleMessage = llm.prompt("hi");;
                return output;
            }

            @Override
            public AgentState nextState(StateMessage input) {
                if (input.simpleMessage.length() > 10){
                    AgentState next =  connectedStates.stream()
                            .filter(state -> state.getName().equals("end"))
                            .findFirst()
                            .orElse(null);
                    return next;
                }
                return null; // For now, return null to indicate no further state transition
            }
        };

        AgentState endState = new AgentState("end") {

            @Override
            public StateMessage task(StateMessage input) {
                return null;
            }

            @Override
            public AgentState nextState(StateMessage input) {
                return null;
            }
        };
        initState.connect(endState);

        return initState;
    }

    public void start(StateMessage input) {
        System.out.println("Starting agent with input: " + input);
        initState.run(input);
    }


    public OntClass nextUnrevealedEntity() {
        if (unrevealedEntitiesWithDescendingBelief.isEmpty()) {
            System.out.println("No unrevealed entities left.");
            return null;
        }
        return unrevealedEntitiesWithDescendingBelief.get(0).obj;
    }

    /**
     * Pairs the given entity with entities from the agent's ontology model.
     * Threshold.
     * Updates the private correspondences.
     *
     * @param entity The entity to pair with.
     * @return A set of paired entities.
     */
    public Set<OntClass> pair(OntClass entity) {
        return null;
    }

    public Set<OntClass> pair(Set<OntClass> entities) {
        return null;
    }

}
