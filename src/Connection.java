
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Connection implements Runnable {

    private Scanner sc = new Scanner(System.in);
    private BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

    private int port = 7158;//sc.nextInt();
    private String ad = "localhost";//keyboard.readLine();

    static Map<String,ArrayList<Byte>> logged = new HashMap<>();

    Connection() throws IOException {
    }


    @Override
    public void run() {
        System.out.println("Введите команду..");
        new Thread(new Cmd()).start();
        try {
            ServerSocket ss = new ServerSocket(port,0,InetAddress.getByName(ad));
            while (true) {
                Socket socket = ss.accept();
                String address = socket.getInetAddress().getHostAddress();
                ArrayList<Byte> pass = logged.get(address);
                InputStream s = socket.getInputStream();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s);
                User user = new User(in, out,pass,address);
                new Thread(user).start();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}