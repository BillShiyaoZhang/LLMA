package LLMABelief;

public class LLM {
    private static LLM llm;

    public static LLM getInstance() {
        if (llm == null) {
            llm = new LLM();
        }
        return llm;
    }
}
