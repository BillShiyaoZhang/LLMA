package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Random;

public class NegotiationGameOverInitCorrespondence extends NegotiationGameOverCorrespondence{
    private String initCorrespondencesPath;

    public NegotiationGameOverInitCorrespondence(Dictionary sourceStringDict, Dictionary targetStringDict, LLMApiCaller apiCaller, double threshold, String initCorrespondencesPath) {
        super(sourceStringDict, targetStringDict, apiCaller, threshold);
        this.initCorrespondencesPath = initCorrespondencesPath;
    }

    @Override
    protected void retrieveCorrespondences() {
        Alignment alignment = Helper.loadInitCorrespondences(initCorrespondencesPath);

        // randomly split the correspondences into two sets, one for each agent
        for (Correspondence c : alignment) {
            if (new Random().nextBoolean()) {
                source.privateCorrespondences.add(c);
            }
            if (new Random().nextBoolean()) {
                target.privateCorrespondences.add(c);
            }
        }
    }

    public static void main(String[] args) {
        Main.initStringDictionaries();
        Main.commonStringsDict.put("threshold", 0.8);
        int iterations = 20;

        FileWriter fw = Helper.createFileWriter(Main.commonStringsDict.get("initCorrespondencesPath").toString()
                + Main.commonStringsDict.get("threshold").toString() + "-statistics.csv");
        for (int i = 0; i < iterations; i++) {
            NegotiationGameOverInitCorrespondence game = new NegotiationGameOverInitCorrespondence(
                    Main.sourceStringsDict, Main.targetStringsDict, null, (double) Main.commonStringsDict.get("threshold"),
                    Main.commonStringsDict.get("initCorrespondencesPath").toString()
                            + Main.commonStringsDict.get("threshold").toString() + ".txt");
            Alignment alignment = game.play();
            compareWithReference(alignment, fw);
        }
        try {
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void compareWithReference(Alignment alignment, FileWriter fw) {
        try {
            Alignment reference = new Alignment(new File(
                    Main.commonStringsDict.get("DataSetRoot").toString() + Main.commonStringsDict.get("dataSet").toString() + "/" +  Main.commonStringsDict.get("reference").toString()));

            int alignmentInReference = 0;
            int referenceInAlignment = 0;
            for (var c : alignment) {
                if (reference.getCorrespondence(c.getEntityTwo(), c.getEntityOne(), CorrespondenceRelation.EQUIVALENCE) != null) {
                    alignmentInReference++;
                }
            }
            for (var c : reference) {
                if (alignment.getCorrespondence(c.getEntityTwo(), c.getEntityOne(), CorrespondenceRelation.EQUIVALENCE) != null) {
                    referenceInAlignment++;
                }
            }

            fw.write(alignmentInReference + ", ");                      // Alignment in reference (TP)
            fw.write((alignment.size() - alignmentInReference) + ", "); // Alignment not in reference (FP)
            fw.write((reference.size() - referenceInAlignment) + "\n"); // Reference not in alignment (FN)
            fw.flush();
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }

    }

}
