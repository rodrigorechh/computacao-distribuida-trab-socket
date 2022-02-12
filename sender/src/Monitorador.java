import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

interface WatcherHandler {
    public void onCreateHandle(Path path);

    public void onEditHandle(Path path);

    public void onDeleteHandle(Path path);
}

interface Callback {
    void callback(Path path);
}

public class Monitorador implements Runnable {
    private Path caminhoPasta = null;

    private Callback callbackAoCriar = null;
    private Callback callbackAoModificar = null;
    private Callback callbackAoDeletar = null;

    public Monitorador(Path path) {
        this.caminhoPasta = path;
    }

    public void setCallbackAoCriar(Callback callback) {
        this.callbackAoCriar = callback;
    }

    public void setCallbackAoModificar(Callback callback) {
        this.callbackAoModificar = callback;
    }

    public void setCallbackAoDeletar(Callback callback) {
        this.callbackAoDeletar = callback;
    }

    @Override
    public void run() {
        try {
            monitorarDiretorio(this.caminhoPasta);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void verificarPasta(Path caminho) throws Exception {
        Boolean ePasta = (Boolean) Files.getAttribute(caminho,
                "basic:isDirectory", NOFOLLOW_LINKS);

        if (!ePasta) {
            throw new IllegalArgumentException("Path: " + caminho
                    + " is not a folder");
        }
    }

    public void monitorarDiretorio(Path caminho) throws Exception {
        this.verificarPasta(caminho);

        System.out.println("Monitorando pasta: " + caminho);

        FileSystem fs = caminho.getFileSystem();

        try (WatchService service = fs.newWatchService()) {
            caminho.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

            WatchKey key = null;

            while (true) {
                key = service.take();

                Kind<?> kind = null;
                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    kind = watchEvent.kind();

                    if (OVERFLOW == kind)
                        continue;

                    Path caminhoEvento = ((WatchEvent<Path>) watchEvent).context();

                    if (ENTRY_CREATE == kind)
                        this.resolveArquivoCriado(caminhoEvento);
                    else if (ENTRY_MODIFY == kind)
                        this.resolveArquivoModificado(caminhoEvento);
                    else if (ENTRY_DELETE == kind)
                        this.resolveArquivoDeletado(caminhoEvento);
                }

                if (!key.reset())
                    break;
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void resolveArquivoCriado(Path path) {
        this.callbackAoCriar.callback(path);
    }

    private void resolveArquivoModificado(Path path) {
        this.callbackAoModificar.callback(path);
    }

    private void resolveArquivoDeletado(Path path) {
        this.callbackAoDeletar.callback(path);
    }
}