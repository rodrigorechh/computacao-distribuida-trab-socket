import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public final static int PORTA = 5003;
    public final static byte[] BUFFER = new byte[100];

    public static void main(String[] args) throws Exception {
        try {
            ServerSocket serverSocket = new ServerSocket(PORTA, 10);

            Socket socket = serverSocket.accept();

            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();

            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            String msg = (String) input.readObject();
            System.out.println("Recebido: " + msg);

            output.writeObject("ACK");
            output.flush();

            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
