package com.zf.sync;

import com.zf.sync.netty.ProtoBufClient;
import com.zf.sync.utils.CmdUtils;

import java.io.IOException;

public class Test {

    public static void main(String[] args) {
        String apkStorePath = CmdUtils.getApkStorePath("com.zf.sync");
        System.out.println(apkStorePath);
        String export = "export CLASSPATH=" + apkStorePath;
        String execute = "exec app_process $base/bin com.zf.sync.Main '$@'";
        String forwardPort = "adb forward tcp:8888 tcp:8888";

        try {
            Process process = CmdUtils.execShellInWait(export, execute);
            CmdUtils.execCmd(forwardPort);
            CmdUtils.readAndPrint(process.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            new ProtoBufClient().connect(8888, "127.0.0.1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
