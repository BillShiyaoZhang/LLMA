package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntModel;

public class OntologyBeliefAgent {
    private OntModel ontology;
    private Alignment alignment;
    private LLM llm;
    // embedding db
    private OntologyBeliefAgent opponent;

    public void start(OntModel ontology, Alignment alignment, LLM llm, OntologyBeliefAgent opponent) {
        this.alignment = alignment;
        this.ontology = ontology;
        this.llm = llm;
        // embedding db
        this.opponent = opponent;
    }

    private void init(){
        // init embedding db

        // calculate distance for each entity
         

    }















}
