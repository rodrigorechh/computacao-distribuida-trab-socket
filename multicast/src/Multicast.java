import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.json.JSONObject;

public class Multicast {

    public final static String IP_SERVIDOR_DATASTREAM = "127.0.0.1";
    public final static int PORTA_SERVIDOR_DATASTREAM = 5003;

    public final static String IP_GRUPO = "230.1.2.3";
    public final static int PORTA_GRUPO = 5000;
    public final static byte[] BUFFER = new byte[100];

    public static void main(String[] args) throws Exception {
        /** Entrar em grupo e esperar contato do Sender */
        MulticastSocket msocket = new MulticastSocket(PORTA_GRUPO);
        InetAddress grupo = InetAddress.getByName(IP_GRUPO);
        msocket.joinGroup(grupo);
        DatagramPacket pacote = new DatagramPacket(BUFFER, BUFFER.length);
        do {
            msocket.receive(pacote);

            /** Responder Sender */
            InetAddress enderecoSender = pacote.getAddress();
            int portaSender = pacote.getPort();
            var resposta = criarResposta().getBytes();
            var pacoteResposta = new DatagramPacket(resposta, resposta.length, enderecoSender, portaSender);
            msocket.send(pacoteResposta);
        } while (true);

        // msocket.close();
    }

    public static String criarResposta() {
        JSONObject json = new JSONObject();

        json.put("ip", IP_SERVIDOR_DATASTREAM);
        json.put("porta", PORTA_SERVIDOR_DATASTREAM);

        return json.toString();
    }
}
