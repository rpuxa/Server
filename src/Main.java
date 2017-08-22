import java.io.IOException;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Сервер запущен!");
        new Thread(new Connection()).start();
    }
}
