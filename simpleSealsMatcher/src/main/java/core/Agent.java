package core;

import de.uni_mannheim.informatik.dws.melt.demomatcher.Weaviate;
import org.apache.jena.ontology.OntModel;

public interface Agent {

    public void init(OntModel ontology, Weaviate db, double threshold);

    public String getCurrentState();

    public String getNextAction();

    public void updateState();

    public void addStateActionPair(String state, String action);
}
