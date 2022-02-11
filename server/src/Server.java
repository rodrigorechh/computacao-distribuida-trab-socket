import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Server {

    private final int PORTA = 5003;
    private final String ENDERECO_PASTA_DESTINO_BACKUP = "./destinoBackup/";

    private ServerSocket serverSocket = null;
    private boolean socketAtivo = true;

    public void start() throws Exception {
        try {
            this.serverSocket = new ServerSocket(PORTA, 10);

            Socket socket = this.serverSocket.accept();

            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();

            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            while (this.socketAtivo) {
                String jsonArquivos = (String) input.readObject();
                System.out.println("Recebido: " + jsonArquivos);

                this.processaStringRecebida(jsonArquivos);

                output.writeObject("ACK");
                output.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processaStringRecebida(String json) throws Exception {
        JSONObject conteudo = new JSONObject(json);

        String type = conteudo.getString("type");

        if (type.equals("CLOSE"))
            this.processaFechamentoConexao();
        else if (type.equals("INIT")) {
            JSONArray payload = conteudo.getJSONArray("payload");
            this.processaCargaInicial(payload);
        } else if (type.equals("CREATE")) {
            JSONObject payload = conteudo.getJSONObject("payload");
            this.processaCriacaoArquivo(payload);
        } else if (type.equals("MODIFY")) {
            JSONObject payload = conteudo.getJSONObject("payload");
            this.processaModificacaoArquivo(payload);
        } else if (type.equals("DELETE")) {
            JSONObject payload = conteudo.getJSONObject("payload");
            this.processaExclusaoArquivo(payload);
        }
    }

    private void processaCriacaoArquivo(JSONObject json) throws Exception {
        System.out.println("Criado: " + json.getString("nome"));
    }

    private void processaModificacaoArquivo(JSONObject json) throws Exception {
        System.out.println("Modificado: " + json.getString("nome"));
    }

    private void processaExclusaoArquivo(JSONObject json) throws Exception {
        System.out.println("Deletado: " + json.getString("nome"));
    }

    private void processaCargaInicial(JSONArray json) throws Exception {
        Map<String, String> mapeamentoArquivos = this.obterMapeamentoArquivos(json);
        this.salvarArquivos(mapeamentoArquivos);
    }

    private void processaFechamentoConexao() throws Exception {
        this.socketAtivo = false;
        this.serverSocket.close();
    }

    private Map<String, String> obterMapeamentoArquivos(JSONArray jsonArquivos) {
        Map<String, String> arquivos = new HashMap<String, String>();

        for (int i = 0; i < jsonArquivos.length(); i++) {
            JSONObject jsonArquivo = jsonArquivos.getJSONObject(i);

            String nomeArquivo = (String) jsonArquivo.get("nome");
            String conteudoArquivo = (String) jsonArquivo.get("conteudo");
            arquivos.put(nomeArquivo, conteudoArquivo);
        }

        return arquivos;
    }

    private void criaPastarDestino() {
        File directory = new File(ENDERECO_PASTA_DESTINO_BACKUP);

        if (directory.exists())
            return;

        directory.mkdir();
    }

    private void salvarArquivos(Map<String, String> arquivos) throws Exception {
        this.criaPastarDestino();

        for (Map.Entry<String, String> arquivo : arquivos.entrySet()) {
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
