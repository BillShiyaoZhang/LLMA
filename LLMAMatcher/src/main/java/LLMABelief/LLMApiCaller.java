package LLMABelief;

public interface LLMApiCaller {
    String prompt(String message);
    Float[] embed(String text);
}

