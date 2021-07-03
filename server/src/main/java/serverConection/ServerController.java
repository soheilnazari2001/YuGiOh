package serverConection;

import controller.menus.MainMenuController;
import controller.menus.RegisterMenuController;
import models.Player;
import org.ietf.jgss.Oid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerController {

    private static HashMap<String, Socket> socketHashMap = new HashMap<>();
    private static final String[] regexes = {
            "^user create (--username|-u) (?<username>\\w+) (--password|-p) (?<password>\\w+) (--nickname|-n) (?<nickname>\\w+)$",
            "^user login (--username|-u) (?<username>\\w+) (--password|-p) (?<password>\\w+)$",
            "^logout (?<token>\\S+)$",
            "^new three-rounded (?<token>\\S+)$",
            "^new one-rounded (?<token> \\S+)$"

    };

    public static void registerSocket(Socket socket, String token) {
    socketHashMap.put(token , socket);
    }

    public static void getInputFromClient(DataInputStream dataInputStream, DataOutputStream dataOutputStream , Socket socket) throws IOException, IOException {
        while (true) {
            String command = dataInputStream.readUTF();
            System.out.println("message from " + command);
            String message = command.split(":")[1];
            String result = processCommand(message , socket);
            if (result.equals("")) return;
            dataOutputStream.writeUTF(result);
            dataOutputStream.flush();
        }
    }

    private static String processCommand(String command , Socket socket) {
        Matcher commandMatcher;
        int whichCommand;
        for (whichCommand = 0; whichCommand < regexes.length; whichCommand++) {
            commandMatcher = findMatcher(command, regexes[whichCommand]);
            if (commandMatcher.find())
                return executeCommands(commandMatcher, whichCommand , socket);

        }
        return "";

    }

    private String sendMessageToSocket(String token, String message) {
        Socket socket = socketHashMap.get(token);
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(message);
            return dataInputStream.readUTF();
        } catch (Exception ignored) {
            return "Error";
        }
    }

    private static String executeCommands(Matcher commandMatcher, int witchCommand , Socket socket) {
        switch (witchCommand) {
            case 0:
                return RegisterMenuController.getInstance().createUser(commandMatcher.group("username"),
                        commandMatcher.group("nickname"), commandMatcher.group("password"));
            case 1:
                String result =  RegisterMenuController.getInstance().login(commandMatcher.group("username"), commandMatcher.group("password"));
                String token = result.split(": ")[1];
                ServerController.registerSocket(socket , token);
                return result;
            case 2:
                return MainMenuController.getInstance().logout(commandMatcher.group("token"));
            case 3:
                return MainMenuController.getInstance().registerOnGame(true, commandMatcher.group("token"));
            case 4:
                return MainMenuController.getInstance().registerOnGame(false, commandMatcher.group("token"));
        }
        return "";
    }

    private static Matcher findMatcher(String input, String regex) {

        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(input);
    }

}