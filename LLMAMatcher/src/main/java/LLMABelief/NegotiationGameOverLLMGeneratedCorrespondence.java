package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntModel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Dictionary;

public class NegotiationGameOverLLMGeneratedCorrespondence extends NegotiationGameOverCorrespondence{
    private String initCorrespondencesPath;

    public NegotiationGameOverLLMGeneratedCorrespondence(
            Dictionary sourceStringDict, Dictionary targetStringDict, String modelName, String initCorrespondencesPath) {
        super(sourceStringDict, targetStringDict, modelName);
        this.initCorrespondencesPath = initCorrespondencesPath;
    }

    /***
     * Calculate the beliefs for all possible correspondences.
     * Store correspondences that LLM agrees on.
     * This normally would lead to two different sets of correspondences, one for each agent, as the LLM will select
     * the correspondences that it believes are relevant based on the ontology each agent has.
     */
    @Override
    protected void retrieveCorrespondences() {
        Alignment alignment = loadCorrespondences(initCorrespondencesPath);
        source.selectCorrespondences(alignment, true, target.entityVerbos);
        target.selectCorrespondences(alignment, false, source.entityVerbos);
    }

    private Alignment loadCorrespondences(String initCorrespondencesPath) {
        if (initCorrespondencesPath == null || initCorrespondencesPath.isEmpty()) {
            System.err.println("No initial correspondences path provided.");
            return null;
        }
        System.out.println("Loading initial correspondences from: " + initCorrespondencesPath);
        Alignment alignment = new Alignment();
        try (BufferedReader reader = new BufferedReader(new FileReader(initCorrespondencesPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // skip empty lines
                }
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    System.err.println("Invalid correspondence format: " + line);
                    continue; // skip invalid lines
                }
                String sourceEntity = parts[0].trim();
                String targetEntity = parts[1].trim();
                double confidence = Double.parseDouble(parts[2].trim());
                Correspondence c = new Correspondence(sourceEntity, targetEntity, confidence);
                alignment.add(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return alignment;
    }
}
