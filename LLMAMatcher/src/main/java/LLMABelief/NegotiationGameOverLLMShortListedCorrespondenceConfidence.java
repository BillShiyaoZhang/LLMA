package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Dictionary;

public class NegotiationGameOverLLMShortListedCorrespondenceConfidence extends NegotiationGameOverLLMShortListedCorrespondence {
    private NegotiationGameOverEntityEntropy.EntropyType entropyType = NegotiationGameOverEntityEntropy.EntropyType.ORIGINAL_ENTROPY;

    public NegotiationGameOverLLMShortListedCorrespondenceConfidence(Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller, String initCorrespondencesPath, NegotiationGameOverEntityEntropy.EntropyType entropyType) {
        super(sourceStringDict, targetStringDict, apiCaller, initCorrespondencesPath);
        this.entropyType = entropyType;
    }

    public static void main(String[] args) {
        Main.initStringDictionaries();
        Main.commonStringsDict.put("threshold", 0.9);
        NegotiationGameOverEntityEntropy.EntropyType entropyType = NegotiationGameOverEntityEntropy.EntropyType.ORIGINAL_ENTROPY;

        NegotiationGameOverLLMShortListedCorrespondenceConfidence game = new NegotiationGameOverLLMShortListedCorrespondenceConfidence(
                Main.sourceStringsDict, Main.targetStringsDict, null,
                Main.commonStringsDict.get("initCorrespondencesPath").toString()
                        + Main.commonStringsDict.get("threshold").toString() + ".txt", entropyType);
        Alignment alignment = game.play();

        String path = "result/" + Main.commonStringsDict.get("dataSet").toString() + "/" +
                Main.commonStringsDict.get("modelName").toString() + "/entropy/alignment-short_listed" +
                entropyType.toString() + Main.commonStringsDict.get("threshold").toString();
        FileWriter fw = Helper.createFileWriter(path + ".txt");
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

    @Override
    protected void modifyCorrespondenceConfidencesBeforeNegotiation() {
        Alignment sourceConfidence = loadTriplesFromFile("result/" + Main.commonStringsDict.get("dataSet").toString() +
                "/entropy/correspondence_confidence-" + source.name + "-" + this.entropyType.toString() +
                Main.commonStringsDict.get("threshold").toString() + ".txt");
        Alignment targetConfidence = loadTriplesFromFile("result/" + Main.commonStringsDict.get("dataSet").toString() +
                "/entropy/correspondence_confidence-" + target.name + "-" + this.entropyType.toString() +
                Main.commonStringsDict.get("threshold").toString() + ".txt");

        modifyCorrespondenceConfidences(source.privateCorrespondences, sourceConfidence);
        modifyCorrespondenceConfidences(target.privateCorrespondences, targetConfidence);
    }

    private Alignment loadTriplesFromFile(String filePath) {
        Alignment alignment = new Alignment();

        try (BufferedReader reader = new BufferedReader(
                new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // skip empty lines
                }
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    continue; // skip lines that do not have enough parts
                }
                String entityOne = parts[0].trim();
                String entityTwo = parts[1].trim();
                double confidence;
                try {
                    confidence = Double.parseDouble(parts[2].trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid confidence in line: " + line);
                    continue; // skip lines with invalid confidence values
                }
                alignment.add(entityOne, entityTwo, confidence);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return alignment;
    }

    private void modifyCorrespondenceConfidences(Alignment correspondences, Alignment confidences) {
        for (Correspondence c : correspondences) {
            Correspondence confidence = confidences.getCorrespondence(c.getEntityOne(), c.getEntityTwo(), CorrespondenceRelation.EQUIVALENCE);
            if (confidence != null) {
                double newConfidence = 0.3 * confidence.getConfidence() + c.getConfidence();
                c.setConfidence(newConfidence);
            } else {
                System.out.println("Correspondence " + c.getEntityOne() + ", " + c.getEntityTwo() + " not found.");
            }
        }
    }
}
