package cn.edu.xjtlu.iot.syzhang.LLMABelief;

import LLMABelief.Agent;
import LLMABelief.Main;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Statistics {
    public static void main(String[] args) {
        Alignment reference;
        try {
            reference = new Alignment(new File("src/main/java/DataSet/Anatomy/reference.rdf"));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        statistics(reference);
    }

    private static void statistics(Alignment reference) {
        calculateStatistics(reference, 0.9);
        calculateStatistics(reference, 0.8);
        calculateStatistics(reference, 0.7);
        calculateStatistics(reference, 0.6);
        calculateStatistics(reference, 0.5);
    }

    private static void calculateStatistics(Alignment reference, double threshold) {
        System.out.println("=========================================");
        System.out.println("Statistics for potential pairs with threshold: " + threshold);
        System.out.println("Total correspondences: " + reference.size());

        potentialPairsToAlignment(reference, threshold);
        llmSelectedPairsToAlignment(reference, threshold);

    }

    private static void llmSelectedPairsToAlignment(Alignment reference, double threshold) {
        Map<String, Set<String>> potentialPairsH = loadLLMSelectedPairs(
                Main.humanStringsDict.get("llmSelectedCorrespondencesPath").toString() + threshold +"-formated.txt");
        Map<String, Set<String>> potentialPairsM = loadLLMSelectedPairs(
                Main.mouseStringsDict.get("llmSelectedCorrespondencesPath").toString() + threshold + "-formated.txt");

        System.out.println("LLM selected pairs -----");
        compareStatics(potentialPairsH, potentialPairsM, reference);
    }

    private static Map<String, Set<String>> loadLLMSelectedPairs(String filePath) {
//        Alignment llmSelectedPairs = Agent.loadSelectedCorrespondencesFromFile(filePath);
        Map<String, Set<String>> llmSelectedPairs = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // skip empty lines
                }
                if (line.startsWith("//")) {
                    continue; // skip comment lines
                }

                String[] lines = line.split(", ");
                if (lines.length < 2) {
                    System.out.println("Skipping line due to insufficient information: " + line);
                    continue; // skip lines that do not have enough information
                }
                Set<String> entities = new HashSet<>();
                for (int i = 1; i < lines.length; i++) {
                    entities.add(lines[i].trim());
                }
                llmSelectedPairs.put(lines[0], entities);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return llmSelectedPairs;
    }

    private static void potentialPairsToAlignment(Alignment reference, double threshold) {
        Map<String, Set<String>> potentialPairsH = loadPotentialPairs(
                Main.humanStringsDict.get("potentialEntityPairsPath").toString() + threshold + ".txt");
        Map<String, Set<String>> potentialPairsM = loadPotentialPairs(
                Main.mouseStringsDict.get("potentialEntityPairsPath").toString() + threshold + ".txt");

        System.out.println("Potential pairs -----");
        compareStatics(potentialPairsH, potentialPairsM, reference);
    }

    private static void compareStatics(Map<String, Set<String>> potentialPairsS,
                                       Map<String, Set<String>> potentialPairsT, Alignment reference) {
        Alignment foundT = new Alignment();
        Alignment foundS = new Alignment();
        for (Correspondence c : reference) {
            if (potentialPairsT.containsKey(c.getEntityOne())) {
                for (var s : potentialPairsT.get(c.getEntityOne())) {
                    if (c.getEntityTwo().equals(s)) {
                        foundT.add(c);
                    }
                }
            }
            if (potentialPairsS.containsKey(c.getEntityTwo())) {
                for (var s : potentialPairsS.get(c.getEntityTwo())) {
                    if (c.getEntityOne().equals(s)) {
                        foundS.add(c);
                    }
                }
            }
        }
        System.out.println("Total in Source: " + potentialPairsS.size());
        System.out.println("Reference found in Source: " + foundS.size());
        System.out.println("Total in Target: " + potentialPairsT.size());
        System.out.println("Reference found in Target: " + foundT.size());
    }

    private static Map<String, Set<String>> loadPotentialPairs(String filePath) {
        Map<String, Set<String>> potentialPairs = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String selfUri = "";
            Set<String> potentialPairEntities;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // skip empty lines
                }
                if (line.startsWith("Self URI: ")) {
                    selfUri = line.substring("Self URI: ".length()).trim();
                    potentialPairEntities = new HashSet<>();
                    potentialPairs.put(selfUri, potentialPairEntities);
                    continue;
                }
                potentialPairs.get(selfUri)
                        .add(line.substring("  - Other Entity URI: ".length()).split(",")[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return potentialPairs;
    }
}
