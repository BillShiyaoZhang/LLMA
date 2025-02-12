package cn.edu.xjtlu.iot.syzhang.LLMA;

import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherYAAAJena;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

public class LLMAMatcher extends MatcherYAAAJena {
    private OntologyAgent sourceAgent;
    private OntologyAgent targetAgent;
    @Override
    public Alignment match(OntModel source, OntModel target, Alignment inputAlignment, Properties properties) throws Exception {
        return matchLogic(source, target, false);
    }

    private Alignment matchLogic(OntModel source, OntModel target, boolean isOnline){
        print("Alignment begin.");
        // setup agents, embeddings, database, and openAI
        setup(source, target, isOnline);

        Alignment alignment = new Alignment();
        int alignmentCount = 0;
        int negotiationRound = 0;

        // start alignment
        // if there is at least one agent has unaligned components
        while (!sourceAgent.isFinished() || !targetAgent.isFinished()){
            // if source agent has unaligned components
            if (!sourceAgent.isFinished()) {
                Correspondence correspondence = startNegotiationForOneEntity(sourceAgent, targetAgent);
                if (correspondence != null){
                    alignment.add(correspondence);
                    print("Current alignment count: " + ++alignmentCount + ". Max pair count: 3304. Reference count: 1516.");
                }
                print("Current negotiation round: " + ++negotiationRound + ". Max round: 6048 = 3304 + 2744. Reference round: 1516.");
            }
            // if target agent has unaligned components
            if (!targetAgent.isFinished()) {
                Correspondence correspondence = startNegotiationForOneEntity(targetAgent, sourceAgent);
                if (correspondence != null){
                    alignment.add(correspondence);
                    print("Current alignment count: " + ++alignmentCount + ". Max pair count: 3304. Reference count: 1516.");
                }
                print("Current negotiation round: " + ++negotiationRound + ". Max round: 6048 = 3304 + 2744. Reference round: 1516.");
            }
        }

        // TODO: resolve attack graph
//        Alignment toRemove = removeAttack(alignment, source, target);
//        alignment.removeAll(toRemove);

        // clean database
        clean();

        return alignment;
    }

    public Alignment removeAttack(Alignment alignment, OntModel source, OntModel target){
        Alignment toRemove = new Alignment();
        int count = 0;
        for (Correspondence var1 : alignment){
            for (Correspondence var2: alignment){
                if (var1.equals(var2)){
                    continue;
                }
                boolean flag = false;
                if (var1.getEntityOne().equals(var2.getEntityOne())) {
                    flag = true;
                }
                if (var1.getEntityOne().equals(var2.getEntityTwo())) {
                    flag = true;
                }
                if (var1.getEntityTwo().equals(var2.getEntityOne())) {
                    flag = true;
                }
                if (var1.getEntityTwo().equals(var2.getEntityTwo())) {
                    flag = true;
                }
                if (flag){
                    // TODO: ask GPT which to keep
                    OntClass source1 = null;
                    OntClass target1 = null;
                    OntClass source2 = null;
                    OntClass target2 = null;
                    try {
                        source1 = source.getOntClass(var1.getEntityOne());
                        target1 = target.getOntClass(var1.getEntityTwo());
                    } catch (Exception e){
                        source1 = target.getOntClass(var1.getEntityOne());
                        target1 = source.getOntClass(var1.getEntityTwo());
                    }
                    try {
                        source2 = source.getOntClass(var2.getEntityOne());
                        target2 = target.getOntClass(var2.getEntityTwo());
                    } catch (Exception e){
                        source2 = target.getOntClass(var2.getEntityOne());
                        target2 = source.getOntClass(var2.getEntityTwo());
                    }
                    Correspondence var  = sourceAgent.resolveAttack(source1, target1, source2, target2);
                    toRemove.add(var);
                    count++;
                }
            }
        }
        print("Remove attack count: " + count);
        return toRemove;
    }

    /***
     * setup agents, embeddings, database, and openAI
     * @param source source ontology
     * @param target target ontology
     */
    public void setup(OntModel source, OntModel target, boolean isOnline){
        this.sourceAgent = new OntologyAgent(source, "Source", isOnline);
        this.targetAgent = new OntologyAgent(target, "Target", isOnline);
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
    private Correspondence startNegotiationForOneEntity(OntologyAgent source, OntologyAgent target) {
        print("Start one Negotiation ===================================");
        // source agent pick one unaligned entity
        OntClass entity = source.startNegotiation();
        if (entity == null){
            source.Finish();
            print(source.getCollectionName() + " has no unaligned entity.");
            return null;
        }
        print(source.getCollectionName() + " pick one unaligned entity: " + entity.getLabel(null));

        ArrayList<Double> embedding = source.getEmbedding(entity);

        // target agent find potential alignment
        Set<PotentialCorrespondence> proposedCorrespondences = target.proposeCorrespondence(entity, embedding);
        if (proposedCorrespondences == null){
            print(target.getCollectionName() + " find no potential alignment for " + entity.getLabel(null));
            // mark the entity as negotiated.
            source.markNegotiated(entity);
            return null;
        }

        // target agent ask openAI which one is better.
        PotentialCorrespondence betterCorrespondence = target.whichTargetIsBetter(entity, proposedCorrespondences, null);
        if (betterCorrespondence == null){
            source.markNegotiated(entity);
            print(target.getCollectionName() + " thinks no option is good for " + entity.getLabel(null) + ". It looked at ." + proposedCorrespondences.toString());
            return null;
        }
        print(target.getCollectionName() + " find the better one: " + betterCorrespondence.getTarget().getLabel(null));

        // source agent check proposed correspondences
        PotentialCorrespondence agreement = source.whichTargetIsBetter(entity, proposedCorrespondences, betterCorrespondence.getTarget());
//        PotentialCorrespondence agreement = source.checkProposal(entity, proposedCorrespondences, betterCorrespondence, target);
        if (agreement == null){
            print(source.getCollectionName() + " dont' think any entity proposed by " + target.getCollectionName() + " is good for " + entity.getLabel(null) + ". It looked at ." + betterCorrespondence.getTarget().getLabel(null));
            source.markNegotiated(entity);
            return null;
        }
        print(source.getCollectionName() + " make agreement: " + agreement.getTarget().getLabel(null));

        source.markNegotiated(agreement.getSource());
        target.markNegotiated(agreement.getTarget());
        print("Alignment found: " + agreement.getSource().getLabel(null) + " - " + agreement.getTarget().getLabel(null));
        return new Correspondence(agreement.getSource().getURI(), agreement.getTarget().getURI(), 1, agreement.getRelation());
    }


    private void clean(){
        this.sourceAgent.clean();
        this.targetAgent.clean();
    }

    private static void print(String s){
        System.out.println(s);
    }
}
