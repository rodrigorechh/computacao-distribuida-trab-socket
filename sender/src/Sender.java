import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Sender {

    private final boolean HABILITAR_MULTICAST = false;

    private final String IP_GRUPO = "230.1.2.3";
    private final String CONTEUDO_REQ_MULTICAST = "ServerAddr";
    private final int PORTA = 5000;
    private final byte[] BUFFER = new byte[100];
    private final String ENDERECO_PASTA_ORIGEM_BACKUP = "./origemBackup/";

    private String IP_SERVIDOR_BACKUP = "";
    private int PORTA_SERVIDOR_BACKUP = 0;

    private Socket socket = null;
    private ObjectOutputStream output = null;
    private ObjectInputStream input = null;

    public void start() throws Exception {
        this.obterEnderecoServidorDataBackup();
        this.iniciarConexacoSocket();
        this.enviarEstadoInicialDeArquivos();
        this.monitorarPastaBackup();
    }

    private void obterEnderecoServidorDataBackup() throws Exception {
        if (HABILITAR_MULTICAST) {
            this.obterEnderecoNoMulticast();
        } else {
            this.IP_SERVIDOR_BACKUP = "127.0.0.1";
            this.PORTA_SERVIDOR_BACKUP = 5003;
        }
    }

    private void monitorarPastaBackup() throws Exception {
        File dir = new File(ENDERECO_PASTA_ORIGEM_BACKUP);

        Monitorador watcher = new Monitorador(dir.toPath());

        watcher.setCallbackAoCriar(path -> this.callbackArquivoCriado(path));
        watcher.setCallbackAoModificar(path -> this.callbackArquivoModificado(path));
        watcher.setCallbackAoDeletar(path -> this.callbackArquivoDeletado(path));

        Thread threadWatcher = new Thread(watcher);
        threadWatcher.start();
    }

    private void callbackArquivoCriado(Path path) {
        try {
            enviarAlteracaoArquivo("CREATE", path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callbackArquivoModificado(Path path) {
        try {
            enviarAlteracaoArquivo("MODIFY", path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callbackArquivoDeletado(Path path) {
        try {
            enviarAlteracaoArquivo("DELETE", path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enviarAlteracaoArquivo(String type, Path path) throws Exception {
        String conteudo = this.gerarStringAlteracaoArquivo(type, path);
        this.output.writeObject(conteudo);
        this.output.flush();

        String msg = (String) input.readObject();
        System.out.println("Recebido: " + msg);
    }

    private String obterNomeArquivo(Path path) {
        return "nome arquivo";
    }

    private String obterConteudoArquivo(Path path) {
        return "conteudo";
    }

    private String gerarStringAlteracaoArquivo(String type, Path path) throws Exception {
        JSONObject jsonArquivo = new JSONObject();
        jsonArquivo.put("nome", obterNomeArquivo(path));

        if (!type.equals("DELETE"))
            jsonArquivo.put("conteudo", obterConteudoArquivo(path));

        JSONObject jsonModificao = new JSONObject();

        jsonModificao.put("type", type);
        jsonModificao.put("payload", jsonArquivo);

        return jsonModificao.toString();
    }

    private void iniciarConexacoSocket() throws Exception {
        this.socket = new Socket(InetAddress.getByName(this.IP_SERVIDOR_BACKUP), this.PORTA_SERVIDOR_BACKUP);

        this.output = new ObjectOutputStream(this.socket.getOutputStream());
        this.output.flush();

        this.input = new ObjectInputStream(this.socket.getInputStream());
    }

    private void enviarEstadoInicialDeArquivos() throws Exception {
        String conteudo = this.gerarStringEnvioInicial();
        this.output.writeObject(conteudo);
        this.output.flush();

        String msg = (String) input.readObject();
        System.out.println("Recebido: " + msg);
    }

    private String gerarStringEnvioInicial() throws Exception {
        Map<String, String> arquivos = this.obterMapeamentoArquivos();

        JSONArray jsonArquivos = this.converterMapeamentoEmJSON(arquivos);

        JSONObject jsonInicial = new JSONObject();

        jsonInicial.put("type", "INIT");
        jsonInicial.put("payload", jsonArquivos);

        return jsonInicial.toString();
    }

    private void obterEnderecoNoMulticast() throws Exception {
        /** Enviar a Multicast */
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket pacote = criarPacoteMulticast();
        socket.send(pacote);

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

        this.IP_SERVIDOR_BACKUP = ipServidorDatastream;
        this.PORTA_SERVIDOR_BACKUP = portaServidorDatastream;
    }

    private DatagramPacket criarPacoteMulticast() throws UnknownHostException {
        InetAddress grupo = InetAddress.getByName(IP_GRUPO);
        return new DatagramPacket(CONTEUDO_REQ_MULTICAST.getBytes(), CONTEUDO_REQ_MULTICAST.getBytes().length, grupo,
                PORTA);
    }

    /*
     * Lê cada um dos arquivos salvo no path ENDERECO_PASTA_ORIGEM_BACKUP. Retorna
     * hash com chave = nomeArquivo e valor = conteudoArquivo
     */
    private Map<String, String> obterMapeamentoArquivos() throws Exception {
        Map<String, String> arquivos = new HashMap<String, String>();

        final File file = new File(ENDERECO_PASTA_ORIGEM_BACKUP);
        final File[] files = file.listFiles();
        for (final File f : files) {
            if (f.isDirectory())
                continue;

            /** Obtém conteúdo do arquivo lendo linha por linha */
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(f.getPath()))) {
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    contentBuilder.append(sCurrentLine);
                }
            }

            /** Salva no hash o conteúdo obtido */
            String nomeArquivo = f.getName();
            String conteudoArquivo = contentBuilder.toString();
            arquivos.put(nomeArquivo, conteudoArquivo);
        }

        return arquivos;
    }

    private JSONArray converterMapeamentoEmJSON(Map<String, String> arquivos) {
        JSONArray jsonArquivos = new JSONArray();

        for (var arquivo : arquivos.entrySet()) {
            JSONObject jsonArquivo = new JSONObject();

            jsonArquivo.put("nome", arquivo.getKey());
            jsonArquivo.put("conteudo", arquivo.getValue());

            jsonArquivos.put(jsonArquivo);
        }

        return jsonArquivos;
    }
}
