package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Agent {
    public String name;
    private LLMApiCaller llm;
    public OntModel ontology;
    private Dictionary stringDict;

    private List<OntClass> entities;
    private Dictionary<String, String> entityVerbos;

    private Alignment initialCorrespondences;
    public Alignment privateCorrespondences;

    public List<Belief<OntClass>> entityBeliefs;

    public Agent(Dictionary stringDict, LLMApiCaller apiCaller) {
        this.stringDict = stringDict;
        this.name = stringDict.get("collectionName").toString();
        this.llm = apiCaller;

        OntModel s = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        s.read(stringDict.get("ontologyPath").toString());
        this.ontology = s;

        entities = Helper.extractEntities(ontology, stringDict.get("entityURIPrefix").toString());
        entityVerbos = Main.loadEntityVerbos(stringDict.get("verbosePath").toString());

        initialCorrespondences = Helper.loadInitCorrespondences(
                Main.commonStringsDict.get("initCorrespondencesPath").toString() +
                        Main.commonStringsDict.get("threshold")+ ".txt");

        privateCorrespondences = new Alignment();

        entityBeliefs = new ArrayList<>();
        for (OntClass entity : entities) {
            Belief<OntClass> belief = new Belief<>(entity, 1);
            entityBeliefs.add(belief);
        }
        // NOTE: the below line is used to load the selected correspondences from the LLM.
        // Only use it if you have already run the LLM to select correspondences.
//        privateCorrespondences = loadSelectedCorrespondencesFromFile(stringDict.get("llmSelectedCorrespondencesPath").toString() + threshold + "-formated.txt");
    }

    public Dictionary<String, String> getVerbose() {
        return this.entityVerbos;
    }

    public Alignment getInitialCorrespondences() {
        return initialCorrespondences;
    }

    public void shortListCorrespondences(Dictionary<String, String> entityVerbosOtherAgent) {
        Dictionary<String, Set<Belief<String>>> potentialEntityPairsDictReload = loadPotentialEntityPairsFromFile(
                Main.commonStringsDict.get("potentiCorrespondencesPath").toString() +
                        Main.commonStringsDict.get("threshold") + "-" + name + ".txt");
        askLLMToSelectCorrespondences(potentialEntityPairsDictReload, entityVerbosOtherAgent);
        
        formatLlmSelectedCorrespondences();

        privateCorrespondences = loadShortListedCorrespondencesFromFile(stringDict.get("llmSelectedCorrespondencesPath").toString() +
                Main.commonStringsDict.get("threshold") + "-formated.txt");
    }

    public void formatLlmSelectedCorrespondences() {
        String basePath = stringDict.get("llmSelectedCorrespondencesPath").toString() +
                Main.commonStringsDict.get("threshold");
        String rawPath = basePath + ".txt";
        String formattedPath = basePath + "-formated.txt";

        File rawFile = new File(rawPath);
        if (!rawFile.exists()) {
            System.out.println("Warning: Raw LLM selection file does not exist: " + rawPath);
            return;
        }

        System.out.println("Formatting raw LLM selection file: " + rawPath + " -> " + formattedPath);

        try (BufferedReader reader = new BufferedReader(new FileReader(rawFile));
             FileWriter writer = Helper.createFileWriter(formattedPath)) {
            
            String line;
            String currentSourceURI = null;
            List<String> targetURIs = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // If the line is a source URI (starts with http:// followed by the source agent name)
                if (trimmed.startsWith("http://" + name.trim().toLowerCase())) {
                    // Before switching to the new source URI, write the previous source and its targets
                    if (currentSourceURI != null && !targetURIs.isEmpty()) {
                        writeFormattedLine(writer, currentSourceURI, targetURIs);
                    }
                    currentSourceURI = trimmed;
                    targetURIs.clear();
                } else {
                    // It's a target URI or other text. Keep only valid URIs (clean brackets)
                    if (trimmed.contains("http://") || trimmed.contains("https://")) {
                        String cleanedUri = trimmed.replace("<", "").replace(">", "").trim();
                        targetURIs.add(cleanedUri);
                    }
                }
            }

            // Write the last source URI and its targets
            if (currentSourceURI != null && !targetURIs.isEmpty()) {
                writeFormattedLine(writer, currentSourceURI, targetURIs);
            }

            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFormattedLine(FileWriter writer, String sourceURI, List<String> targetURIs) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceURI);
        for (String targetURI : targetURIs) {
            sb.append(", ").append(targetURI);
        }
        sb.append("\n");
        writer.write(sb.toString());
    }

    public static Dictionary<String, Set<Belief<String>>> loadPotentialEntityPairsFromFile(String filePath) {
        Dictionary<String, Set<Belief<String>>> potentialEntityPairsDict = new Hashtable<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String selfURI = null;
            Set<Belief<String>> beliefs = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // skip empty lines
                }
                if (line.startsWith("Self URI: ")) {
                    if (selfURI != null && !beliefs.isEmpty()) {
                        // Store the previous selfURI and its beliefs before moving to the next one
                        potentialEntityPairsDict.put(selfURI, beliefs);
                        beliefs = new HashSet<>(); // Reset beliefs for the next selfURI
                    }
                    selfURI = line.substring("Self URI: ".length());
                    continue;
                }
                String entityAndBelief = line.substring("  - Other Entity URI: ".length());
                String[] parts = entityAndBelief.split(", Confidence: ");
                if (parts.length != 2) {
                    System.out.println("Skipping line due to insufficient information: " + line);
                    continue; // skip lines that do not have enough information
                }
                Belief<String> belief = new Belief<>(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                beliefs.add(belief);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return potentialEntityPairsDict;
    }

    public Alignment loadShortListedCorrespondencesFromFile(String filePath) {
        Alignment llmSelectedPairs = new Alignment();
        try (BufferedReader reader = new BufferedReader(
                new FileReader(filePath))) {
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
                for (int i = 1; i < lines.length; i++) {
                    for (var c : this.initialCorrespondences.getCorrespondencesSource(lines[0].trim())) {
                        if (c.getEntityTwo().equals(lines[i].trim())) {
                            llmSelectedPairs.add(c);
                        }
                    }
                    for (var c : this.initialCorrespondences.getCorrespondencesTarget(lines[0].trim())) {
                        if (c.getEntityOne().equals(lines[i].trim())) {
                            llmSelectedPairs.add(c);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return llmSelectedPairs;
    }

    private int getOptimalThreadPoolSize(int totalTasks) {
        int defaultThreads = 5; // Safe default
        if (llm instanceof MiniMaxApiCaller) {
            MiniMaxApiCaller.MiniMaxQuotaUsage usage = ((MiniMaxApiCaller) llm).getQuotaUsage();
            if (usage != null) {
                long intervalTotal = usage.getIntervalTotal();
                long intervalRemains = usage.getIntervalRemains();
                
                System.out.println("[MiniMax Quota Info] " + usage.toString());
                System.out.println("预计需要发送的请求数 (Tasks): " + totalTasks);
                
                // Determine tier based on total credits in the 5-hour window
                // Concurrency = Upper limit of recommended range + 1
                int recommendedThreads = 5;
                if (intervalTotal <= 1500) {
                    recommendedThreads = 6; // Plus (4 + 1 = 5)
                } else if (intervalTotal <= 3000) {
                    recommendedThreads = 7; // Max (5 + 1 = 6)
                } else {
                    recommendedThreads = 9; // Ultra (7 + 1 = 8)
                }
                
                // If remaining quota is extremely low, reduce concurrency to 1 to avoid abrupt 429s
                if (intervalRemains > 0 && intervalRemains < 20) {
                    System.out.println("[WARNING] Quota is low (" + intervalRemains + " credits remaining). Reducing concurrency to 1.");
                    recommendedThreads = 1;
                }
                recommendedThreads = 20;

                // Warning if remaining quota is insufficient for the batch of tasks
                if (intervalRemains > 0 && totalTasks > intervalRemains) {
                    System.out.println(String.format(
                        "[WARNING] 当前 Token Plan 剩余配额为 %d，但预计需要发送 %d 个请求。额度不足，执行中途可能会用尽并暂停等待刷新...",
                        intervalRemains, totalTasks
                    ));
                }
                
                return recommendedThreads;
            }
        }
        return defaultThreads;
    }

    private void askLLMToSelectCorrespondences(Dictionary<String, Set<Belief<String>>> potentialEntityPairsDict,
                                               Dictionary<String, String> entityVerbosOtherAgent) {
        // Load already selected URIs to avoid duplication
        Set<String> selected = new HashSet<>();
        String path = stringDict.get("llmSelectedCorrespondencesPath").toString() +
                Main.commonStringsDict.get("threshold")+ ".txt";
        if (new File(path).exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("http://" + name.trim().toLowerCase())) {
                        selected.add(line.trim());
                    } else {
                        continue;
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Calculate expected number of tasks
        int totalTasks = 0;
        for (String selfURI : ((Hashtable<String, Set<Belief<String>>>) potentialEntityPairsDict).keySet()) {
            if (selected.contains(selfURI.trim())) {
                continue;
            }
            Set<Belief<String>> beliefs = potentialEntityPairsDict.get(selfURI);
            if (beliefs == null || beliefs.isEmpty()) {
                continue;
            }
            int size = beliefs.size() / 10;
            if (beliefs.size() % 10 != 0) {
                size++;
            }
            totalTasks += size;
        }

        FileWriter fw = Helper.createFileWriter(stringDict.get("llmSelectedCorrespondencesPath").toString() +
                Main.commonStringsDict.get("threshold")+ ".txt", true);
//        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
        int poolSize = getOptimalThreadPoolSize(totalTasks);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            for (String selfURI : ((Hashtable<String, Set<Belief<String>>>) potentialEntityPairsDict).keySet()) {
                if (selected.contains(selfURI.trim())) {
                    continue; // Skip URIs that have already been selected
                }
                Set<Belief<String>> beliefs = potentialEntityPairsDict.get(selfURI);
                if (beliefs.isEmpty()) {
                    continue;
                }

                System.out.println(selfURI);
                try {
                    fw.write(selfURI + "\n ");
                    fw.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Set<Belief<String>>[] beliefsArray = null;

                if (beliefs.size() > 0) {
                    int size = beliefs.size() / 10;
                    if (beliefs.size() % 10 != 0) {
                        size++;
                    }
                    beliefsArray = new Set[size];
                    int index = 0;
                    for (Belief<String> belief : beliefs) {
                        if (beliefsArray[index] == null) {
                            beliefsArray[index] = new HashSet<>();
                        }
                        beliefsArray[index].add(belief);
                        if (beliefsArray[index].size() >= 10) {
                            index++;
                        }
                    }
                }

                if (beliefsArray != null) {
                    final String entityVerbose = this.getVerbose().get(selfURI).toString();
                    List<Callable<String>> tasks = new ArrayList<>();
                    for (var beliefsSet : beliefsArray) {
                        if (beliefsSet == null) {
                            continue; // Skip null sets
                        }
                        tasks.add(() -> {
                            StringBuilder messageBuilder = new StringBuilder()
                                    .append("You are an assistant helping to select relevant entity pairs for alignment.\n")
                                    .append("You have been provided with an entity from one ontology, and a set of entity from another ontology.\n")
                                    .append("Your task is to prioritize the relevance of entities of another ontology from high to low ")
                                    .append("based on the provided contents.\n\n")
                                    .append("<Entity to align>:\n")
                                    .append(entityVerbose)
                                    .append("\n\n")
                                    .append("<Set of Entities>:\n");
                            for (Belief<String> belief : beliefsSet) {
                                messageBuilder
                                        .append("<Entity from another agent>\n")
                                        .append("URI: ")
                                        .append(belief.obj)
                                        .append("\n")
                                        .append(entityVerbosOtherAgent.get(belief.obj).toString())
                                        .append("\n");
                            }
                            messageBuilder
                                    .append("Please select all possible entities, from the given set, you find likely to be aligned to the given entity.\n")
                                    .append("Provide your response in the following format:\n")
                                    .append("<Possible Entity URI>\n")
                                    .append("<Possible Entity URI>\n")
                                    .append("<Possible Entity URI>\n")
                                    .append("...\n\n")
                                    .append("If you do not find any relevant entity, please respond with 'No relevant entity found'.\n");
                            String response = llm.prompt(messageBuilder.toString());
                            return llm.getUrisOnlyFromStringForThinkingModel(response);
                        });
                    }

                    if (!tasks.isEmpty()) {
                        try {
                            List<Future<String>> futures = executor.invokeAll(tasks);
                            for (Future<String> future : futures) {
                                try {
                                    String formattedResponse = future.get();
                                    fw.write(formattedResponse + "\n");
                                    fw.flush();
                                } catch (ExecutionException e) {
                                    throw new RuntimeException("Failed to process correspondences for " + selfURI, e);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException("Thread interrupted while retrieving correspondences.", e);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Thread interrupted while selecting correspondences.", e);
                        }
                    }
                }
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        try {
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}