package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Dictionary;

public class NegotiationGameOverCorrespondenceLLMSelectStructure extends NegotiationGameOverCorrespondence {
    private String llmShortListedCorrespondencesPathBase;

    public NegotiationGameOverCorrespondenceLLMSelectStructure(
            Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller, String llmShortListedCorrespondencesPath,
            double threshold) {
        super(sourceStringDict, targetStringDict, apiCaller, threshold);
        this.llmShortListedCorrespondencesPathBase = llmShortListedCorrespondencesPath;
    }

    public static void main(String[] args) {
        Main.initStringDictionaries();
        Main.commonStringsDict.put("threshold", 0.7);
        var game = new NegotiationGameOverCorrespondenceLLMSelectStructure(
                Main.sourceStringsDict, Main.targetStringsDict, Main.getAPICaller(),
                "result/" + Main.commonStringsDict.get("dataSet").toString() + "/" +
                        Main.commonStringsDict.get("modelName").toString(),
                (double) Main.commonStringsDict.get("threshold"));
        game.play();
    }

    @Override
    protected void retrieveCorrespondences() {
        retrieveLLMShortListedCorrespondences();
        selectCorrespondencesBasedOnStrucutralInfo();
    }

    private void retrieveLLMShortListedCorrespondences() {
        source.privateCorrespondences = source.loadShortListedCorrespondencesFromFile(
                Main.sourceStringsDict.get("llmSelectedCorrespondencesPath").toString() +
                Main.commonStringsDict.get("threshold").toString() + "-formated.txt");
        target.privateCorrespondences = target.loadShortListedCorrespondencesFromFile(
                Main.targetStringsDict.get("llmSelectedCorrespondencesPath").toString() +
                        Main.commonStringsDict.get("threshold").toString() + "-formated.txt");
    }

    private void selectCorrespondencesBasedOnStrucutralInfo() {
        agentSelectCorrespondencesBasedOnStrucutralInfo(source, target);
        agentSelectCorrespondencesBasedOnStrucutralInfo(target, source);
    }

    private void agentSelectCorrespondencesBasedOnStrucutralInfo(Agent source, Agent target) {
        FileWriter fw = Helper.createFileWriter(Main.commonStringsDict.get("dataSetResultBase").toString() +
                Main.commonStringsDict.get("modelName").toString() + "/" + source.name +
                "/llm_selected-structural/llm_selected-structural-" + Main.commonStringsDict.get("threshold").toString() +
                ".txt", true);
        int i = 1;
        for (Correspondence c : source.privateCorrespondences) {
            String entityUri = c.getEntityOne();
            String entityVerbose = getNeighborhoodVerbose(entityUri, source);
            String targetVerbose = "";
            for (Correspondence targetC : target.privateCorrespondences) {
                if (targetC.getEntityOne().equals(entityUri)) {
                    targetVerbose = targetVerbose + targetC.getEntityTwo() + "\n" + getNeighborhoodVerbose(targetC.getEntityTwo(), target) + "\n";
                }
            }
            String message =
                    "You are an assistant helping to select relevant entity pairs for alignment.\n" +
                    "You have been provided with an entity, with its parent and children if applicable, from one ontology, " +
                            "and a set of entities, each with its parent and children if applicable, from another ontology.\n" +
                    "Your task is to prioritize the relevance of entities of another ontology from high to low " +
                            "based on the provided contents.\n\n" +
                    "Entity from source ontology:\n" + entityVerbose + "\n\n" +
                    "Entities from target ontology:\n" + targetVerbose + "\n\n" +
                    "Please select all possible entities, from the given set, you find likely to be aligned to the given entity.\n" +
                    "Provide your response in the following format:\n" +
                    "<Possible Entity URI>\n" +
                    "<Possible Entity URI>\n" +
                    "<Possible Entity URI>\n" +
                    "...\n\n" +
                    "If you do not find any relevant entity, please respond with 'No relevant entity found'.\n";
            String response = source.llm.prompt(message);
            String formatedResponse = source.llm.getUrisOnlyFromStringForThinkingModel(response);
            try {
                System.out.println(i++ + entityUri);
                fw.write(entityUri + "\n" + formatedResponse + "\n\n");
                fw.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
            // Save the selected correspondences to a file.
    }

    /***
     * Get the entities in its neighborhood with distance = 1.
     * Format a verbose String that describes the selected entity and the entities in the neighborhood.
     * @param uri URI of the entity to describe
     * @param agent Agent that contains the ontology and entity verbose
     * @return A verbose String that describes the entity and its neighborhood
     */
    private String getNeighborhoodVerbose(String uri, Agent agent) {
        StringBuilder verbose = new StringBuilder(agent.entityVerbos.get(uri));
        OntClass ontClass = agent.ontology.getOntClass(uri);
        for (OntClass superClass : ontClass.listSuperClasses().toList()) {
            if (!superClass.isURIResource()) {
                continue; // Skip blank nodes
            }
            verbose.append("  The super class of this entity is: `\n");
            verbose.append(agent.entityVerbos.get(superClass.getURI()));
        }
        for (OntClass subClass : ontClass.listSubClasses().toList()) {
            if (!subClass.isURIResource()) {
                continue; // Skip blank nodes
            }
            verbose.append("  The sub classes of this entity are:\n");
            verbose.append(agent.entityVerbos.get(subClass.getURI()));
        }
        return verbose.toString();
    }
}
