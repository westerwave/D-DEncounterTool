package app.bvk.utils;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.bvk.entity.Creature;
import app.bvk.library.CreatureLibrary;
import de.schlichtherle.truezip.file.TFileInputStream;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class Utils
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static File imageFile = null;
    private static final String NAMESTRING = "Name: ";
    private static final String CANCELSTRING = "Cancel";
    private static final String ENTERPLAYERNAMESTRING = "Enter Player Name";

    public static final Image ICON = new Image(Utils.class.getClassLoader().getResourceAsStream("icon.png"));

    private Utils()
    {

    }

    public static String readTextFromFile(final Path filePath)
    {
        final StringBuilder builder = new StringBuilder();
        try (TFileInputStream fileInputStream = new TFileInputStream(filePath.toFile());
                final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);)
        {
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                builder.append(line);
            }
            bufferedReader.close();
        }
        catch (final IOException e)
        {
            LOGGER.error("ERROR while reading encounter save file", e);
        }
        return builder.toString();
    }

    public static void showImageFrame(final Creature creature)
    {
        Image img;
        final Stage imgFrame = new Stage();
        imgFrame.getIcons().add(ICON);
        imgFrame.initModality(Modality.WINDOW_MODAL);
        img = creature.getImage();
        final Pane pane = new Pane();
        final VBox vbox = new VBox();
        pane.getChildren().add(vbox);

        final Scene myDialogScene = new Scene(pane);
        final ZoomableImageView iv = new ZoomableImageView(pane, creature.getImage());
        vbox.getChildren().add(iv);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(10));

        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        final double width = img.getWidth() >= gd.getDisplayMode().getWidth() * 0.8 ? gd.getDisplayMode().getWidth() * 0.8 : img.getWidth();
        final double height = img.getHeight() >= gd.getDisplayMode().getHeight() * 0.8 ? gd.getDisplayMode().getHeight() * 0.8 : img.getHeight();
        final double windowXPos = 80;
        final double windowYPos = 45;

        imgFrame.setWidth(width);
        imgFrame.setHeight(height);
        imgFrame.setX(windowXPos);
        imgFrame.setY(windowYPos);

        imgFrame.setScene(myDialogScene);
        imgFrame.show();
    }

    public static Stage showSavingWarning()
    {
        final Stage stage = new Stage();
        stage.getIcons().add(ICON);
        stage.setTitle("Saving! Don't close this Window!");
        stage.initModality(Modality.APPLICATION_MODAL);
        final Label label = new Label("Saving!");
        final Scene scene = new Scene(label);
        stage.setScene(scene);
        stage.setWidth(200);
        stage.setHeight(100);
        stage.setResizable(false);
        stage.show();
        return stage;
    }

    public static void newLibraryEntryWindow(final Window ownerWindow)
    {
        final Dialog<Creature> d = new Dialog<>();
        final Stage stage = (Stage) d.getDialogPane().getScene().getWindow();
        stage.getIcons().add(ICON);
        d.initOwner(ownerWindow);
        d.initModality(Modality.WINDOW_MODAL);
        d.setTitle(ENTERPLAYERNAMESTRING);
        d.setResizable(false);
        final Label name = new Label(NAMESTRING);
        final TextField tfName = new TextField();
        final Label image = new Label(NAMESTRING);
        final TextField imageName = new TextField();
        imageName.setEditable(false);
        final Button chooseImage = new Button("Select Image");
        chooseImage.setOnAction(event ->
        {
            final FileChooser fc = new FileChooser();
            fc.setInitialDirectory(new File(System.getProperty("user.dir")));
            fc.getExtensionFilters().add(new ExtensionFilter("Image File", "*.png", "*.jpeg", "*.bmp"));
            final File file = fc.showOpenDialog(ownerWindow);
            if (file != null)
            {
                imageFile = file;
                imageName.setText(file.getName());
            }
        });

        final GridPane grid = new GridPane();
        grid.add(name, 0, 0);
        grid.add(tfName, 1, 0);
        grid.add(image, 0, 1);
        grid.add(imageName, 1, 1);
        grid.add(chooseImage, 2, 1);
        d.getDialogPane().setContent(grid);
        final ButtonType okButton = new ButtonType("Save", ButtonData.OK_DONE);
        final ButtonType cancelButton = new ButtonType(CANCELSTRING, ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().add(okButton);
        d.getDialogPane().getButtonTypes().add(cancelButton);
        d.setResultConverter(param ->
        {
            if (param == okButton)
            {
                return new Creature(tfName.getText(), imageFile.getName());
            }
            if (param == cancelButton)
            {
                return null;
            }
            return null;
        });

        final Optional<Creature> a = d.showAndWait();
        if (a.isPresent())
        {
            CreatureLibrary.getInstance().addCreature(a.get());
        }
    }

}
