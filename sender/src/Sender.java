import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Joiner;

import org.json.JSONObject;

public class Sender {

    public final static String IP_GRUPO = "230.1.2.3";
    public final static String MENSAGEM = "Hello!";
    public final static int PORTA = 5000;
    public final static byte[] BUFFER = new byte[100];
    public final static String ENDERECO_PASTA_ORIGEM_BACKUP = "./origemBackup/";

    public static void main(String[] args) throws Exception {
        String connection = obterConexaoServer();     
        String[] spliteConnection = connection.split(":");

        String ip = spliteConnection[0];
        int porta = Integer.parseInt(spliteConnection[1]);

        conectarSocketStream(ip, porta);
    }

    public static void conectarSocketStream(String ip, int porta) throws Exception {
        Socket socket = new Socket(InetAddress.getByName(ip), porta);

        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();

        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
        
        var mensagemEnviar = hashmapToString(encontrarArquivos());
        output.writeObject(mensagemEnviar);
        output.flush();
        
        String msg = (String) input.readObject();
        System.out.println("Recebido: " + msg);
    }

    public static String obterConexaoServer() throws Exception {
        /** Enviar a Multicast */
        var socket = new DatagramSocket();
        socket.send(criarPacoteMulticast());

        /** Obter retorno Multicast */
        DatagramPacket reply = new DatagramPacket(BUFFER, BUFFER.length);
        socket.receive(reply);
        socket.close();

        String respostaMulticast = new String(reply.getData(), 0, reply.getLength());
        JSONObject json = new JSONObject(respostaMulticast);
        String ipServidorDatastream = (String) json.get("ip");
        int portaServidorDatastream = (int) json.get("porta");

        System.out.println(
                "Dados recebidos do Multicast -> ip: " + ipServidorDatastream + " porta: " + portaServidorDatastream);

        return ipServidorDatastream + ":" + portaServidorDatastream;
    }

    public static DatagramPacket criarPacoteMulticast() throws UnknownHostException {
        InetAddress grupo = InetAddress.getByName(IP_GRUPO);
        return new DatagramPacket(MENSAGEM.getBytes(), MENSAGEM.getBytes().length, grupo, PORTA);
    }

    /*Lê cada um dos arquivos salvo no path ENDERECO_PASTA_ORIGEM_BACKUP. Retorna hash com chave = nomeArquivo e valor = conteudoArquivo*/
    public static Map<String,String> encontrarArquivos() throws FileNotFoundException, IOException {
        Map<String,String> arquivos = new HashMap<String,String>();

        final File file = new File(ENDERECO_PASTA_ORIGEM_BACKUP);
        final File[] files = file.listFiles();
        for (final File f : files) {
            if(f.exists()) {//valida se não é um diretório
                /**Obtém conteúdo do arquivo lendo linha por linha */
                StringBuilder contentBuilder = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(f.getPath()))) {
                    String sCurrentLine;
				    while ((sCurrentLine = br.readLine()) != null) {
					    contentBuilder.append(sCurrentLine).append("\n");
                    }
                }

                /**Salva no hash o conteúdo obtido*/
                String nomeArquivo = f.getName();
                String conteudoArquivo = contentBuilder.toString();
                arquivos.put(nomeArquivo, conteudoArquivo);
            }
        }

        return arquivos;
    }

    public static String hashmapToString(Map<String, String> map) {
        return Joiner.on(",").withKeyValueSeparator("=").join(map);
    }
}
