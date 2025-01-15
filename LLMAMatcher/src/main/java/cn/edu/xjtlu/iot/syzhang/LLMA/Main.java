package cn.edu.xjtlu.iot.syzhang.LLMA;

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
import io.github.ollama4j.exceptions.OllamaBaseException;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;

import org.xml.sax.SAXException;

public class Main {
    public static void main(String[] args) throws IOException, SAXException {
//        run();

//        initDatabase();
         runMatcherWithLocalData();
//         testMatcherOnline();
//         calculateStaticsManually();
    }

    /***
     * Run the whole process
     */
    private static void run(){
        calculateEmbeddings("LLMAMatcher/src/main/java/DataSet/",
                new String[]{"human.owl", "mouse.owl"},
                "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");

        File directory = new File("results/result_"
                + new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date())
                + "/");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        storeEmbeddings(directory);

        // TODO: align. logs need to be saved.
        runMatcherWithLocalData();

    }

    private static void calculateEmbeddings(String base, String[] ontologyFiles, String propertyUri) {
        // read the ontology from local files
        OntModel source = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        source.read(base + ontologyFiles[0]);
        OntModel target = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        target.read(base + ontologyFiles[1]);

        // init db
        calculateEmbedding(source, propertyUri, "Source");
        calculateEmbedding(target, propertyUri, "Target");
    }

    private static void calculateEmbedding(OntModel source, String propertyUri, String collection) {
        int count = 0;
        for (OntClass entity : source.listClasses().toList()){
            if (entity.asNode().isBlank()) {
                continue;
            }
            if (entity.getURI().equals("http://www.w3.org/2002/07/owl#Thing")){
                continue;
            }
            String output = OntClassHelper.verbalize(entity, propertyUri);
            List<Double> embedding = Ollama.embed(output);
            Weaviate.client.data().creator()
                    .withClassName(collection)
                    .withVector(embedding.stream().map(Double::floatValue).toArray(Float[]::new))
                    .withProperties(new HashMap<String, Object>() {{
                        put("uri", entity.getURI());
                        put("isNegotiated", false); // will be automatically added as a number property
                    }})
                    .run();
            count++;
        }
        System.out.println("Collection " + collection + ": " + count);
    }

    private static void storeEmbeddings(File directory){
        storeEmbeddingsToFile("Source", directory);
        storeEmbeddingsToFile("Target", directory);
    }

    private static void storeEmbeddingsToFile(String collection, File directory) {
        File embeddingFolder = new File(directory.getPath() + "/embeddings/");
        if (!embeddingFolder.exists()) {
            embeddingFolder.mkdirs();
        }
        System.out.println(embeddingFolder.getPath());
        try (FileWriter file = new FileWriter(embeddingFolder.getPath() + "/" + collection.toLowerCase() + ".json", true)) {
            file.write("[");
            boolean first = true;
            var objs = Weaviate.getAllEntry(collection);
            for (var obj : objs) {
                if (!first) {
                    file.write(",");
                }
                file.write(Weaviate.toJsonString(obj));
                first = false;
            }
            file.write("]");
            file.flush();
            file.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static Map<OntClass, Set<OntClass>> calculateNearbyClasses(OntModel model, int depth){
        Map<OntClass, Set<OntClass>> nearbyClasses = new HashMap<>();
        for (OntClass entity : model.listClasses().toList()){
            if (entity.asNode().isBlank()) {
                continue;
            }
            if (entity.getURI().equals("http://www.w3.org/2002/07/owl#Thing")){
                continue;
            }
            nearbyClasses.put(entity,OntClassHelper.getNearbyClasses(depth, entity));
        }
        return nearbyClasses;
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
        LLMAMatcher matcher = new LLMAMatcher();
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
        // let's execute our matcher on the OAEI Anatomy test case
        File sourceFile = new File("LLMAMatcher/src/main/java/DataSet/human.owl");
        File targetFile = new File("LLMAMatcher/src/main/java/DataSet/mouse.owl");
        File referenceFile = new File("LLMAMatcher/src/main/java/DataSet/reference.rdf");
        ExecutionResultSet ers = Executor.run(
                new TestCase("localtest", sourceFile.toURI(), targetFile.toURI(), referenceFile.toURI(),
                        new Track(null, null, null, false) {
                            @Override
                            protected void downloadToCache() throws Exception {
                                return;
                            }
                        }), new LLMAMatcher());

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
        ExecutionResultSet ers = Executor.run(TrackRepository.Anatomy.Default.getFirstTestCase(), new LLMAMatcher());

        // let's evaluate our matcher (you can find the results in the `results` folder (will be created if it
        // does not exist).
        EvaluatorCSV evaluatorCSV = new EvaluatorCSV(ers);
        evaluatorCSV.writeToDirectory();
    }

    private static void print(String s){
        System.out.println(s);
    }
}


















