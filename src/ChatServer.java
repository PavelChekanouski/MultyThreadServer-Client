import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatServer {
    private ServerSocket ss;
    private Thread serverThread;
    private int port;
    BlockingQueue<SocketProcessor> q = new LinkedBlockingQueue<SocketProcessor>();

    /**
     * Конструктор объекта сервера
     * @param port Порт, где будем слушать входящие сообщения.
     * @throws IOException Если не удасться создать сервер-сокет, вылетит по эксепшену, объект Сервера не будет создан
     */
    public ChatServer(int port) throws IOException {
        ss = new ServerSocket(port);
        this.port = port;
    }

    void run() {
        serverThread = Thread.currentThread();
        while (true) {
            Socket s = getNewConn();
            if (serverThread.isInterrupted()) {

                break;
            } else if (s != null){
                try {
                    final SocketProcessor processor = new SocketProcessor(s);
                    final Thread thread = new Thread(processor);
                    thread.setDaemon(true);
                    thread.start();
                    q.offer(processor);
                }
                catch (IOException ignored) {}
            }
        }
    }

    /**
     * Ожидает новое подключение.
     * @return Сокет нового подключения
     */
    private Socket getNewConn() {
        Socket s = null;
        try {
            s = ss.accept();
        } catch (IOException e) {
            shutdownServer();
        }
        return s;
    }

    /**
     * метод "убийства" сервера
     */
    private synchronized void shutdownServer() {
        for (SocketProcessor s: q) {
            s.close();
        }
        if (!ss.isClosed()) {
            try {
                ss.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * входная точка программы
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        new ChatServer(45000).run();
    }
    /**
     * вложенный класс асинхронной обработки одного коннекта.
     */
    private class SocketProcessor implements Runnable{
        Socket s;
        BufferedReader br;
        BufferedWriter bw;
        /**
         * Сохраняем сокет, пробуем создать читателя и писателя. Если не получается - вылетаем без создания объекта
         * @param socketParam сокет
         * @throws IOException Если ошибка в создании br || bw
         */
        SocketProcessor(Socket socketParam) throws IOException {
            s = socketParam;
            br = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8") );
        }

        /**
         * Главный цикл чтения сообщений/рассылки
         */
        public void run() {
            while (!s.isClosed()) {
                String line = null;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    close();
                }

                if (line == null) {
                    close();
                } else if ("shutdown".equals(line)) {
                    serverThread.interrupt();
                    try {
                        new Socket("localhost", port);
                    } catch (IOException ignored) {
                    } finally {
                        shutdownServer();
                    }
                } else {
                    for (SocketProcessor sp:q) {
                        sp.send(line);
                    }
                }
            }
        }

        /**
         * Метод посылает в сокет полученную строку
         * @param line строка на отсылку
         */
        public synchronized void send(String line) {
            try {
                bw.write(line);
                bw.write("\n");
                bw.flush();
            } catch (IOException e) {
                close();
            }
        }

        /**
         * метод аккуратно закрывает сокет и убирает его со списка активных сокетов
         */
        public synchronized void close() {
            q.remove(this);
            if (!s.isClosed()) {
                try {
                    s.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
 