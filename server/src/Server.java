import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import com.google.common.base.Splitter;

public class Server {

    public final static int PORTA = 5003;
    public final static byte[] BUFFER = new byte[100];
    public final static String ENDERECO_PASTA_DESTINO_BACKUP = "./destinoBackup/";
    public static void main(String[] args) throws Exception {
        try {
            ServerSocket serverSocket = new ServerSocket(PORTA, 10);

            Socket socket = serverSocket.accept();

            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();

            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            String msg = (String) input.readObject();
            System.out.println("Recebido: " + msg);

            salvarArquivos(stringToHashmap(msg));

            output.writeObject("ACK");
            output.flush();

            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> stringToHashmap(String mapAsString) {
        return Splitter.on(',').withKeyValueSeparator('=').split(mapAsString);
    }

    public static void salvarArquivos(Map<String, String> arquivos) throws IOException {
        for(Map.Entry<String, String> arquivo : arquivos.entrySet()) {
            String nomeArquivo = arquivo.getKey();
            String conteudoArquivo = arquivo.getValue();

            FileOutputStream fos = new FileOutputStream(ENDERECO_PASTA_DESTINO_BACKUP + nomeArquivo);
            PrintWriter pr = new PrintWriter(fos);

            pr.println(conteudoArquivo);
            pr.close();
            fos.close();
        }
        System.out.println("Arquivos salvos!");
    }
}
