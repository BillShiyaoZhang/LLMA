package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;

import java.io.*;
import java.util.*;

public class Agent {
    private String name;
    private LLMApiCaller llm;
    private OntModel ontology;
    private Dictionary stringDict;
    private double threshold;

    public List<OntClass> entities;
    public Dictionary<String, String> entityVerbos;

    public Alignment initialCorrespondences;
    public Alignment privateCorrespondences;

    public Agent(Dictionary stringDict, LLMApiCaller apiCaller, double threshold) {
        this.stringDict = stringDict;
        this.name = stringDict.get("collectionName").toString();
        this.llm = apiCaller;
        this.threshold = threshold;

        OntModel s = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        s.read(stringDict.get("ontologyPath").toString());
        this.ontology = s;

        entities = extractEntities(ontology, stringDict.get("entityURIPrefix").toString());
        entityVerbos = Main.loadEntityVerbos(stringDict.get("verbosePath").toString());

        initialCorrespondences = Helper.loadCorrespondences(
                Main.commonStringsDict.get("initCorrespondencesPath").toString() + threshold + ".txt");

        privateCorrespondences = new Alignment();
        // NOTE: the below line is used to load the selected correspondences from the LLM.
        // Only use it if you have already run the LLM to select correspondences.
//        privateCorrespondences = loadSelectedCorrespondencesFromFile();
    }

    public static List<OntClass> extractEntities(OntModel ontology, String entityURIPrefix) {
        List<OntClass> entities = new ArrayList<>();
        for (OntClass ontClass : ontology.listClasses().toList()) {
            if (ontClass.isURIResource()
                    && !ontClass.getURI().isEmpty()
                    && ontClass.getURI().startsWith(entityURIPrefix)) {
                entities.add(ontClass);
            }
        }

        System.out.println("Agent " + "finds " + entities.size() + " entities.");
        return entities;
    }

    //    propertyURI = "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym"
    public static String verbalize(OntClass ontClass, String propertyUri) {
        if (ontClass.asNode().isBlank()) {
            return "";
        }

        String output = "";
        output += "URI: " + ontClass.getURI() + "\n";
        output += "  - Label: " + ontClass.getLabel(null) + "\n";

        if (ontClass.getComment(null) != null) {
            output += "  - Comment: " + ontClass.getComment(null) + "\n";
        }
        output += "  - Local name: " + ontClass.getLocalName() + "\n";

        if (ontClass.hasSuperClass()) {
            output += "  - Super classes: \n";
        }
        for (OntClass superClass : ontClass.listSuperClasses().toList()) {
            if (!superClass.isURIResource()) {
                continue; // Skip blank nodes
            }
            output += "    - " + superClass.getURI() + "\n";
            output += "      - Label: " + superClass.getLabel(null) + "\n";
            if (superClass.getComment(null) != null) {
                output += "      - Comment: " + superClass.getComment(null) + "\n";
            }
            output += "      - Local name: " + superClass.getLocalName() + "\n";
        }

        if (ontClass.hasSubClass()) {
            output += "  - Sub classes: \n";
        }
        for (OntClass subClass : ontClass.listSubClasses().toList()) {
            output += "    - " + subClass.getURI() + "\n";
            output += "      - Label: " + subClass.getLabel(null) + "\n";
            output += "      - Comment: " + subClass.getComment(null) + "\n";
            output += "      - Local name: " + subClass.getLocalName() + "\n";
        }

        if (!ontClass.listEquivalentClasses().toList().isEmpty()) {
            output += "  - Equivalent classes: \n";
        }
        for (OntClass equivalentClass : ontClass.listEquivalentClasses().toList()) {
            output += "    - " + equivalentClass.getURI() + "\n";
            output += "      - Label: " + equivalentClass.getLabel(null) + "\n";
            output += "      - Comment: " + equivalentClass.getComment(null) + "\n";
            output += "      - Local name: " + equivalentClass.getLocalName() + "\n";
        }

        if (!ontClass.listDisjointWith().toList().isEmpty()) {
            output += "  - Disjoint classes: \n";
        }
        for (OntClass disjointClass : ontClass.listDisjointWith().toList()) {
            output += "    - " + disjointClass.getURI() + "\n";
            output += "      - Label: " + disjointClass.getLabel(null) + "\n";
            output += "      - Comment: " + disjointClass.getComment(null) + "\n";
            output += "      - Local name: " + disjointClass.getLocalName() + "\n";
        }

        OntModel model = ontClass.getOntModel();
        OntProperty hasRelatedSynonym = model.getOntProperty(propertyUri);
        Resource cls = model.getResource(ontClass.getURI());
        if (!cls.listProperties().toList().isEmpty()) {
            output += "  - Related Synonyms: \n";
            for (StmtIterator it = cls.listProperties(hasRelatedSynonym); it.hasNext(); ) {
                Statement stmt = it.nextStatement();
                // 获取rdf:resource的值
                Resource relatedSynonymResource = stmt.getObject().asResource();
                output += "    - URI: " + relatedSynonymResource.getURI() + "\n";
                // 获取rdfs:label属性
                Property labelProperty = model.getProperty(RDFS.label.getURI());
                output += "      - Label: " + model.getResource(relatedSynonymResource.getURI()).getProperty(labelProperty).getString() + "\n";
            }
        }

        if (!ontClass.listDeclaredProperties().toList().isEmpty()) {
            output += "  - Properties: " + "\n";
            for (OntProperty property : ontClass.listDeclaredProperties().toList()) {
                output += "    - URI: " + property.getURI() + "\n";
                output += "      - Local name: " + property.getLocalName() + "\n";
                output += "      - Property value: " + property.getPropertyValue(null) + "\n";
            }
        }

        return output;
    }


    public void selectCorrespondences(Dictionary<String, String> entityVerbosOtherAgent) {
        Dictionary<String, Set<Belief<String>>> potentialEntityPairsDictReload = loadPotentialEntityPairsFromFile(
                Main.commonStringsDict.get("potentiCorrespondencesPath").toString() + threshold + "-" + name + ".txt");
        askLLMToSelectCorrespondences(potentialEntityPairsDictReload, entityVerbosOtherAgent);
//        privateCorrespondences = loadSelectedCorrespondencesFromFile(stringDict.get("llmSelectedCorrespondencesPath").toString() + threshold + "-formated.txt");
    }

    private Dictionary<String, Set<Belief<String>>> loadPotentialEntityPairsFromFile(String filePath) {
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

    public Alignment loadSelectedCorrespondencesFromFile(String filePath) {
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

    private void askLLMToSelectCorrespondences(Dictionary<String, Set<Belief<String>>> potentialEntityPairsDict,
                                               Dictionary<String, String> entityVerbosOtherAgent) {
        Set<String> selected = new HashSet<>();
        String path = stringDict.get("llmSelectedCorrespondencesPath").toString() + threshold + ".txt";
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

        FileWriter fw = Helper.createFileWriter(stringDict.get("llmSelectedCorrespondencesPath").toString() + threshold + ".txt", true);
        for (String selfURI : ((Hashtable<String, Set<Belief<String>>>) potentialEntityPairsDict).keySet()) {
            if (selected.contains(selfURI.trim())) {
                continue; // Skip URIs that have already been selected
            }
            Set<Belief<String>> beliefs = potentialEntityPairsDict.get(selfURI);
            if (beliefs.isEmpty()) {
                continue;
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

            String response = "";
            if (beliefsArray != null) {
                for (var beliefsSet : beliefsArray) {
                    if (beliefsSet == null) {
                        continue; // Skip null sets
                    }
                    String message =
                            "You are an assistant helping to select relevant entity pairs for alignment.\n" +
                                    "You have been provided with an entity from one ontology, and a set of entity from another ontology.\n" +
                                    "Your task is to prioritize the relevance of entities of another ontology from high to low " +
                                    "based on the provided contents.\n\n" +
                                    "<Entity to align>:\n" +
                                    entityVerbos.get(selfURI).toString() + "\n\n" +
                                    "<Set of Entities>:\n";
                    for (Belief<String> belief : beliefsSet) {
                        message = message +
                                "<Entity from another agent>\n" +
                                "URI: " + belief.obj + "\n" +
                                entityVerbosOtherAgent.get(belief.obj).toString() + "\n";
                    }
                    message = message +
                            "Please select all possible entities, from the given set, you find likely to be aligned to the given entity.\n" +
                            "Provide your response in the following format:\n" +
                            "<Possible Entity URI>\n" +
                            "<Possible Entity URI>\n" +
                            "<Possible Entity URI>\n" +
                            "...\n\n" +
                            "If you do not find any relevant entity, please respond with 'No relevant entity found'.\n";

                    response += llm.prompt(message);
                }
                try {
                    String formatedResponse = formatResponse(response);
                    System.out.println(formatedResponse);
                    fw.write(selfURI + "\n " + formatedResponse + "\n");
                    fw.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String formatResponse(String response) {
        String prompt = "You are a helpful formatter.  The below is the response from the LLM on the task " +
                "finding the relevant entities regarding a given entity. Please format it to a list of URIs, " +
                "one URI per line, and remove any other text.  " +
                "If there are no URIs, please respond with an empty space only.\n\n" +
                response;
        return llm.prompt(prompt);
    }

    // NOTE: maybe not used
    private Dictionary<String, Set<Belief<String>>> extractPotentialEntityPairs(
            Alignment alignment, boolean isSource) {
        Set<String> selfURIs = new HashSet<>();
        if (isSource) {
            selfURIs = alignment.getDistinctSourcesAsSet();
        } else {
            selfURIs = alignment.getDistinctTargetsAsSet();
        }

        // Dict: selfURI -> Set of Belief<otherEntityURI>
        Dictionary<String, Set<Belief<String>>> potentialEntityPairsDict = new Hashtable<>();
        for (String selfURI : selfURIs) {
            potentialEntityPairsDict.put(selfURI, new HashSet<>());
        }

        // Populate the potentialEntityPairsDict with beliefs based on the alignment
        for (Correspondence c : alignment) {
            if (isSource) {
                potentialEntityPairsDict.get(c.getEntityOne()).add(new Belief<>(c.getEntityTwo(), c.getConfidence()));
            } else {
                potentialEntityPairsDict.get(c.getEntityTwo()).add(new Belief<>(c.getEntityOne(), c.getConfidence()));
            }
        }
        return potentialEntityPairsDict;
    }
}