package app.bvk.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.bvk.encounter.Encounter;
import app.bvk.encounter.EncounterEntry;
import app.bvk.entity.Creature;
import app.bvk.entity.Monster;
import app.bvk.gui.MainGUI;
import app.bvk.library.LibraryEntry;
import app.bvk.utils.Utils;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class MainController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    private Encounter encounter;

    @FXML
    private BorderPane rootBorderPane;

    @FXML
    private ListView<LibraryEntry> libraryList;

    @FXML
    private ListView<EncounterEntry> encounterList;

    @FXML
    private Text entryAmountText;

    @FXML
    private TextField filterTextField;

    @FXML
    private TextField encounterNameTextField;

    @FXML
    private CheckBox autoSaveCheckBox;

    @FXML
    private ProgressBar loadingProgressBar;

    @FXML
    private Label loadingProgressLabel;

    private SimpleDoubleProperty barProgress;
    private SimpleStringProperty progressText;
    private float progress = 0.0f;
    private float amountToLoad = 0.0f;

    @FXML
    public void initialize() {
        initProgressDisplay();
        Thread t = new Thread(() -> {
            loadData();
            fillLibrary();
            addLibraryTextFieldFilter();
            Platform.runLater(() -> rootBorderPane.setBottom(null));
        });
        t.setDaemon(true);
        t.start();
        initEncounter();

    }

    private void initProgressDisplay() {
        barProgress = new SimpleDoubleProperty(0.0);
        progressText = new SimpleStringProperty();
        loadingProgressBar.progressProperty().bindBidirectional(barProgress);
        loadingProgressLabel.textProperty().bindBidirectional(progressText);
    }

    private void initEncounter() {
        encounter = MainGUI.encounter;

        encounterNameTextField.setText(encounter.getEncounterName());
        encounterNameTextField.textProperty().bindBidirectional(encounter.encounterNameProperty);
        encounterNameTextField.textProperty()
                .addListener((obs, oldValue, newValue) -> encounter.setEncounterName(newValue));
    }

    private void addLibraryTextFieldFilter() {
        FilteredList<LibraryEntry> fData = new FilteredList<>(libraryList.getItems(), p -> true);
        filterTextField.textProperty().addListener((obs, oldValue, newValue) -> {
            fData.setPredicate(t -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                if (t.getCreature().getName().toLowerCase().contains(newValue.toLowerCase())) {
                    return true;
                }
                return false;
            });
            libraryList.setItems(fData);
        });
        incProgress();

        setProgressTextAndBar(progress / amountToLoad);
    }

    private void incProgress() {
        progress++;
    }

    private void loadData() {
        if (MainGUI.imageZipFile == null) {

            String userDir = System.getProperty("user.dir");
            Stream<Path> str = null;
            try {
                str = Files.walk(Paths.get(userDir + "\\images"));
                str.forEach(filePath -> {
                    if (Files.isRegularFile(filePath) && filePath.toString().toLowerCase().contains("png")) {
                        amountToLoad++;
                    }
                });
                str.forEach(filePath -> {
                    if (Files.isRegularFile(filePath) && filePath.toString().toLowerCase().contains("png")) {
                        String filePathString = filePath.toString();
                        String[] filePathSplitBackslash = filePathString.split("\\\\");
                        String[] fileNameSplitDot = filePathSplitBackslash[filePathSplitBackslash.length - 1]
                                .split("\\.");
                        String creatureName = fileNameSplitDot[0];
                        Monster m = new Monster(creatureName,
                                filePathSplitBackslash[filePathSplitBackslash.length - 1]);
                        MainGUI.creatureList.add(m);

                    }
                    incProgress();
                    setProgressTextAndBar(progress / (amountToLoad));
                });

            } catch (IOException e) {
                LOGGER.error("ERROR while reading zip file", e);
            } finally {
                if (str != null) {
                    str.close();
                }
            }
        } else {
            Enumeration<? extends ZipEntry> entries = MainGUI.imageZipFile.entries();
            while (entries.hasMoreElements()) {
                amountToLoad++;
                entries.nextElement();
            }
            entries = MainGUI.imageZipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ne = (ZipEntry) entries.nextElement();
                String name = ne.getName().split("\\.")[0];
                Monster m = new Monster(name, ne.getName());
                MainGUI.creatureList.add(m);

            }
            incProgress();
            setProgressTextAndBar(progress / amountToLoad);
        }
        entryAmountText.setText("#Entries: " + (int) amountToLoad);
    }

    private void fillLibrary() {
        ObservableList<LibraryEntry> leList = FXCollections.observableArrayList();
        for (Creature c : MainGUI.creatureList) {
            leList.add(new LibraryEntry(c));
            incProgress();
            setProgressTextAndBar(progress / (amountToLoad));
        }

        libraryList.setItems(leList);
    }

    @FXML
    private void newEncounter(ActionEvent event) {
        encounter.reset();
        encounterList.setItems(encounter.getObsList());
    }

    @FXML
    private void loadEncounter(ActionEvent event) {
        final FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        fc.getExtensionFilters().add(new ExtensionFilter("D&D Encounter Save", "*.ddesav"));
        File file = fc.showOpenDialog(MainGUI.mainStage);
        if (file != null) {
            encounter.readFromFile(file);
        }
        encounterList.setItems(encounter.getObsList());
    }

    @FXML
    private void saveEncounter(ActionEvent event) {
        final FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        fc.getExtensionFilters().add(new ExtensionFilter("D&D Encounter Save", "*.ddesav"));
        File file = fc.showSaveDialog(MainGUI.mainStage);
        if (file != null) {
            encounter.saveToFile(file);
        }
    }

    @FXML
    private void addNPC(ActionEvent event) {
        Utils.newNPCWindow(encounter);
        encounterList.setItems(encounter.getObsList());
    }

    @FXML
    private void addPlayer(ActionEvent event) {
        Utils.newPlayerWindow(encounter);
        encounterList.setItems(encounter.getObsList());
    }

    @FXML
    private void deleteSelected(ActionEvent event) {
        if (!encounterList.getSelectionModel().isEmpty() && !encounter.getCreatureList().isEmpty()) {
            encounter.remove(encounterList.getSelectionModel().getSelectedIndex());
            encounterList.setItems(encounter.getObsList());
        }
    }

    @FXML
    private void copySelected(ActionEvent event) {
        if (!encounterList.getSelectionModel().isEmpty()) {
            encounter.copy(encounterList.getSelectionModel().getSelectedIndex());
            encounterList.setItems(encounter.getObsList());
        }
    }

    @FXML
    private void sortEncounter(ActionEvent event) {
        encounter.sort();
        encounterList.setItems(encounter.getObsList());
    }

    @FXML
    private void nextTurn(ActionEvent event) {
        encounter.setNextIndex();
        encounterList.setItems(encounter.getObsList());
        encounterList.scrollTo(encounter.getCurrentIndex());
    }

    @FXML
    private void previousTurn(ActionEvent event) {
        encounter.setLastIndex();
        encounterList.setItems(encounter.getObsList());
        encounterList.scrollTo(encounter.getCurrentIndex());
    }

    @FXML
    private void addNewLibraryEntry(ActionEvent event) {
        Utils.newLibraryEntryWindow();
    }

    @FXML
    private void autoSaveChanged() {
        MainGUI.autosave = autoSaveCheckBox.isSelected();
    }

    private void setProgressTextAndBar(double percentage) {
        barProgress.set(percentage);
        Platform.runLater(() -> progressText.set("Loading progress: " + (int) (percentage * 100) + "%"));
    }
}