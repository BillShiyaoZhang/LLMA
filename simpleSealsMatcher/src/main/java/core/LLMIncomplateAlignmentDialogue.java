package core;

import de.uni_mannheim.informatik.dws.melt.demomatcher.OntologyAgent;
import de.uni_mannheim.informatik.dws.melt.demomatcher.PotentialCorrespondence;
import de.uni_mannheim.informatik.dws.melt.demomatcher.Weaviate;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.jeasy.states.api.*;
import org.jeasy.states.core.TransitionBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LLMIncomplateAlignmentDialogue extends AbstractDialogue {

    private Alignment eventualAlignment;
    private HashMap<String, AlignmentAgent> participants;
    private int alignmentCount = 0;
    private int negotiationRound = 0;


    @Override
    public void start() {
//        // start alignment
//        // if there is at least one agent has unaligned components
//        while (participants.get("Agent1").getCurrentState().equals("Finished") || participants.get("Agent2").getCurrentState().equals("Finished")){
//            // if source agent has unaligned components
//            if (!participants.get("Agent1").getCurrentState().equals("Finished")) {
//                Correspondence correspondence = startNegotiationForOneEntity(participants.get("Agent1"), participants.get("Agent2"));
//                if (correspondence != null){
//                    eventualAlignment.add(correspondence);
////                    print("Current alignment count: " + ++alignmentCount + ". Max pair count: 3304. Reference count: 1516.");
//                }
////                print("Current negotiation round: " + ++negotiationRound + ". Max round: 6048 = 3304 + 2744. Reference round: 1516.");
//            }
//            // if target agent has unaligned components
//            if (!participants.get("Agent2").getCurrentState().equals("Finished")) {
//                Correspondence correspondence = startNegotiationForOneEntity(participants.get("Agent2"), participants.get("Agent1"));
//                if (correspondence != null){
//                    eventualAlignment.add(correspondence);
////                    print("Current alignment count: " + ++alignmentCount + ". Max pair count: 3304. Reference count: 1516.");
//                }
////                print("Current negotiation round: " + ++negotiationRound + ". Max round: 6048 = 3304 + 2744. Reference round: 1516.");
//            }
//        }
//
//        // TODO: resolve attack graph
////        Alignment toRemove = removeAttack(alignment, source, target);
////        alignment.removeAll(toRemove);
//
//        // clean database
//
//
    }

    /***
     * Start negotiation.
     * Source agent pick one unaligned entity, target agent find potential alignment.
     * If there are potential alignments, source and target agent discuss which one is the best in turn,
     * with proposing new suitable components.
     * @param source source agent
     * @param target target agent
     * @return The agreed correspondence.
     */
//    private Correspondence startNegotiationForOneEntity(AlignmentAgent source, AlignmentAgent target) {
////        print("Start one Negotiation ===================================");
//        // source agent pick one unaligned entity
//        OntClass entity = source.startNegotiation();
//        if (entity == null){
//            source.Finish();
////            print(source.getCollectionName() + " has no unaligned entity.");
//            return null;
//        }
////        print(source.getCollectionName() + " pick one unaligned entity: " + entity.getLabel(null));
//
//        // TODO: make the getEmbedding(entity) method public static.
//        ArrayList<Double> embedding = source.getEmbedding(entity);
//
//        // target agent find potential alignment
//        Set<PotentialCorrespondence> proposedCorrespondences = target.proposeCorrespondence(entity, embedding);
//        if (proposedCorrespondences == null){
////            print(target.getCollectionName() + " find no potential alignment for " + entity.getLabel(null));
//            // mark the entity as negotiated.
//            source.markNegotiated(entity);
//            return null;
//        }
//
//        // target agent ask openAI which one is better.
//        PotentialCorrespondence betterCorrespondence = target.whichTargetIsBetter(entity, proposedCorrespondences, null);
//        if (betterCorrespondence == null){
//            source.markNegotiated(entity);
////            print(target.getCollectionName() + " thinks no option is good for " + entity.getLabel(null) + ". It looked at ." + proposedCorrespondences.toString());
//            return null;
//        }
////        print(target.getCollectionName() + " find the better one: " + betterCorrespondence.getTarget().getLabel(null));
//
//        // source agent check proposed correspondences
//        PotentialCorrespondence agreement = source.whichTargetIsBetter(entity, proposedCorrespondences, betterCorrespondence.getTarget());
////        PotentialCorrespondence agreement = source.checkProposal(entity, proposedCorrespondences, betterCorrespondence, target);
//        if (agreement == null){
////            print(source.getCollectionName() + " dont' think any entity proposed by " + target.getCollectionName() + " is good for " + entity.getLabel(null) + ". It looked at ." + betterCorrespondence.getTarget().getLabel(null));
//            source.markNegotiated(entity);
//            return null;
//        }
////        print(source.getCollectionName() + " make agreement: " + agreement.getTarget().getLabel(null));
//
//        source.markNegotiated(agreement.getSource());
//        target.markNegotiated(agreement.getTarget());
////        print("Alignment found: " + agreement.getSource().getLabel(null) + " - " + agreement.getTarget().getLabel(null));
//        return new Correspondence(agreement.getSource().getURI(), agreement.getTarget().getURI(), 1, agreement.getRelation());
//    }


    @Override
    public void end() {

    }

    @Override
    public void init() {
        participants = new HashMap<String, AlignmentAgent>();
        participants.put("Agent1", new AlignmentAgent("Agent1"));
        participants.put("Agent2", new AlignmentAgent("Agent2"));

        OntModel humanKnowledge = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        humanKnowledge.read("simpleSealsMatcher/src/main/java/DataSet/human.owl");
        OntModel mouseKnowledge = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        mouseKnowledge.read("simpleSealsMatcher/src/main/java/DataSet/mouse.owl");

        String collectionName1 = "source";
        String collectionName2 = "target";

        Weaviate db1 = new Weaviate(collectionName1);
        Weaviate db2 = new Weaviate(collectionName2);

        participants.get("Agent1").init(humanKnowledge, db1, 0.95);
        participants.get("Agent2").init(mouseKnowledge, db2, 0.95);

        eventualAlignment = new Alignment();
    }

    @Override
    public void getStatus() {

    }

    @Override
    public void getParticipants() {

    }

    @Override
    public String getDialogueContent(int index) {
        return "";
    }

    @Override
    public void addDialogueContent(String content) {

    }

    @Override
    public void addDialogueContent(String content, int index) {

    }
}
