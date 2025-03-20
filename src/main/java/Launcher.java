
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.*;
import java.io.InputStream;
import java.util.Objects;

import javax.imageio.ImageIO;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import static java.awt.SystemColor.window;


public class Launcher extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                try (InputStream iconStream = getClass().getResourceAsStream("/images/general/application_icon.jpeg")) {
                    BufferedImage awtIcon = ImageIO.read(iconStream);
                    taskbar.setIconImage(awtIcon);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/ServerClient/launcher.fxml"))
            );
            Parent root = loader.load();
            Scene scene = new Scene(root, 850, 550);
            primaryStage.setTitle("Robo Rally");



            primaryStage.setScene(scene);
            primaryStage.show();



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);

    }
}
