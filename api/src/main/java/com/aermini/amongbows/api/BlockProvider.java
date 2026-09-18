package com.aermini.amongbows.api;

public interface BlockProvider {

    org.bukkit.Material getMaterialById(int id, int data);

    org.bukkit.Material getMaterialByName(String name);

    int[] lookup(String name);

    String getVersion();
}
