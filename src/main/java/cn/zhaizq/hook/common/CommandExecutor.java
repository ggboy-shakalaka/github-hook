package cn.zhaizq.hook.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CommandExecutor {
    public static void execute(String... commands) throws IOException {
        if (commands == null)
            return;

        for (String command : commands) {
            System.out.println(String.format("[command]-> `%s`", command));
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(command);
                outputAppender(process.getInputStream());
                outputAppender(process.getErrorStream());
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
            System.out.println("[command end]");
        }
    }

    private static void outputAppender(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(String.format("[result]-> `%s`", line));
        }
    }
}