package de.uni_mannheim.informatik.dws.melt.demomatcher;

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
    public static void print(OntClass ontClass) {
        if (ontClass.asNode().isBlank()) {
            return;
        }

        System.out.println("===============================");

        System.out.println("OntClass: " + ontClass.getURI());
        System.out.println("  - Label: " + ontClass.getLabel(null));
        System.out.println("  - Comment: " + ontClass.getComment(null));
        System.out.println("  - Local name: " + ontClass.getLocalName());
        System.out.println("  - Super classes: ");
        for (OntClass superClass : ontClass.listSuperClasses().toList()) {
            System.out.println("    - " + superClass.getURI());
        }

        System.out.println("  - Sub classes: ");
        for (OntClass subClass : ontClass.listSubClasses().toList()) {
            System.out.println("    - " + subClass.getURI());
        }

        System.out.println("  - Equivalent classes: ");
        for (OntClass equivalentClass : ontClass.listEquivalentClasses().toList()) {
            System.out.println("    - " + equivalentClass.getURI());
        }

        System.out.println("  - Disjoint classes: ");
        for (OntClass disjointClass : ontClass.listDisjointWith().toList()) {
            System.out.println("    - " + disjointClass.getURI());
        }

        System.out.println("  - Related Synonyms: ");
        printRelatedSynonym(ontClass);

        System.out.println("  - Properties: ");
        printProperty(ontClass);
    }

    private static void printRelatedSynonym(OntClass ontClass){
        OntModel model = ontClass.getOntModel();
        OntProperty hasRelatedSynonym = model.getOntProperty("http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");
        Resource cls = model.getResource(ontClass.getURI());
        for (StmtIterator it = cls.listProperties(hasRelatedSynonym); it.hasNext(); ) {
            Statement stmt = it.nextStatement();
            // 获取rdf:resource的值
            Resource relatedSynonymResource = stmt.getObject().asResource();
            System.out.println("    - URI: " + relatedSynonymResource.getURI());
            // 获取rdfs:label属性
            Property labelProperty = model.getProperty(RDFS.label.getURI());
            System.out.println("      - Label: " + model.getResource(relatedSynonymResource.getURI()).getProperty(labelProperty).getString());
        }
    }

    private static void printProperty(OntClass ontClass) {
        for (OntProperty property : ontClass.listDeclaredProperties().toList()) {
            System.out.println("    - URI: " + property.getURI());
            System.out.println("      - Local name: " + property.getLocalName());
            System.out.println("      - Property value: " + property.getPropertyValue(null));
        }

    }

    public static int calculateNearbyClasses(int depth, OntClass ontClass) {
//        System.out.println("=================================================================================");
//        System.out.println("Calculate nearby classes for OntClass: " + ontClass.getURI() + " - Label: " + ontClass.getLabel(null) + " - Depth: " + depth);

        if (depth == 0) {
            return 0;
        }


        Set<OntClass> visitedClasses = new HashSet<>();
        visitedClasses.add(ontClass);
        calculateClasses(depth, ontClass.listSubClasses().toList(), visitedClasses);
        calculateClasses(depth, ontClass.listSuperClasses().toList(), visitedClasses);

        return visitedClasses.size();
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
