package core;

import cn.edu.xjtlu.iot.syzhang.LLMA.OntClassHelper;
import cn.edu.xjtlu.iot.syzhang.LLMA.Weaviate;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.jeasy.states.api.*;
import org.jeasy.states.core.FiniteStateMachineBuilder;
import org.jeasy.states.core.TransitionBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlignmentAgent extends AbstractAgent{
    private String name;
    private String currentState;
    private static final double SIMILARITY_THRESHOLD = 0.95;
    private HashMap<String, Double> beliefMap;
    private Set<State> states;
    private FiniteStateMachine fsm;

    public boolean testState = true;

    public static void main(String[] args) throws FiniteStateMachineException {
        Set<State> states;
        FiniteStateMachine fsm;
        AlignmentAgent agent = new AlignmentAgent("Agent1");
        agent.stateMachineInit();
        OntModel humanKnowledge = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        humanKnowledge.read("simpleSealsMatcher/src/main/java/DataSet/human.owl");
        agent.init(humanKnowledge, new Weaviate("source"), SIMILARITY_THRESHOLD);

        agent.updateBeliefMap("method");
        for(Map.Entry<String, Double> entry : agent.beliefMap.entrySet()){
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }


//        System.out.println("Turnstile initial state : " + fsm.getCurrentState().getName() + ", testState: " + agent.testState);
//
//        fsm.fire(new Join());
//        System.out.println("Turnstile state : " + fsm.getCurrentState().getName() + ", testState: " + agent.testState);
    }



    public void stateMachineInit(){
        State initial = new State("Initial");
        State waiting = new State("Waiting");
        State ready = new State("Ready");
        State proposing = new State("Proposing");
        State accepting = new State("Accepting");
        State finished = new State("Finished");

        states = new HashSet<>();
        states.add(initial);
        states.add(waiting);
        states.add(ready);
        states.add(proposing);
        states.add(accepting);
        states.add(finished);

        class Join extends AbstractEvent{}
        class Pick extends AbstractEvent{}
        class Propose extends AbstractEvent{}
        class Decide extends AbstractEvent{}

        Transition t1 = new TransitionBuilder()
                .name("t1")
                .sourceState(initial)
                .eventType(Join.class)
                .eventHandler(new EventHandler<Event>() {
                    @Override
                    public void handleEvent(Event event) throws Exception {

                    }
                })
                .targetState(waiting)
                .build();

        Transition t2 = new TransitionBuilder()
                .name("t2")
                .sourceState(waiting)
                .eventType(Pick.class)
                .eventHandler(new EventHandler<Event>() {
                    @Override
                    public void handleEvent(Event event) throws Exception {

                    }
                })
                .targetState(ready)
                .build();

        Transition t3 = new TransitionBuilder()
                .name("t3")
                .sourceState(ready)
                .eventType(Propose.class)
                .eventHandler(new EventHandler<Event>() {
                    @Override
                    public void handleEvent(Event event) throws Exception {

                    }
                })
                .targetState(proposing)
                .build();

        Transition t4 = new TransitionBuilder()
                .name("t4")
                .sourceState(proposing)
                .eventType(Decide.class)
                .eventHandler(new EventHandler<Event>() {
                    @Override
                    public void handleEvent(Event event) throws Exception {

                    }
                })
                .targetState(accepting)
                .build();

        fsm = new FiniteStateMachineBuilder(states, initial)
                .registerTransition(t1)
                .registerTransition(t2)
                .registerTransition(t3)
                .registerTransition(t4)
                .build();
    }

    private HashMap<String, String> StateActionMap;

    public AlignmentAgent(String name){
        this.name = name;
    }



    @Override
    public void init(OntModel ontology, Weaviate db, double threshold) {
        super.init(ontology, db, threshold);
        this.currentState = "";
        this.StateActionMap = new HashMap<>();
        this.beliefMap = new HashMap<>();
        for (OntClass entity : this.getOntologyKnowledge().listClasses().toList()){
            this.beliefMap.put(entity.getURI(), 1.0);
        }
    }

    public void updateBeliefMap(String method){
        switch (method){
            case "simple":
                for (OntClass entity : this.getOntologyKnowledge().listClasses().toList()){
                    double belief = this.beliefMap.get(entity.getURI());
                    int belongedEntity = calculateNearbyClasses(entity, 2);
                    belief = 1 / (1 + Math.exp(-0.1 * (belongedEntity - 50)));
                    this.updateBelief(entity.getURI(), belief);
                }
                break;
            default:
                break;
        }
    }

    private static int calculateNearbyClasses(OntClass entity, int depth){
        // calculate the number of all subclasses and their subclasses
        return OntClassHelper.getNearbyClasses(depth, entity).size();
    }

    private void updateBelief(String entityURI, double belief){
        this.beliefMap.put(entityURI, belief);
    }

    @Override
    public String getCurrentState() {
        return currentState;
    }

    @Override
    public String getNextAction() {
        if(this.StateActionMap.containsKey(this.currentState)){
            return this.StateActionMap.get(this.currentState);
        } else {
            return "NaN";
        }
    }

    @Override
    public void updateState() {

    }

    @Override
    public void addStateActionPair(String state, String action) {

    }
}
