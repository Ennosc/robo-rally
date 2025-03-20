package view;

import viewmodel.SceneManager;

import java.io.IOException;

public class ClientGUI{

    public static void startClient(String serverAddress, int serverPort) throws IOException {
        SceneManager sceneManager = new SceneManager(serverAddress, serverPort);
        sceneManager.switchToSelectRobot();
    }

}
