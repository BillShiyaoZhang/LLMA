package de.uni_mannheim.informatik.dws.melt.demomatcher;

public interface Agent {

    public void init();

    public String getCurrentState();

    public String getNextAction();

    public void updateState();

    public void addStateActionPair(String state, String action);
}
