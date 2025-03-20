package network.interpreters;

import helpers.SoundFX;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import model.server_client.Client;
import network.JsonHandler;
import network.messages.actions8.AnimationMessage;
import network.messages.lobby3.MapSelectedMessage;
import network.messages.lobby3.SelectMapMessage;
import network.messages.phases7.programming.*;
import network.messages.specialMessage5.ErrorMessage;
import viewmodel.Animation;
import viewmodel.MapParser;


import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The ClientJsonInterpreter class processes JSON messages received by the client.
 * It extends the BaseClientJsonInterpreter and implements the JsonInterpreter interface.
 * This class sets up handlers for various message types to update the client state and UI accordingly.
 */
public class ClientJsonInterpreter extends BaseClientJsonInterpreter implements JsonInterpreter {

    /**
     * Constructs a new ClientJsonInterpreter for the specified client.
     *
     * @param client the Client instance associated with this interpreter.
     * @param logger the Logger used for logging events and errors.
     */
    public ClientJsonInterpreter(Client client, Logger logger) {
        super(client, logger);
        modifyHandlers();
    }

    /**
     * Configures the mapping between JSON message types and their corresponding handler functions.
     * <p>
     * This method populates the handlerMap with lambda functions that process each message type.
     * Handlers update the client’s state, notify the UI via Platform.runLater, or call relevant
     * methods on the client's data bridges based on the message content.
     * </p>
     */
    @Override
    protected void modifyHandlers(){
        handlerMap.put("Error", jsonObj -> {
            ErrorMessage err = JsonHandler.fromJson(jsonObj.toString(), ErrorMessage.class);
            String errorMessage = err.getMessageBody().getError();
            logger.info(errorMessage);
            boolean isGameRunning = errorMessage.equals("A game for group EdleEisbecher is already running");
            if (isGameRunning) {
                Platform.runLater(() -> {
                    client.getLobbyDataBridge().setGameRunning(true);
                });
            } else if (errorMessage.equals("NotYourTurn")) {
                client.getGameDataBridge().setErrorMessage("Not Your Turn", -1);
            } else if (errorMessage.equals("StartingPointTaken")) {
                client.getGameDataBridge().setErrorMessage("STARTING POINT IS ALREADY TAKEN", -1);
            } else if (errorMessage.startsWith("Priority in register")) {
                client.getGameDataBridge().setErrorMessage(errorMessage, -1);
            } else if (errorMessage.equals("You have already activated AdminPrivilege")) {
                client.getGameDataBridge().setErrorMessage(errorMessage, -1);
            } else if (errorMessage.equals("Upgrade card has already been used.")) {
                client.getGameDataBridge().setErrorMessage(errorMessage, -1);
            } else if (errorMessage.equals("Cannot select map yet. All players are not ready.")) {
                client.getLobbyDataBridge().setErrorMessage(errorMessage, -1);
            } else if (errorMessage.equals("Cannot select map yet. There are not enough players.")) {
                client.getLobbyDataBridge().setErrorMessage(errorMessage, -1);
            } else {
                client.getLobbyDataBridge().setErrorMessage(errorMessage, -1);
                client.getLobbyDataBridge().setRobotErrorMessage(errorMessage, -1);
                client.getGameDataBridge().setErrorMessage(errorMessage, -1);
            }

        });

        handlerMap.put("SelectMap", jsonObj -> {
            SelectMapMessage sm = JsonHandler.fromJson(jsonObj.toString(), SelectMapMessage.class);
            List<String> availableMaps = sm.getMessageBody().getAvailableMaps();
            Platform.runLater(() -> {
                client.getLobbyDataBridge().addClientIDToMapSelection(client.getClientID(), true);
            });
        });

        handlerMap.put("MapSelected", jsonObj -> {
            MapSelectedMessage ms = JsonHandler.fromJson(jsonObj.toString(), MapSelectedMessage.class);
            String map = ms.getMessageBody().getMap();
        });

        handlerMap.put("NotYourCards", jsonObj -> {
            NotYourCardsMessage nycm = JsonHandler.fromJson(jsonObj.toString(), NotYourCardsMessage.class);
        });

        handlerMap.put("ShuffleCoding", jsonObj -> {
            ShuffleCodingMessage scm = JsonHandler.fromJson(jsonObj.toString(), ShuffleCodingMessage.class);
        });

        handlerMap.put("CardSelected", jsonObj -> {
            CardSelectedMessage csm = JsonHandler.fromJson(jsonObj.toString(), CardSelectedMessage.class);
        });

        handlerMap.put("SelectionFinished", jsonObj -> {
            SelectionFinishedMessage sfm = JsonHandler.fromJson(jsonObj.toString(), SelectionFinishedMessage.class);
            if (sfm.getMessageBody().getClientID() == client.getClientID()) {
                client.getGameDataBridge().clearCardsInHand();
            }
        });

        handlerMap.put("TimerStarted", jsonObj -> {
            TimerStartedMessage tsm = JsonHandler.fromJson(jsonObj.toString(), TimerStartedMessage.class);
            Platform.runLater(() -> {
                client.getGameDataBridge().setTimerValue(30);
            });
        });

        handlerMap.put("TimerEnded", jsonObj -> {
            TimerEndedMessage tem = JsonHandler.fromJson(jsonObj.toString(), TimerEndedMessage.class);
            Platform.runLater(() -> client.getGameDataBridge().setDoneButtonDisabled(true));
            client.getGameDataBridge().setDragDisabled(true);
        });

        handlerMap.put("CardsYouGotNow", jsonObj -> {
            CardsYouGotNowMessage cygnm = JsonHandler.fromJson(jsonObj.toString(), CardsYouGotNowMessage.class);
            client.getGameDataBridge().handleCardsYouGotNow(cygnm.getMessageBody().getCards());
            client.getGameDataBridge().setRandomCards(cygnm.getMessageBody().getCards());
            client.getGameDataBridge().setErrorMessage("TOO SLOW\n\nRANDOM CARDS FOR YOU", -1);
        });

        handlerMap.put("Animation", jsonObj -> {
            AnimationMessage am = JsonHandler.fromJson(jsonObj.toString(), AnimationMessage.class);
            String animationType = am.getMessageBody().getType();
            if (animationType.equals("BlueConveyorBelt")) {
            } else if (animationType.equals("GreenConveyorBelt")) {
            } else if (animationType.equals(("PushPanel"))){
                SoundFX.playSoundEffect("pushPanel.wav");
                if(client.getGameDataBridge().getCurrentRegisterIndex()==1||client.getGameDataBridge().getCurrentRegisterIndex()==3||client.getGameDataBridge().getCurrentRegisterIndex()==5){
                    MapParser.animatePushPanels(client.getGameDataBridge(), "PushPanel135");
                } else {
                    MapParser.animatePushPanels(client.getGameDataBridge(), "PushPanel24");
                }
            } else if (animationType.equals(("Gear"))){
                Set<Node> gearNodes = client.getGameDataBridge().getGameMapGridPane().lookupAll(".gearTile");
                if (gearNodes.isEmpty()) {
                    logger.warning("No gear tiles found!");
                } else {
                    SoundFX.playSoundEffect("gears.wav");
                    for (Node gearNode : gearNodes) {
                        if (gearNode instanceof ImageView) {
                            boolean clockwise;
                            String rotationDirection = gearNode.getUserData().toString();
                            if(rotationDirection.equalsIgnoreCase("Clockwise")){
                                clockwise = true;
                            } else {
                                clockwise = false;
                            }
                            Platform.runLater(() -> Animation.animateGearRotation(gearNode, clockwise));
                        }
                    }
                }
            } else if (animationType.equals(("WallShooting"))){
                Platform.runLater(() -> MapParser.fireWallLasers(client.getGameDataBridge(),
                        client.getGameDataBridge().getGameMapGridPane(), (Client) client));
                SoundFX.playSoundEffect("boardlaser.wav");
            } else if (animationType.equals(("PlayerShooting"))){
                Platform.runLater(() -> MapParser.renderRobotLasers(client.getGameDataBridge().getGameMap(),
                        client.getGameDataBridge().getGameMapGridPane(), (Client) client));
                SoundFX.playSoundEffect("robotlaser.wav");
            } else if (animationType.equals(("EnergySpace"))){
            } else if (animationType.equals(("CheckPoint"))) {
            }
        });
    }
}