import com.zf.sync.utils.CmdUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class AdbUtilsTest {

    @Test
    public void test_isAppInstalled() {
        boolean installed = CmdUtils.isAppInstalled("com.zf.sync");
        System.out.println("installed: " + installed);
    }

    @Test
    public void test_getAppVersionCode() {
        int code = CmdUtils.getAppVersionCode("com.zf.sync");
        System.out.println("versionCode: " + code);
    }

    @Test
    public void test_install() {
        String apkPath = System.getProperty("user.dir") + "/src/res/app-release.apk";
        boolean installed = CmdUtils.installDirectly(apkPath);
        Assert.assertTrue(installed);
    }

    @Test
    public void test_uninstall() {
        boolean result = CmdUtils.uninstallDirectly("com.zf.sync");
        Assert.assertTrue(result);
    }

    @Test
    public void test_check_package() {
        String packageName = "com.zf.sync";
        String path = CmdUtils.getApkStorePath(packageName);
        System.out.println(path);
    }

    @Test
    public void test(){
        short bodyLength = Short.MAX_VALUE;
        byte[] header = new byte[2];
        header[0] = (byte) (bodyLength & 0xff);
        header[1] = (byte) ((bodyLength >> 8) & 0xff);

        byte low = header[0];
        byte high = header[1];
        short s0 = (short) (low & 0xff);
        short s1 = (short) (high & 0xff);

        s1 <<= 8;
        short length = (short) (s0 | s1);
        System.out.println(length);
    }

}
