import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class GithubHookServer {
    public static void main(String[] args) {
        int port = 1101;
        ServerSocket server;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("启动服务失败!");
            e.printStackTrace();
            return;
        }
        System.out.println("启动服务成功!");

        while (true) {
            Socket socket = null;
            BufferedReader bufferedReader = null;
            OutputStream outputStream = null;
            byte[] responseData = "HTTP/1.1 400\r\n\r\nNothing to Change".getBytes();
            try {
                System.out.println("监听端口[" + port + "]中...");
                socket = server.accept();
                System.out.println("监听到端口[" + port + "]请求。");
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = socket.getOutputStream();

                var contentLength = -1;

                String line;
                while ((line = bufferedReader.readLine()) != null && line.length() > 0) {
                    if (line.toLowerCase().startsWith("content-length:"))
                        contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
                }

                if (contentLength <= 0) {
                    System.out.println("contentLength : " + contentLength + ", continue!");
                    continue;
                }

                char[] data = readByLength(bufferedReader, contentLength);
                if (data == null) {
                    System.out.println("解析请求体为空, continue!!!");
                    continue;
                }

                if (service(new String(data)))
                    responseData = "HTTP/1.1 200 OK\r\n\r\nOK".getBytes();
            } catch (Exception e) {
                responseData = ("HTTP/1.1 500\r\n\r\n" + e.getMessage()).getBytes();
            } finally {
                if (outputStream != null)
                    try {
                        outputStream.write(responseData);
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                    }
                if (bufferedReader != null)
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                    }
                if (socket != null)
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
            }
        }
    }

    private final static char[] readByLength(Reader reader, int dataLength) throws IOException {
        char[] data = new char[dataLength];

        int count = 0;
        while (count < dataLength) {
            int dataSize = reader.read(data, count, dataLength - count);
            if (dataSize == -1)
                break;

            for (var i = 0; i< dataSize; ++i)
                if (data[count + i] > (Byte.MIN_VALUE & 0xff))
                    dataLength -= 2;
            count += dataSize;
        }


        return count == dataLength ? data : null;
    }

    private final static boolean service(String data) {
        var fullName = getByJsonString("full_name", data);
        if (fullName == null || fullName.length() == 0) {
            System.out.println("解析Json，查找full_name失败, Json -> " + data);
            return false;
        }
        String command = getCommand(fullName);
        if (command == null || command.length() == 0) {
            System.out.println("获取command失败, full_name -> " + fullName);
            return false;
        }

        System.out.println("##################################");
        System.out.println(fullName);
        System.out.println("##################################");
        System.out.println("`command` -> " + command);
        System.out.println("##################################");

        OutputStream os = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("/bin/bash");
            os = process.getOutputStream();
            os.write(command.getBytes());
            os.flush();
            appender(process);
        } catch (IOException e) {
            if (process != null)
                process.destroy();
            System.out.println("传输命令失败");
            e.printStackTrace();
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }

        return true;
    }

    private final static String getCommand(String fullName) {
        try (var inputStream = new FileInputStream(new File("shell.properties"))) {
            Properties prop = new Properties();
            prop.load(inputStream);
            String command = prop.getProperty(fullName);
            return command;
        } catch (IOException e) {
            return null;
        }
    }

    private final static void appender(Process process) {
        if (process == null)
            return;
        new Thread(() -> {
            System.out.println("new appender...");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println("result -> " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (process != null)
                    process.destroy();
            }
            System.out.println("appender destroy...");
        }).start();
    }

    private final static String getByJsonString(String key, String json) {
        var value = json.substring(json.indexOf(key));
        value = value.substring(value.indexOf(":"));
        int index1 = value.indexOf(",") > 0 ? value.indexOf(",") : value.length();
        int index2 = value.indexOf("}") > 0 ? value.indexOf("}") : value.length();
        value = value.substring(1, Math.min(index1, index2)).trim();
        if (value.startsWith("\"") && value.endsWith("\""))
            value = value.substring(1, value.length() - 1).trim();
        return value;
    }
}
