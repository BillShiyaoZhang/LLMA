package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.apache.jena.ontology.OntClass;

import java.io.*;
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
        Alignment alignment = game.play();
        FileWriter fw = Helper.createFileWriter("result/" + Main.commonStringsDict.get("dataSet").toString() + "/" +
                        Main.commonStringsDict.get("modelName").toString() + "/alignment-llm_selected-structural-" +
                        Main.commonStringsDict.get("threshold") + ".txt",
                true);
        for (Correspondence c : alignment) {
            try {
                fw.write(c.getEntityOne() + " " + c.getEntityTwo() + " " + c.getConfidence() + "\n");
                fw.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
        agentSelectCorrespondencesBasedOnStrucutralInfo(source, target, true);
        agentSelectCorrespondencesBasedOnStrucutralInfo(target, source, false);
    }

    private void agentSelectCorrespondencesBasedOnStrucutralInfo(Agent source, Agent target, boolean isSource) {
        String filePath = Main.commonStringsDict.get("dataSetResultBase").toString() +
                Main.commonStringsDict.get("modelName").toString() + "/" + source.name +
                "/llm_selected-structural/llm_selected-structural-" + Main.commonStringsDict.get("threshold").toString() +
                ".txt";

        Alignment remains = new Alignment();
        for (Correspondence c : source.privateCorrespondences) {
            remains.add(c);
        }

        int i = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("http://" + source.name.toLowerCase())) {
                    String uri = line.trim();
                    // Check if the URI is already in the alignment
                    if (remains.getCorrespondencesSource(uri) != null) {
                        remains.removeCorrespondencesSource(uri);
                        i++;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // File does not exist, we will create it.
            System.out.println("File not found, creating new file: " + filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileWriter fw = Helper.createFileWriter(filePath, true);
        for (Correspondence c : remains) {  // c = <source entity, target entity, confidence>
            String entityUri;
            if (isSource) {
                entityUri = c.getEntityOne();
            } else {
                entityUri = c.getEntityTwo();
            }
            String entityVerbose = getNeighborhoodVerbose(entityUri, source);
            String targetVerbose = "";
            for (Correspondence targetC : target.privateCorrespondences) {  // targetC = <source entity, target entity, confidence>
                if (isSource) {
                    if (targetC.getEntityOne().equals(entityUri)) {
                        targetVerbose = targetVerbose + targetC.getEntityTwo() + "\n" + getNeighborhoodVerbose(targetC.getEntityTwo(), target) + "\n";
                    }
                } else {
                    if (targetC.getEntityTwo().equals(entityUri)) {
                        targetVerbose = targetVerbose + targetC.getEntityOne() + "\n" + getNeighborhoodVerbose(targetC.getEntityOne(), target) + "\n";
                    }
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
