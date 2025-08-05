package LLMABelief;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Helper {

    public static Alignment loadInitCorrespondences(String initCorrespondencesPath) {
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
}
