public class Bootstrap {
    public static void main(String[] args) throws Exception {
        try {
            Sender client = new Sender();
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
