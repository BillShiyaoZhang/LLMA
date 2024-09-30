package core;

import de.uni_mannheim.informatik.dws.melt.demomatcher.Weaviate;
import org.apache.jena.ontology.OntModel;

import java.util.HashMap;

public class AlignmentAgent extends AbstractAgent{
    private String currentState;

    private HashMap<String, String> StateActionMap;

    @Override
    public void init(OntModel ontology, Weaviate db, double threshold) {
        super.init(ontology, db, threshold);
        this.currentState = "";
        this.StateActionMap = new HashMap<>();
    }

    @Override
    public String getCurrentState() {
        return "";
    }

    @Override
    public String getNextAction() {
        return "";
    }

    @Override
    public void updateState() {

    }

    @Override
    public void addStateActionPair(String state, String action) {

    }
}
