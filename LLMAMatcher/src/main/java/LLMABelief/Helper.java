package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;

import java.io.*;

public class Helper {

    public static Alignment loadCorrespondences(String initCorrespondencesPath) {
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

    public static FileWriter createFileWriter(String filePath) {
        return createFileWriter(filePath, false);
    }

    public static FileWriter createFileWriter(String filePath, boolean append) {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            return new FileWriter(file, append);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
