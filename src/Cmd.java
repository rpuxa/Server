import MultiPlayer.Profile;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class Cmd implements Runnable {
    private BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

    private static final String LOAD_USERS = "/load users";
    private static final String STOP = "/stop";
    private static final String SAVE_USERS = "/save users";

    @Override
    public void run() {
        while (true) {
            try {
                String cmd = keyboard.readLine();
                switch (cmd) {
                    case LOAD_USERS: {
                        try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream("Users/users.u"))) {
                            User.profiles = (Map<ArrayList<Byte>, Profile>) oos.readObject();
                            System.out.println("Загрузка прошла успешно!");
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Ошибка загрузки");
                        }
                        break;
                    }

                    case SAVE_USERS: {
                        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("Users/users.u"))) {
                            oos.writeObject(User.profiles);
                            System.out.println("Сохранение прошло успешно!");
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Ошибка сохранения");
                        }
                        break;
                    }

                    case STOP: {
                        System.out.println("Остановка сервера!");
                        System.exit(0);
                        break;
                    }

                    default: {
                        System.out.println("Неизвестная команда");
                        break;
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }
}
