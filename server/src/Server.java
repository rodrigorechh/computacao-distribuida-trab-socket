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

    public void start() throws Exception {
        try {
            ServerSocket serverSocket = new ServerSocket(PORTA, 10);

            Socket socket = serverSocket.accept();

            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();

            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            String jsonArquivos = (String) input.readObject();
            System.out.println("Recebido: " + jsonArquivos);

            this.provessarStringRecebida(jsonArquivos);

            output.writeObject("ACK");
            output.flush();

            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void provessarStringRecebida(String json) throws Exception {
        Map<String, String> mapeamentoArquivos = this.obterMapeamentoArquivos(json);
        this.salvarArquivos(mapeamentoArquivos);
    }

    private Map<String, String> obterMapeamentoArquivos(String json) {
        JSONArray jsonArquivos = new JSONArray(json);
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
