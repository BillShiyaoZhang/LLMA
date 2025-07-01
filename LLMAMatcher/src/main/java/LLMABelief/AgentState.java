package LLMABelief;

import java.util.ArrayList;
import java.util.List;

public abstract class AgentState {
    private String name;
    public List<AgentState> connectedStates;

    public AgentState(String name) {
        this.name = name;
        this.connectedStates = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void connect(AgentState state) {
        this.connectedStates.add(state);
        System.out.println("State " + name + " connects to state: " + state.getName());
    }

    public void run(StateMessage input) {
        System.out.println("State " + name + " receives input: " + input.toString());
        StateMessage output = task(input);
        if (output == null) {
            throw new RuntimeException("Output is null.");
        }
        AgentState next = nextState(output);
        if (next != null) {
            next.run(output);
        }
        System.out.println("Ends at state: " + name + " with output: " + output.toString());
    }

    public abstract StateMessage task(StateMessage input);

    public abstract AgentState nextState(StateMessage input);
}
