package me.gypopo.autosellchestsaddon.commands;

import java.util.List;

public interface SubCommad {

    String getName();

    String getDescription();

    String getSyntax();

    void perform(Object logger, String args[]);

    List<String> getTabCompletion(String args[]);

}
