package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherYAAAJena;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntModel;

import java.util.Properties;

public class LLMABeliefMatcher extends MatcherYAAAJena {
    private OntologyBeliefAgent sourceAgent;
    private OntologyBeliefAgent targetAgent;
    private LLM llm;
    // embedding db


    @Override
    public Alignment match(OntModel source, OntModel target, Alignment inputAlignment, Properties properties) throws Exception {
        setupEnvironment();

        Alignment outputAlignment = conductAlignment(source, target, llm, inputAlignment);

        return outputAlignment;
    }

    private Alignment conductAlignment(OntModel source, OntModel target, LLM llm, Alignment inputAlignment) {
        sourceAgent.start(source, inputAlignment, llm, targetAgent);
        targetAgent.start(target, inputAlignment, llm, sourceAgent);

        return inputAlignment;
    }

    private void setupEnvironment() {
        this.sourceAgent = new OntologyBeliefAgent();
        this.targetAgent = new OntologyBeliefAgent();
        this.llm = LLM.getInstance();
        // embedding db
    }
}
