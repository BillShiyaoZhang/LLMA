package core;

import cn.edu.xjtlu.iot.syzhang.LLMA.Weaviate;
import org.apache.jena.ontology.OntModel;

public interface Agent {

    public void init(OntModel ontology, Weaviate db, double threshold);

    public String getCurrentState();

    public String getNextAction();

    public void updateState();

    public void addStateActionPair(String state, String action);
}
