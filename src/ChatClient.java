import java.net.Socket;
import java.io.*;

/**
 * Класс-клиент чат-сервера. Работает в консоли. Командой с консоли shutdown посылаем сервер в оффлайн
 */
public class ChatClient {
    final Socket s;
    final BufferedReader socketReader;
    final BufferedWriter socketWriter;
    final BufferedReader userInput;
    /**
     * Конструктор объекта клиента
     * @param host - IP адрес или localhost или доменное имя
     * @param port - порт, на котором висит сервер
     * @throws java.io.IOException - если не смогли приконнектиться, кидается исключение, чтобы
     * предотвратить создание объекта
     */
    public ChatClient(String host, int port) throws IOException {
        s = new Socket(host, port);
        socketReader = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        socketWriter = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));

        userInput = new BufferedReader(new InputStreamReader(System.in));
        new Thread(new Receiver()).start();
    }

    /**
     * метод, где происходит главный цикл чтения сообщений с консоли и отправки на сервер
     */
    public void run() {
        System.out.println("Type phrase(s) (hit Enter to exit):");
        while (true) {
            String userString = null;
            try {
                userString = userInput.readLine();
            } catch (IOException ignored) {}
            if (userString == null || userString.length() == 0 || s.isClosed()) {
                close();
                break;
            } else {
                try {
                    socketWriter.write(userString);
                    socketWriter.write("\n");
                    socketWriter.flush();
                } catch (IOException e) {
                    close();
                }
            }
        }
    }

    /**
     * метод закрывает коннект и выходит из
     * программы (это единственный  выход прервать работу BufferedReader.readLine(), на ожидании пользователя)
     */
    public synchronized void close() {
        if (!s.isClosed()) {
            try {
                s.close();
                System.exit(0);
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public static void main(String[] args)  {
        try {
            new ChatClient("localhost", 45000).run();
        } catch (IOException e) {
            System.out.println("Unable to connect. Server not running?");
        }
    }

    /**
     * Вложенный приватный класс асинхронного чтения
     */
    private class Receiver implements Runnable{
        /**
         * run() вызовется после запуска нити из конструктора клиента чата.
         */
        public void run() {
            while (!s.isClosed()) {
                String line = null;
                try {
                    line = socketReader.readLine();
                } catch (IOException e) {
                    if ("Socket closed".equals(e.getMessage())) {
                        break;
                    }
                    System.out.println("Connection lost");
                    close();
                }
                if (line == null) {
                    System.out.println("Server has closed connection");
                    close();
                } else {
                    System.out.println("Server:" + line);
                }
            }
        }
    }
}
 