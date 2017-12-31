package com.zf.sync.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CmdUtils {

    public static Process execShellInWait(String... commands) throws IOException {
        Process process = Runtime.getRuntime().exec("adb shell");
        writeCommands(process.getOutputStream(), true, commands);
        return process;
    }

    public static ExecResult execShell(String... commands) {
        try {
            Process process = Runtime.getRuntime().exec("adb shell");
            writeCommands(process.getOutputStream(), true, commands);
            List<String> errors = readAsList(process.getErrorStream());
            if (errors.size() > 0) {
                return new ExecResult(false, errors);
            } else {
                List<String> results = readAsList(process.getInputStream());
                return new ExecResult(true, results);
            }
        } catch (IOException e) {
            List<String> errors = Collections.singletonList(e.getMessage());
            return new ExecResult(false, errors);
        }
    }

    private static ExecResult execAdb(String command) {
        try {
            Process process = Runtime.getRuntime().exec("adb"); // adb
            writeCommands(process.getOutputStream(), true, command);
            List<String> errors = readAsList(process.getErrorStream());
            if (errors.size() > 0) {
                return new ExecResult(false, errors);
            } else {
                List<String> results = readAsList(process.getInputStream());
                return new ExecResult(true, results);
            }
        } catch (IOException e) {
            List<String> errors = Collections.singletonList(e.getMessage());
            return new ExecResult(false, errors);
        }
    }

    public static void readAndPrint(final InputStream inputStream) {
        new Thread(() -> {
            try {
                String line;
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
                IOUtils.closeQuietly(inputStream);
            }
        }).start();
    }

    public static List<String> readAsList(final InputStream inputStream) {
        try {
            List<String> list = new ArrayList<>();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String temp;
            while ((temp = reader.readLine()) != null) {
                list.add(temp);
            }

            return list;
        } catch (IOException e) {
            e.printStackTrace();
            IOUtils.closeQuietly(inputStream);
        }

        return new ArrayList<>();
    }

    private static String readAsString(final InputStream inputStream) {
        try {
            return new BufferedReader(new InputStreamReader(inputStream)).readLine();
        } catch (IOException e) {
            e.printStackTrace();
            IOUtils.closeQuietly(inputStream);
        }

        return null;
    }

    public static boolean isAppInstalled(String packageName) {
        ExecResult result = execShell("pm list packages | grep " + packageName);
        if (result.isSuccess()) {
            for (String name : result.getResults()) {
                name = name.replace("package:", "");
                if (name.contains(packageName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int getAppVersionCode(String packageName) {
        ExecResult result = execShell("dumpsys package " + packageName + " | grep versionCode");
        if (result.isSuccess()) {
            if (result.getResults().size() == 0) {
                return -1;
            }

            String[] infoArray = result.getResult().trim().split(" ");
            for (String item : infoArray) {
                if (item.contains("versionCode")) {
                    return Integer.parseInt(item.split("=")[1]);
                }
            }
        }

        return -1;
    }

    public static void install(String apkPath, InstallCallback callback) {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("adb install -r " + apkPath);
                int result = process.waitFor();
                if (result == 0) {
                    callback.onSuccess();
                } else {
                    List<String> errors = readAsList(process.getErrorStream());
                    callback.onFailed(errors);
                }
            } catch (IOException | InterruptedException e) {
                List<String> errors = Collections.singletonList(e.getMessage());
                callback.onFailed(errors);
            }
        }).start();
    }

    public static boolean installDirectly(String apkPath) {
        try {
            Process process = Runtime.getRuntime().exec("adb install -r " + apkPath);
            int result = process.waitFor();
            if (result == 0) {
                return true;
            } else {
                readAndPrint(process.getErrorStream());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void uninstall(String packageName, UninstallCallback callback) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Process process = Runtime.getRuntime().exec("adb uninstall " + packageName);
                    int result = process.waitFor();
                    if (result == 0) {
                        callback.onSuccess();
                    } else {
                        callback.onError("maybe app(" + packageName + ") is not exist");
                    }
                } catch (IOException | InterruptedException e) {
                    callback.onError(e.getMessage());
                }
            }
        }.start();
    }

    public static boolean uninstallDirectly(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("adb uninstall " + packageName);
            int result = process.waitFor();
            return result == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void writeCommands(OutputStream outputStream, boolean endWithExist, String... commands) {
        BufferedWriter bufferedWriter = null;

        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            for (String cmd : commands) {
                bufferedWriter.write(cmd);
                bufferedWriter.write("\n");
            }

            if (endWithExist) {
                bufferedWriter.write("exit\n");
            }
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            IOUtils.closeQuietly(bufferedWriter);
        }
    }

    public static void execCmd(String cmd) {
        System.out.println("---> " + cmd);

        BufferedWriter bufferedWriter = null;

        try {
            String startCmd;
            String os = System.getProperty("os.name");
            if (os.toLowerCase().startsWith("win")) {
                startCmd = "cmd";
            } else {
                startCmd = "sh";
            }

            Process process = Runtime.getRuntime().exec(startCmd);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            bufferedWriter.write(cmd);
            bufferedWriter.write("\n");
            bufferedWriter.write("exit\n");
            bufferedWriter.flush();
            process.waitFor();
            readAndPrint(process.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            IOUtils.closeQuietly(bufferedWriter);
        }
    }

    public static String getApkStorePath(String packageName) {
        if (!isAppInstalled(packageName)) {
            throw new RuntimeException("apk isn't installed yet");
        }

        String cmd = "if [ -d /data/app/" + packageName + "-1 ]\n" +
                "then\n" +
                "echo \"true\"\n" +
                "else\n" +
                "echo \"false\"\n" +
                "fi";

        ExecResult result = CmdUtils.execShell(cmd);
        if (result.isSuccess()) {
            boolean exist = Boolean.parseBoolean(result.getResult());
            if (exist) {
                return "/data/app/" + packageName + "-1/base.apk";
            } else {
                return "/data/app/" + packageName + "-2/base.apk";
            }
        }

        throw new RuntimeException(result.toString());
    }
}