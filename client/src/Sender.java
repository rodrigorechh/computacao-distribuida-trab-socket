import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONObject;

public class Sender {

    public final static String IP_GRUPO = "230.1.2.3";
    public final static String MENSAGEM = "Hello!";
    public final static int PORTA = 5000;
    public final static byte[] BUFFER = new byte[100];

    public static void main(String[] args) throws Exception {    
        
        /**Enviar a Multicast */
        var socket = new DatagramSocket();
        socket.send(criarPacoteMulticast());

        /**Obter retorno Multicast */
        DatagramPacket reply = new DatagramPacket(BUFFER, BUFFER.length);
        socket.receive(reply);
        socket.close();

        String respostaMulticast = new String(reply.getData(), 0, reply.getLength());
        JSONObject json = new JSONObject(respostaMulticast);
        String ipServidorDatastream = (String) json.get("ip");
        int portaServidorDatastream = (int) json.get("porta");

        System.out.println("Dados recebidos do Multicast -> ip: " + ipServidorDatastream + " porta: " + portaServidorDatastream);
    }

    public static DatagramPacket criarPacoteMulticast() throws UnknownHostException {
        InetAddress grupo = InetAddress.getByName(IP_GRUPO);
        return new DatagramPacket(MENSAGEM.getBytes(), MENSAGEM.getBytes().length, grupo, PORTA);
    }
}
