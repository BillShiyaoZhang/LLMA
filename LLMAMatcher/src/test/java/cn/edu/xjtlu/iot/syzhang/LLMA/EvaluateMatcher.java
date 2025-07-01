package cn.edu.xjtlu.iot.syzhang.LLMA;

import de.uni_mannheim.informatik.dws.melt.matching_data.TrackRepository;
import de.uni_mannheim.informatik.dws.melt.matching_eval.ExecutionResult;
import de.uni_mannheim.informatik.dws.melt.matching_eval.ExecutionResultSet;
import de.uni_mannheim.informatik.dws.melt.matching_eval.Executor;
import de.uni_mannheim.informatik.dws.melt.matching_eval.evaluator.EvaluatorCSV;
import de.uni_mannheim.informatik.dws.melt.matching_eval.paramtuning.GridSearch;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;

public class EvaluateMatcher {
    
    @Test
    public void evalLLMAMatcherMatcher(){
//        ExecutionResultSet result = Executor.run(TrackRepository.Anatomy.Default, new SimpleStringMatcher());
        ExecutionResultSet result = Executor.run(TrackRepository.Anatomy.Default, new LLMAMatcher());
        ExecutionResult r = result.iterator().next();
        System.out.print(r.getSystemAlignment());
    }
    

}
