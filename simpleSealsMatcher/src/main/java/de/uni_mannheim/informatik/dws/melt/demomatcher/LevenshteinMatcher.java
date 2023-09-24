package de.uni_mannheim.informatik.dws.melt.demomatcher;

import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherYAAAJena;
import java.util.List;
import java.util.Properties;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * A simple matcher using the Levenshtein similarity metric.
 */
public class LevenshteinMatcher extends MatcherYAAAJena {
    
    private double threshold;
    
    public LevenshteinMatcher() {
        this.threshold = 1.0;
    }
    
    public LevenshteinMatcher(double threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public Alignment match(OntModel source, OntModel target, Alignment inputAlignment, Properties p) throws Exception {
//        Alignment alignment = new Alignment();
//        matchResources(source.listClasses().toList(), target.listClasses().toList(), alignment);//match only classes
//        return alignment;
    return null;
    }



//    private void matchResources(List<? extends OntResource> sourceResources, List<? extends OntResource> targetResources, Alignment alignment) {
//        //simple cartesian product -> might be very slow
//        for(OntResource source : sourceResources){
//            String sourceText = getStringRepresentation(source);
//            if(sourceText == null)
//                continue;
//            for(OntResource target : targetResources){
//                String targetText = getStringRepresentation(target);
//                if(targetText == null)
//                    continue;
//                double confidence = normalizedLevenshteinDistance(sourceText, targetText);
//                if (confidence >= threshold) {
//                    alignment.add(source.getURI(), target.getURI(), confidence);
//                }
//            }
//        }
//    }

    
    /**
     * Compute levenstein string distance
     * @see https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */

    
    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }



        public static void main(String[] args) {

            // 创建源模型和目标模型
            OntModel source = ModelFactory.createOntologyModel();
            OntModel target = ModelFactory.createOntologyModel();
            source.read("D:\\WorkSpace\\projects\\sealsproj\\simpleSealsMatcher\\src\\main\\java\\DataSet\\human.owl");
            target.read("D:\\WorkSpace\\projects\\sealsproj\\simpleSealsMatcher\\src\\main\\java\\DataSet\\mouse.owl");

            // TODO: 加载或创建源模型和目标模型

            // 创建输入对齐和配置参数
            Alignment inputAlignment = new Alignment();
            Properties p = new Properties();

            // 创建匹配器对象
            LevenshteinMatcher matcher = new LevenshteinMatcher();

            // 计算对齐
            try {
                Alignment alignment = matcher.match(source, target, inputAlignment, p);
                // TODO: 处理对齐结果
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}