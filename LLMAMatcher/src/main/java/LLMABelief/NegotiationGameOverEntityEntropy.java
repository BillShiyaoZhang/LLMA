package LLMABelief;

import org.apache.jena.ontology.OntClass;

import java.util.Dictionary;
import java.util.List;

public class NegotiationGameOverEntityEntropy extends NegotiationGameOverCorrespondence {

    private EntropyType entropyType;

    public NegotiationGameOverEntityEntropy(Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller) {
        super(sourceStringDict, targetStringDict, apiCaller);
        entropyType = EntropyType.ORIGINAL_ENTROPY;
    }

    @Override
    protected void retrieveCorrespondences(){
        calculateStructuralEntropy(source.entityBeliefs, entropyType);
        calculateStructuralEntropy(target.entityBeliefs, entropyType);
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

            switch(entropyType) {
                case ORIGINAL_ENTROPY:
                    belief.value = originalEntropy(superClassCount, subClassCount, equivalentClassCount,
                            disjointClassCount, relatedSynonymCount, propertiesCount);
                    break;
                case LANGUAGE_ENTROPY:
                    belief.value = languageEntropy(superClassCount, subClassCount, equivalentClassCount,
                            disjointClassCount, relatedSynonymCount, propertiesCount);
                    break;
                case DOMAIN_ENTROPY:
                    belief.value = domainEntropy(superClassCount, subClassCount, equivalentClassCount,
                            disjointClassCount, relatedSynonymCount, propertiesCount);
                    break;
                case IMPROVED_ENTROPY:
                    belief.value = improvedEntropy(superClassCount, subClassCount, equivalentClassCount,
                            disjointClassCount, relatedSynonymCount, propertiesCount);
                    break;
            }
        }
    }

    // (Calmet & Daemi, 2004)
    private double originalEntropy(int superClassCount, int subClassCount, int equivalentClassCount,
                                   int disjointClassCount, int relatedSynonymCount, int propertiesCount) {
        return 0;
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
