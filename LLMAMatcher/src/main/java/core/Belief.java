package core;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;

public class Belief {
    public OntClass entity;
    public int belief;
    public OntModel model;
    public String modelName;

    public Belief(OntClass entity, int belief, OntModel model, String modelName){
        this.entity = entity;
        this.belief = belief;
        this.model = model;
        this.modelName = modelName;
    }

    @Override
    public String toString(){
        return modelName + ", " + entity.getURI() + ", " + belief;
    }
}
