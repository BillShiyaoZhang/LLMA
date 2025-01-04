package core;

public interface Dialogue {

    public void start();

    public void end();

    public void init();

    public void getStatus();

    public void getParticipants();

    public String getDialogueContent(int index);

    public void addDialogueContent(String content);

    public void addDialogueContent(String content, int index);
}
