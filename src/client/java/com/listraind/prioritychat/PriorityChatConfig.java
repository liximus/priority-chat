package com.listraind.prioritychat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PriorityChatConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("prioritychat.json");

    private static PriorityChatConfig INSTANCE;


    private float textScale = 1.5f;
    private List<String> favouritePlayers = new ArrayList<>();
    private Boolean minimalBackground = true;


    public static PriorityChatConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static PriorityChatConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, PriorityChatConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new PriorityChatConfig();
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Геттеры и сеттеры для удобства
    public float getTextScale() {
        return textScale;
    }

    public void setTextScale(float value) {
        textScale = value;
        save();
    }


    public void setIsMinimalBackground(Boolean bool) {
        minimalBackground =  bool;
    }

    public Boolean getIsMinimalBackground() {
        return minimalBackground;
    }



    public void addFavouritePerson(String favouritePerson) {
        if (favouritePerson != null && !favouritePerson.isBlank()) {
            favouritePlayers.add(favouritePerson.toLowerCase());
            save();
        }
    }

    public void removeFavouritePerson(String favouritePerson) {
        if (favouritePerson != null) {
            favouritePlayers.remove(favouritePerson.toLowerCase());
            save();
        }
    }

    public List<String> getFavouritePersons() {
        return new ArrayList<>(favouritePlayers);
    }

    public void putFavouritePersonsList(List<String> favouritePersons) {
        favouritePlayers.clear();
        if (favouritePersons != null) {
            for (String person : favouritePersons) {
                if (person != null && !person.isBlank()) {
                    favouritePlayers.add(person.toLowerCase());
                }
            }
        }
        save();
    }

    public boolean getIsPersonFavourite(String person) {
        if (person == null) {
            return false;
        }
        return favouritePlayers.contains(person.toLowerCase());
    }




}