public class Bootstrap {
    public static void main(String[] args) {
        try {
            Server server = new Server();

            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
