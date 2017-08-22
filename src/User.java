import MultiPlayer.Invite;
import MultiPlayer.Player;
import MultiPlayer.Profile;
import MultiPlayer.ServerCommand;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static sun.audio.AudioPlayer.player;

class User implements Runnable {

    ObjectInputStream in;
    ObjectOutputStream out;
    private ArrayList<Byte> pass;
    Profile profile;
    private String address;
    private long id;

    static volatile Map<ArrayList<Byte>, Profile> profiles = new HashMap<>();
    private static volatile ArrayList<Player> onlinePlayers = new ArrayList<>();
    private static volatile Map<Long, User> idUsers = new HashMap<>();
    private final static int LOG_OUT = 20;
    private final static int UPDATE_PLAYER_LIST = 25;
    private final static int INVITE_OPPONENT = 26;
    private final static int START_GAME_WITH_OPPONENT = 27;
    private final static int PLAYER_NOT_FOUND = 28;
    private final static int GET_PLAYER_PROFILE = 29;
    private final static int SET_YOUR_ID = 30;


    User(ObjectInputStream in, ObjectOutputStream out, ArrayList<Byte> pass, String address) {
        this.in = in;
        this.out = out;
        this.pass = pass;
        this.address = address;
    }

    @Override
    public void run() {
        try {
            System.out.println("Подключен игрок");
            if (pass == null) {
                out.writeObject(new ServerCommand(null, ServerCommand.GIVE_LOGIN_AND_PASS));
                out.flush();
                while (true) {
                    ServerCommand serverCommand = (ServerCommand) in.readObject();
                    pass = (ArrayList<Byte>) serverCommand.getData();
                    Profile profile = profiles.get(pass);
                    if (serverCommand.getCommand() == ServerCommand.CREATE_NEW_ACCOUNT) {
                        if (profile != null) {
                            out.writeObject(new ServerCommand(null, ServerCommand.ACCOUNT_ALREADY_EXISTS));
                            out.flush();
                        } else {
                            out.writeObject(new ServerCommand(null, ServerCommand.ACCOUNT_CREATED));
                            out.flush();
                            ServerCommand serverCommand1 = (ServerCommand) in.readObject();
                            if (serverCommand1.getCommand() == ServerCommand.UPDATE_PROFILE) {
                                setProfile((Profile) serverCommand1.getData());
                                Connection.logged.put(address, pass);
                            }
                            break;
                        }
                    } else if (serverCommand.getCommand() == ServerCommand.LOGIN) {
                        if (profile != null) {
                            out.writeObject(new ServerCommand(profile, ServerCommand.SET_PROFILE));
                            this.profile = profile;
                            out.flush();
                            Connection.logged.put(address,pass);
                            break;
                        } else {
                            out.writeObject(new ServerCommand(null, ServerCommand.UNCORRECTED_LOGIN_OR_PASSWORD));
                            out.flush();
                        }
                    }
                }
            } else {
                Profile profile = profiles.get(pass);
                out.writeObject(new ServerCommand(profile, ServerCommand.SET_PROFILE));
                this.profile = profile;
                out.flush();
            }

            add();

            out.writeObject(new ServerCommand(id, SET_YOUR_ID));
            out.flush();
            out.writeObject(new ServerCommand(onlinePlayers, UPDATE_PLAYER_LIST));
            out.flush();

            while (true) {
                ServerCommand serverCommand = (ServerCommand) in.readObject();
                int command = serverCommand.getCommand();
                Object data = serverCommand.getData();
                if (command == ServerCommand.UPDATE_PROFILE)
                    setProfile((Profile) serverCommand.getData());
                else if (command == LOG_OUT) {
                    System.out.println("Игрок вышел из аккаунта");
                    Connection.logged.remove(address);
                    remove();
                    return;
                }
                else if (command == UPDATE_PLAYER_LIST){
                    out.writeObject(new ServerCommand(new ArrayList<>(onlinePlayers), UPDATE_PLAYER_LIST));
                    out.flush();
                } else if (command == INVITE_OPPONENT){
                    try {
                        Invite invite = (Invite) data;
                        invite.setPlayer(new Player(id,profile.nick,profile.rating));
                        idUsers.get(invite.getToPlayerId()).out.writeObject(new ServerCommand(invite, INVITE_OPPONENT));
                        out.flush();
                    } catch (Exception e) {
                        out.writeObject(new ServerCommand(null, PLAYER_NOT_FOUND));
                        out.flush();
                    }
                } else if (command == START_GAME_WITH_OPPONENT){
                    Invite invite = (Invite) data;
                    User user = idUsers.get(invite.getPlayer().getId());
                    if (user != null) {
                        new Thread(new Game(user, this,invite.getTime(),invite.getSide())).start();
                        remove();
                        break;
                    } else {
                        out.writeObject(new ServerCommand(null, PLAYER_NOT_FOUND));
                        out.flush();
                    }
                } else if (command == GET_PLAYER_PROFILE){
                    try {
                        Player player = (Player) data;
                        out.writeObject(new ServerCommand(idUsers.get(player.getId()).profile, GET_PLAYER_PROFILE));
                        out.flush();
                    } catch (Exception e) {
                        out.writeObject(new ServerCommand(null, PLAYER_NOT_FOUND));
                        out.flush();
                    }
                }
            }

        } catch (Exception ignore){
            remove();
            System.out.println("Игрок отключен");
        }
    }

    void setProfile(Profile profile) {
        this.profile = profile;
        profiles.put(new ArrayList<>(pass),profile);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("Users/users.u"))) {
              oos.writeObject(profiles);
        } catch (Exception ignore) {}
    }

    private static Random random = new Random();

    private void add(){
        id = random.nextLong();
        idUsers.put(id,this);
        onlinePlayers.add(new Player(id,this.profile.nick,this.profile.rating));
    }

    private void remove(){
        idUsers.remove(id);
        onlinePlayers.remove(new Player(id,"",0));
    }
}
