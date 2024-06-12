package de.uni_mannheim.informatik.dws.melt.demomatcher;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import de.uni_mannheim.informatik.dws.melt.matching_data.TestCase;
import de.uni_mannheim.informatik.dws.melt.matching_data.Track;
import de.uni_mannheim.informatik.dws.melt.matching_data.TrackRepository;
import de.uni_mannheim.informatik.dws.melt.matching_eval.ExecutionResultSet;
import de.uni_mannheim.informatik.dws.melt.matching_eval.Executor;
import de.uni_mannheim.informatik.dws.melt.matching_eval.evaluator.EvaluatorCSV;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.AlignmentParser;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.vocabulary.RDFS;
import org.xml.sax.SAXException;

public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        loadOntologiesAndCalculateBelief();
        // initDatabase();
        // runMatcherWithLocalData();
        // testMatcherOnline();
        // calculateStaticsManually();
    }

    private static void loadOntologiesAndCalculateBelief() {
        // load ontologies from files
        OntModel source = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        source.read("src/main/java/DataSet/human.owl");
        OntModel target = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        target.read("src/main/java/DataSet/mouse.owl");

        // TODO: calculate beliefs for each entities of ontologies
        for (OntClass entity : source.listClasses().toList()){
            OntClassHelper.print(entity);
        }

        // TODO: store the calculated beliefs into a file
    }

    private static void calculateStaticsManually() throws IOException, SAXException {
        File referenceFile = new File("simpleSealsMatcher/src/main/java/DataSet/reference.rdf");
        Alignment alignment = new Alignment();
        try (BufferedReader br = new BufferedReader(new FileReader("alignment.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("s")){
                    continue;
                }
                String[] values = line.split(",");
                alignment.add(new Correspondence(values[0].trim(), values[1].trim(), Double.parseDouble(values[2].trim())));
            }
        }
        Alignment reference = AlignmentParser.parse(referenceFile);

        calculateStatics(alignment, reference);

        File referenceFile1 = new File("simpleSealsMatcher/src/main/java/DataSet/reference.rdf");
        Alignment alignment1 = new Alignment();
        try (BufferedReader br = new BufferedReader(new FileReader("alignment.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("s")){
                    continue;
                }
                String[] values = line.split(",");
                alignment1.add(new Correspondence(values[0].trim(), values[1].trim(), Double.parseDouble(values[2].trim())));
            }
        }
        Alignment reference1 = AlignmentParser.parse(referenceFile1);

        OntModel source = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        source.read("simpleSealsMatcher/src/main/java/DataSet/human.owl");
        OntModel target = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        target.read("simpleSealsMatcher/src/main/java/DataSet/mouse.owl");
        MyMatcher matcher = new MyMatcher();
        matcher.setup(source, target, false);
        Alignment toRemove = matcher.removeAttack(alignment1, source, target);
        alignment1.removeAll(toRemove);

        print("===========================================");
        calculateStatics(alignment1, reference1);

    }

    private static void calculateStatics(Alignment alignment, Alignment reference) {
        Alignment referenceCopy = new Alignment(reference);
        Alignment alignmentCopy = new Alignment(alignment);
        Alignment tp = new Alignment();
        Alignment tn = new Alignment();
        Alignment fp = new Alignment();
        Alignment fn = new Alignment();
        for (Correspondence myVar : alignment){
            for (Correspondence referenceVar : reference){
                if (referenceVar.getEntityOne().trim().equals(myVar.getEntityOne().trim())
                        && referenceVar.getEntityTwo().trim().equals(myVar.getEntityTwo().trim())) {
                    if (referenceVar.getConfidence() > 0.5){
                        if (myVar.getConfidence() > 0.5){
                            tp.add(myVar);  // always true
                        }
                    } else {
                        if (myVar.getConfidence() > 0.5){
                            fp.add(myVar);  // always true
                        }
                    }
                    referenceCopy.remove(referenceVar);
                    alignmentCopy.remove(myVar);
                    break;
                }
                if (referenceVar.getEntityOne().trim().equals(myVar.getEntityTwo().trim())
                        && referenceVar.getEntityTwo().trim().equals(myVar.getEntityOne().trim())){
                    if (referenceVar.getConfidence() > 0.5){
                        if (myVar.getConfidence() > 0.5){
                            tp.add(myVar);
//                        } else {
//                            fn.add(myVar);
                        }
                    } else {
                        if (myVar.getConfidence() > 0.5){
                            fp.add(myVar);
//                        } else {
//                            tn.add(myVar);
                        }
                    }
                    referenceCopy.remove(referenceVar);
                    alignmentCopy.remove(myVar);
                    break;
                }
            }
        }

        // if reference says true, but not found
        for (Correspondence var : referenceCopy){
            if (var.getConfidence() > 0.5) {
                fn.add(var);    // always true
            }
        }
        // if we found, but reference says false
        for (Correspondence var : alignmentCopy){
            if (var.getConfidence() > 0.5) {
                fp.add(var);    // always true
            }
        }


        System.out.println("tp: " + tp.size() + ", tn: " + tn.size() + ", fp: " + fp.size() + ", fn: " + fn.size());

        System.out.print("precision: " + (double) tp.size() / (tp.size() + fp.size()) + ", ");
        System.out.print("recall: " + (double) tp.size() / (tp.size() + fn.size()) + ", ");

        System.out.println("f1: " + (double) 2 * tp.size() / (2 * tp.size() + fp.size() + fn.size()));
    }

    private static void initDatabase() throws IOException {
        uploadEmbeddingsFromFileToWeaviate("embeddings/anatomy/source.json", "Source");
        uploadEmbeddingsFromFileToWeaviate("embeddings/anatomy/target.json", "Target");
    }

    private static void runMatcherWithLocalData(){
        File sourceFile = new File("simpleSealsMatcher/src/main/java/DataSet/human.owl");
        File targetFile = new File("simpleSealsMatcher/src/main/java/DataSet/mouse.owl");
        File referenceFile = new File("simpleSealsMatcher/src/main/java/DataSet/reference.rdf");
        // let's execute our matcher on the OAEI Anatomy test case
        ExecutionResultSet ers = Executor.run(
                new TestCase("localtest", sourceFile.toURI(), targetFile.toURI(), referenceFile.toURI(),
                        new Track(null, null, null, false) {
                            @Override
                            protected void downloadToCache() throws Exception {
                                return;
                            }
                        }), new MyMatcher());

        // let's evaluate our matcher (you can find the results in the `results` folder (will be created if it
        // does not exist).
        EvaluatorCSV evaluatorCSV = new EvaluatorCSV(ers);
        evaluatorCSV.writeToDirectory();
    }

    private static void uploadEmbeddingsFromFileToWeaviate(String fileName, String collectionName) throws IOException {
        print("uploading embeddings to weaviate collection " + collectionName + " ...");
        FileReader fileReader = new FileReader(fileName);
        JSONReader jsonReader = new JSONReader(fileReader);

        Weaviate db = new Weaviate();

//        ArrayList<JSONObject> rows = new ArrayList<>();
        while(fileReader.ready()){
            JSONObject var = jsonReader.readObject(JSONObject.class);
            if (var.get("uri") == null){
                continue;
            }
            ArrayList<Float> vector = new ArrayList<>();
            for (Object bigDecimal : (JSONArray) var.get("vector")) {
                vector.add(((BigDecimal) bigDecimal).floatValue());
            }
//            var.put("vector", vector);

            db.client.data().creator()
                    .withClassName(collectionName)
                    .withVector(vector.toArray(new Float[0]))
                    .withProperties(new HashMap<String, Object>() {{
                        put("uri", var.get("uri"));
                        put("isNegotiated", var.get("isNegotiated")); // will be automatically added as a number property
                    }})
                    .run();

//            rows.add(var);
        }

//        Zilliz db = new Zilliz(collectionName).initCollection();
//        db.insert(rows);
    }

    /***
     * Test the matcher with online resource to have statistics.
     */
    private static void testMatcherOnline(){
        // let's execute our matcher on the OAEI Anatomy test case
        ExecutionResultSet ers = Executor.run(TrackRepository.Anatomy.Default.getFirstTestCase(), new MyMatcher());

        // let's evaluate our matcher (you can find the results in the `results` folder (will be created if it
        // does not exist).
        EvaluatorCSV evaluatorCSV = new EvaluatorCSV(ers);
        evaluatorCSV.writeToDirectory();
    }

    private static void print(String s){
        System.out.println(s);
    }
}


















