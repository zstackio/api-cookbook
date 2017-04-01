import org.zstack.sdk.LogInByAccountAction;
import org.zstack.sdk.ZSClient;
import org.zstack.sdk.ZSConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by heathhose on 17-3-2.
 */
public class TestLoginByAccount {
    public static String SHA(final String strText, final String strType) {
        String strResult = null;
        if (strText != null && strText.length() > 0) {
            try {
                // SHA 加密开始
                // 创建加密对象 并传入加密类型
                MessageDigest messageDigest = MessageDigest.getInstance(strType);
                // 传入要加密的字符串
                messageDigest.update(strText.getBytes());
                // 得到 byte 类型结果
                byte byteBuffer[] = messageDigest.digest();
                // 将 byte 转换为 string
                StringBuffer strHexString = new StringBuffer();
                // 遍历 byte buffer
                for (int i = 0; i < byteBuffer.length; i++) {
                    String hex = Integer.toHexString(0xff & byteBuffer[i]);
                    if (hex.length() == 1) {
                        strHexString.append('0');
                    }
                    strHexString.append(hex);
                }
                // 得到返回结果
                strResult = strHexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        return strResult;
    }

    public static void main(String[] args) {

        //声明sessionId；每个action均需要sessionId
        String sessionId = null;

        //设置登录zstack的地址；通过172.20.12.4连接到部署zstack管理节点环境的主机
        ZSConfig.Builder zBuilder = new ZSConfig.Builder();
        zBuilder.setHostname("10.0.204.52");
        ZSClient.configure(zBuilder.build());

        //登录zstack；获取session
        LogInByAccountAction logInByAccountAction = new LogInByAccountAction();
        logInByAccountAction.accountName = "admin";
        logInByAccountAction.password = SHA("password", "SHA-512");
        LogInByAccountAction.Result logInByAccountActionRes = logInByAccountAction.call();

        if (logInByAccountActionRes.error == null) {
            System.out.println("logInByAccount successfully");
            sessionId = logInByAccountActionRes.value.getInventory().getUuid();
        } else logInByAccountActionRes.throwExceptionIfError();
    }
}
