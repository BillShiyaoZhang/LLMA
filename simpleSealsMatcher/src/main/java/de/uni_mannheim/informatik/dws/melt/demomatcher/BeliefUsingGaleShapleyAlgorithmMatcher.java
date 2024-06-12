package de.uni_mannheim.informatik.dws.melt.demomatcher;

import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherYAAAJena;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;

import java.util.*;

public class BeliefUsingGaleShapleyAlgorithmMatcher extends MatcherYAAAJena {

    private OntModel source;
    private OntModel target;
    private List<OntClass> superiorEntities;
    private List<OntClass> inferiorEntities;

    @Override
    public Alignment match(OntModel source, OntModel target, Alignment inputAlignment, Properties properties) throws Exception {
        return MatchWithBeliefUsingGaleShapleyAlgorithm(source, target);
    }

    public void setup(OntModel source, OntModel target){
        this.source = source;
        this.target = target;

        // TODO: load sorted beliefs to superiorEntities and inferiorEntities.  The size of each can be calculated?

    }

    private Alignment MatchWithBeliefUsingGaleShapleyAlgorithm(OntModel source, OntModel target) {
        print("Alignment begin.");
        setup(source, target);

        // TODO: initiate preferences for all entities.  Using embeddings and LLM?

        Alignment tentatives = new Alignment();
        while(true) {   // before all entities are stable
            // Phase 1 superiorEntities move first
            proposeAndSelect(superiorEntities, inferiorEntities, tentatives);

            // Phase 2 inferiorEntities move first
            proposeAndSelect(inferiorEntities, superiorEntities, tentatives);

            // TODO: break when all entities are stable
            break;
        }
        return tentatives;
    }

    private void proposeAndSelect(List<OntClass> proposers, List<OntClass> acceptors, Alignment tentatives){
        Map<OntClass, List<OntClass>> acceptorWaitingLists = new HashMap<>();
        // each proposer select the best pair to propose to the acceptor
        for (OntClass proposer : proposers){
            // TODO: proposer finds the best pair

            // TODO: put the pair into the acceptor's waiting list
        }

        // each acceptor selects the best proposal received and refuse all other proposals
        for (OntClass acceptor : acceptors){
            // TODO: acceptor finds the best proposal from the waiting list

            // TODO: add the accepted proposal into tentatives
        }

    }

    private static void print(String s){
        System.out.println(s);
    }
}
