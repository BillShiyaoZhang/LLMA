package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class NegotiationGameOverEntityEntropy extends NegotiationGameOverCorrespondence {

    private EntropyType entropyType;

    public NegotiationGameOverEntityEntropy(Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller) {
        super(sourceStringDict, targetStringDict, apiCaller);
        entropyType = EntropyType.ORIGINAL_ENTROPY;
    }

    @Override
    protected void retrieveCorrespondences(){
        // load private correspondences from potential_pairs for each agent
        Dictionary<String, Set<Belief<String>>> potentialEntityPairsS = Agent.loadPotentialEntityPairsFromFile(
                Main.commonStringsDict.get("potentiCorrespondencesPath").toString() +
                        Main.commonStringsDict.get("threshold")+ "-" + source.name + ".txt");
        source.privateCorrespondences = toAlignment(potentialEntityPairsS, true);
        Dictionary<String, Set<Belief<String>>> potentialEntityPairsT = Agent.loadPotentialEntityPairsFromFile(
                Main.commonStringsDict.get("potentiCorrespondencesPath").toString() +
                        Main.commonStringsDict.get("threshold")+ "-" + source.name + ".txt");
        target.privateCorrespondences = toAlignment(potentialEntityPairsT, false);

        calculateStructuralEntropy(source.entityBeliefs, entropyType);
        calculateStructuralEntropy(target.entityBeliefs, entropyType);
    }

    private Alignment toAlignment(Dictionary<String, Set<Belief<String>>> potentialEntityPairs, boolean isSource) {
        Alignment alignment = new Alignment();
        for (String selfURI : ((Hashtable<String, Set<Belief<String>>>) potentialEntityPairs).keySet()) {
            Set<Belief<String>> beliefs = potentialEntityPairs.get(selfURI);
            for (Belief<String> belief : beliefs) {
                String entityTwo = belief.obj;
                double confidence = belief.value;
                Correspondence c;
                if (isSource) {
                    c = new Correspondence(selfURI, entityTwo, confidence);
                } else {
                    c = new Correspondence(entityTwo, selfURI, confidence);
                }
                alignment.add(c);
            }
        }
        return alignment;
    }

    public static void main(String[] args) {
        Main.initStringDictionaries();
        Main.commonStringsDict.put("threshold", 0.9);

        NegotiationGameOverEntityEntropy game = new NegotiationGameOverEntityEntropy(Main.sourceStringsDict,
                Main.targetStringsDict, null);
        Alignment alignment = game.play();

        String path = "result/" + Main.commonStringsDict.get("dataSet").toString() + "/entropy/" +
                game.entropyType.toString() + Main.commonStringsDict.get("threshold").toString();
        FileWriter fw = Helper.createFileWriter(path + ".txt", true);
        try {
            for (Correspondence c : alignment) {
                fw.write(c.getEntityOne() + ", " + c.getEntityTwo() + ", " + c.getConfidence() + "\n");
                fw.flush();
            }
            fw.flush();
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Main.compareWithReference(alignment, path + "-statistics.txt");
    }

    private void calculateStructuralEntropy(List<Belief<OntClass>> entityBeliefs, EntropyType entropyType) {
        for (Belief<OntClass> belief : entityBeliefs) {
            OntClass entity = belief.obj;
            int superClassCount = entity.listSuperClasses().toList().size();
            int subClassCount = entity.listSubClasses().toList().size();
            int equivalentClassCount = entity.listEquivalentClasses().toList().size();
            int disjointClassCount = entity.listDisjointWith().toList().size();
            int relatedSynonymCount = entity.getOntModel().getResource(entity.getURI()).listProperties().toList().size();
            int propertiesCount = entity.listDeclaredProperties().toList().size();

            Set<Resource> uniquePredicates = new HashSet<>();
            StmtIterator iter = entity.getOntModel().listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement(); // 获取下一条语句
                Property predicate = stmt.getPredicate(); // 获取该语句的谓语
                uniquePredicates.add(predicate); // 将谓语添加到 Set 中
            }
            int totalUniqueEdges = uniquePredicates.size();
            long totalEdgeCount = entity.getOntModel().size();

            double entropy = 0;
            switch(entropyType) {
                case ORIGINAL_ENTROPY:
                    entropy = originalEntropy(superClassCount, subClassCount, equivalentClassCount,
                            disjointClassCount, relatedSynonymCount, propertiesCount, totalEdgeCount);
                    break;
                case LANGUAGE_ENTROPY:
                    entropy = languageEntropy(superClassCount, subClassCount, equivalentClassCount,
                            disjointClassCount, relatedSynonymCount, propertiesCount);
                    break;
                case DOMAIN_ENTROPY:
                    entropy = domainEntropy(superClassCount, subClassCount, equivalentClassCount,
                            disjointClassCount, relatedSynonymCount, propertiesCount);
                    break;
                case IMPROVED_ENTROPY:
                    entropy = improvedEntropy(superClassCount, subClassCount, equivalentClassCount,
                            disjointClassCount, relatedSynonymCount, propertiesCount);
                    break;
            }
            belief.value = entropy;
        }
    }

    // (Calmet & Daemi, 2004)
    private double originalEntropy(int superClassCount, int subClassCount, int equivalentClassCount,
                                   int disjointClassCount, int relatedSynonymCount, int propertiesCount, long totalEdgeCount) {
        int edgeCount = superClassCount + subClassCount + equivalentClassCount + disjointClassCount +
                relatedSynonymCount + propertiesCount;
        double amplifier = Math.pow(10, Math.floor(Math.log10(totalEdgeCount)));
        double entropy = (amplifier * edgeCount) / totalEdgeCount;
        return entropy;
    }

    // (Doran et. al, 2008)
    private double languageEntropy(int superClassCount, int subClassCount, int equivalentClassCount,
                                   int disjointClassCount, int relatedSynonymCount, int propertiesCount) {
        return 0;
    }

    // (Doran et. al, 2008)
    private double domainEntropy(int superClassCount, int subClassCount, int equivalentClassCount,
                                 int disjointClassCount, int relatedSynonymCount, int propertiesCount) {
        return 0;
    }

    // (Doran et. al, 2008)
    private double improvedEntropy(int superClassCount, int subClassCount, int equivalentClassCount, int disjointClassCount, int relatedSynonymCount, int propertiesCount) {
        return languageEntropy(superClassCount, subClassCount, equivalentClassCount, disjointClassCount, relatedSynonymCount, propertiesCount) +
                domainEntropy(superClassCount, subClassCount, equivalentClassCount, disjointClassCount, relatedSynonymCount, propertiesCount);
    }

    public enum EntropyType { ORIGINAL_ENTROPY, LANGUAGE_ENTROPY, DOMAIN_ENTROPY, IMPROVED_ENTROPY }
}
