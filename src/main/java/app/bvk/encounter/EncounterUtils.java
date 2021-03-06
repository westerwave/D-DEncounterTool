package app.bvk.encounter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

import app.bvk.entity.Creature;
import app.bvk.utils.Utils;

public class EncounterUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EncounterUtils.class);
    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final String CREATURES = "creatures";
    private static final String ENCOUNTER_NAME = "encounterName";
    private static final String SELECTED_INDEX = "selectedIndex";

    private EncounterUtils()
    {
    }

    public static boolean saveEncounterToFile(final Path targetFile, final Encounter encounterToSave)
    {
        try (JsonWriter jsonWriter = new JsonWriter(new FileWriter(targetFile.toFile()));)
        {
            jsonWriter.setIndent("  ");
            jsonWriter.beginObject();
            final String encounterName = "".equals(encounterToSave.nameProperty().get()) ? "Unnamed Encounter" : encounterToSave.nameProperty().get();
            jsonWriter.name(ENCOUNTER_NAME).value(encounterName);
            jsonWriter.name(SELECTED_INDEX).value(encounterToSave.getIndexOfSelectedCreature());
            jsonWriter.name(CREATURES);
            jsonWriter.beginArray();
            for (final EncounterEntry creatureEntry : encounterToSave.getCreatureList())
            {
                final Creature creature = creatureEntry.getCreature();
                jsonWriter.jsonValue(creature.getAsJson());
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
            jsonWriter.close();
        }
        catch (final IOException e)
        {
            LOGGER.error("ERROR while save encounter to file", e);
            return false;
        }
        return true;
    }

    public static Encounter loadEncounterFromFile(final Path sourceFile)
    {
        LOGGER.debug("Load Encounter from File {}.", sourceFile);
        final Encounter newEncounter = new Encounter("unnamed");
        final String fileContent = Utils.readTextFromFile(sourceFile);
        final JsonObject encounterSave = JSON_PARSER.parse(fileContent).getAsJsonObject();
        final String encounterName = encounterSave.get(ENCOUNTER_NAME).getAsString();
        final int selectedIndex = encounterSave.get(SELECTED_INDEX).getAsInt();
        final JsonArray creatures = encounterSave.get(CREATURES).getAsJsonArray();
        newEncounter.nameProperty().set(encounterName);
        for (final JsonElement creatureJson : creatures)
        {
            newEncounter.addCreatureEntry(new Creature(creatureJson));
        }
        newEncounter.selectIndex(selectedIndex);
        LOGGER.debug("Encounter Name: {}, Selected Index: {}, Amount of Creatures: {}", encounterName, selectedIndex, creatures.size());
        return newEncounter;
    }

    public static boolean autoSaveEncounter(final Encounter encounterToSave)
    {
        Path filePath = encounterToSave.getAutoSavePath();
        if (filePath != null)
        {
            final boolean deletionSuccessful = filePath.toFile().delete();
            LOGGER.debug("Deletion of auto save file was successful? {}", deletionSuccessful);
        }
        final String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        filePath = Paths.get(System.getProperty("user.dir") + "\\saves\\" + encounterToSave.nameProperty().get() + "-" + date.replace(":", ".") + ".ddesav");
        filePath.toFile().getParentFile().mkdirs();
        try
        {
            final boolean creationSuccessful = filePath.toFile().createNewFile();
            LOGGER.debug("Creation of autosave file was successful? {}", creationSuccessful);
        }
        catch (final IOException e)
        {
            LOGGER.error("ERROR while performing autosave", e);
            return false;
        }
        saveEncounterToFile(filePath, encounterToSave);
        return true;
    }
}
