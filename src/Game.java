import MultiPlayer.Invite;
import MultiPlayer.Profile;
import MultiPlayer.ServerCommand;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.util.Random;


public class Game implements Runnable {

    private User player1;
    private User player2;
    private int time;
    private int sideFirstPlayer;

    Game(User player1, User player2, int time, int sideFirstPlayer) {
        this.player1 = player1;
        this.player2 = player2;
        this.time = time;
        this.sideFirstPlayer = sideFirstPlayer;
    }

    private static final Random random = new Random();

    @Override
    public void run() {
        try {
            System.out.println("Сессия создана");
            if (random.nextBoolean() && sideFirstPlayer == Invite.RANDOM || sideFirstPlayer == Invite.WHITE) {
                player1.out.writeObject(new ServerCommand(time,ServerCommand.BEGIN_GAME_FOR_WHITE));
                player1.out.flush();
                player2.out.writeObject(new ServerCommand(time,ServerCommand.BEGIN_GAME_FOR_BLACK));
                player2.out.flush();
            } else {
                player2.out.writeObject(new ServerCommand(time,ServerCommand.BEGIN_GAME_FOR_WHITE));
                player2.out.flush();
                player1.out.writeObject(new ServerCommand(time,ServerCommand.BEGIN_GAME_FOR_BLACK));
                player1.out.flush();
            }
            player1.out.writeObject(new ServerCommand(player2.profile,ServerCommand.SET_OPPONENT_PROFILE));
            player1.out.flush();
            player2.out.writeObject(new ServerCommand(player1.profile,ServerCommand.SET_OPPONENT_PROFILE));
            player2.out.flush();

            Thread pl1 = newStream(player1, player2);
            pl1.start();
            Thread pl2 = newStream(player2, player1);
            pl2.start();
            pl1.join();
            pl2.join();
        } catch (Exception ignored){
ignored.printStackTrace();
        }
        System.out.println("Сессия прервана");
    }

    private Thread newStream(User player1, User player2){
        return new Thread(() -> {
            try {
                while (true) {
                    Object obj = player1.in.readObject();
                  //  System.out.println(obj.getClass().getName());
                  //  if (obj instanceof ObjectStreamClass)
                 //       obj = obj;
                    ServerCommand serverCommand = (ServerCommand) obj;
                    if (serverCommand.getCommand() == ServerCommand.UPDATE_PROFILE){
                        player1.setProfile((Profile) serverCommand.getData());
                    } else {
                        player2.out.writeObject(serverCommand);
                        player2.out.flush();
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println(Thread.currentThread().getName() + "   " + e.getMessage());
                try {
                    player1.out.writeObject(new ServerCommand(null,ServerCommand.LOST_CONNECTION));
                } catch (IOException ignored) {
                }
                try {
                    player2.out.writeObject(new ServerCommand(null,ServerCommand.LOST_CONNECTION));
                } catch (IOException ignored) {
                }
                try {
                    player1.in.close();
                    player1.out.close();
                } catch (IOException ignored) {
                }
                try {
                player2.in.close();
                player2.out.close();
            } catch (IOException ignored) {
            }
            }
        });
    }
}
