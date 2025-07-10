package cn.edu.xjtlu.iot.syzhang.LLMABelief;

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
        potentialPairsToAlignment(0.5);
        potentialPairsToAlignment(0.6);
        potentialPairsToAlignment(0.7);
        potentialPairsToAlignment(0.8);
        potentialPairsToAlignment(0.9);
    }

    private static void potentialPairsToAlignment(double threshold) {
        Map<String, Set<String>> potentialPairsH = loadPotentialPairs(
                Main.humanStringsDict.get("potentialEntityPairsPath").toString() + threshold + ".txt");
        Map<String, Set<String>> potentialPairsM = loadPotentialPairs(
                Main.mouseStringsDict.get("potentialEntityPairsPath").toString() + threshold + ".txt");

        Alignment alignment;
        try {
            alignment = new Alignment(new File("src/main/java/DataSet/Anatomy/reference.rdf"));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Alignment foundM = new Alignment();
        Alignment foundH = new Alignment();
        for (Correspondence c : alignment) {
            if (potentialPairsM.containsKey(c.getEntityOne())) {
                for (var s : potentialPairsM.get(c.getEntityOne())) {
                    if (c.getEntityTwo().equals(s)) {
                        foundM.add(c);
                    }
                }
            }
            if (potentialPairsH.containsKey(c.getEntityTwo())) {
                for (var s : potentialPairsH.get(c.getEntityTwo())) {
                    if (c.getEntityOne().equals(s)) {
                        foundH.add(c);
                    }
                }
            }
        }
        System.out.println("Statistics for potential pairs with threshold: " + threshold);
        System.out.println("Total correspondences: " + alignment.size());
        System.out.println("Total potential pairs for Human: " + potentialPairsH.size());
        System.out.println("Reference correspondences existing in human potential pairs: " + foundH.size());
        System.out.println("Total potential pairs for Mouse: " + potentialPairsM.size());
        System.out.println("Reference correspondences existing in mouse potential pairs: " + foundM.size());
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
