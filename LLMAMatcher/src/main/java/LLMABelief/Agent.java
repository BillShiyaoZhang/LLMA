package LLMABelief;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;

import java.util.Set;

public class Agent {
    private QwenApiCaller llm;
    private OntModel ontModel;
    private AgentState initState;

    public Agent(OntModel ontModel, String modelName) {
        this.llm = new QwenApiCaller(modelName);
        this.ontModel = ontModel;
        this.initState = initStateGraph();
    }

    public boolean hasEntityLeft(){
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


    public OntClass selectEntityForPairing() {
        return null;
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
