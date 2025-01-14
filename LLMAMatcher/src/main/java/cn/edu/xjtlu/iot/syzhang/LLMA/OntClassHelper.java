package cn.edu.xjtlu.iot.syzhang.LLMA;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OntClassHelper {
    public static String verbalize(OntClass ontClass, String propertyUri) {
        if (ontClass.asNode().isBlank()) {
            return "";
        }

        String output = "";
        output += "OntClass: " + ontClass.getURI() + "\n";
        output += "  - Label: " + ontClass.getLabel(null) + "\n";
        output += "  - Comment: " + ontClass.getComment(null) + "\n";
        output += "  - Local name: " + ontClass.getLocalName() + "\n";
        output += "  - Super classes: \n";
        for (OntClass superClass : ontClass.listSuperClasses().toList()) {
            output += "    - " + superClass.getURI() + "\n";
            output += "      - Label: " + superClass.getLabel(null) + "\n";
            output += "      - Comment: " + superClass.getComment(null) + "\n";
            output += "      - Local name: " + superClass.getLocalName() + "\n";
        }

        output += "  - Sub classes: \n";
        for (OntClass subClass : ontClass.listSubClasses().toList()) {
            output += "    - " + subClass.getURI() + "\n";
            output += "      - Label: " + subClass.getLabel(null) + "\n";
            output += "      - Comment: " + subClass.getComment(null) + "\n";
            output += "      - Local name: " + subClass.getLocalName() + "\n";
        }

        output += "  - Equivalent classes: \n";
        for (OntClass equivalentClass : ontClass.listEquivalentClasses().toList()) {
            output += "    - " + equivalentClass.getURI() + "\n";
            output += "      - Label: " + equivalentClass.getLabel(null) + "\n";
            output += "      - Comment: " + equivalentClass.getComment(null) + "\n";
            output += "      - Local name: " + equivalentClass.getLocalName() + "\n";
        }

        output += "  - Disjoint classes: \n";
        for (OntClass disjointClass : ontClass.listDisjointWith().toList()) {
            output += "    - " + disjointClass.getURI() + "\n";
            output += "      - Label: " + disjointClass.getLabel(null) + "\n";
            output += "      - Comment: " + disjointClass.getComment(null) + "\n";
            output += "      - Local name: " + disjointClass.getLocalName() + "\n";
        }

//        printRelatedSynonym(ontClass, propertyUri);
        OntModel model = ontClass.getOntModel();
        OntProperty hasRelatedSynonym = model.getOntProperty(propertyUri);
        Resource cls = model.getResource(ontClass.getURI());
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

        printProperty(ontClass);
        output += "  - Properties: " + "\n";
        for (OntProperty property : ontClass.listDeclaredProperties().toList()) {
            output += "    - URI: " + property.getURI() + "\n";
            output += "      - Local name: " + property.getLocalName() + "\n";
            output += "      - Property value: " + property.getPropertyValue(null) + "\n";
        }

//        System.out.println(output);
        return output;
    }

    private static void printProperty(OntClass ontClass) {


    }

    public static int countNearbyClasses(int depth, OntClass ontClass) {
//        System.out.println("=================================================================================");
//        System.out.println("Calculate nearby classes for OntClass: " + ontClass.getURI() + " - Label: " + ontClass.getLabel(null) + " - Depth: " + depth);

        if (depth == 0) {
            return 0;
        }

        return getNearbyClasses(depth, ontClass).size();
    }

    public static Set<OntClass> getNearbyClasses(int depth, OntClass ontClass) {
        Set<OntClass> visitedClasses = new HashSet<>();
        visitedClasses.add(ontClass);
        calculateClasses(depth, ontClass.listSubClasses().toList(), visitedClasses);
        calculateClasses(depth, ontClass.listSuperClasses().toList(), visitedClasses);

        return visitedClasses;
    }

    private static void calculateClasses(int depth, List<OntClass> list, Set<OntClass> visitedClasses){
        if (depth == 0) {
            return;
        }

        if (list == null || list.isEmpty()) {
            return;
        }

        for (OntClass ontClass : list) {
            if (ontClass.asNode().isBlank()) {
                continue;
            }
            if (ontClass.getURI().equals("http://www.w3.org/2002/07/owl#Thing")) {
                continue;
            }
            if (visitedClasses.add(ontClass)) {
//                System.out.println("OntClass: " + ontClass.getURI() + " - Label: " + ontClass.getLabel(null) + " - Depth: " + depth + " - Count: " + visitedClasses.size());
                calculateClasses(depth - 1, ontClass.listSubClasses().toList(), visitedClasses);
                calculateClasses(depth - 1, ontClass.listSuperClasses().toList(), visitedClasses);
            }

        }
    }
}
