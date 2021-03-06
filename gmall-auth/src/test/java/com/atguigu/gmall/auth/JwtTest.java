import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
    private static final String pubKeyPath = "D:\\project\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\project\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "2edtbgrtgn34");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1OTY1NTA4OTh9.rJxy8C8AobtbB8tnmSRdfB_QqPQSsUBv8H2a4tQh5kBOkBvLQPLLP5mkwo-MPGJNatcBMeJQaLHVX7PnsLCxN9yP9BmBZkayskREndCedOI0gG8DAiyzdE1T25vu262glk0npmXhn2vmGssgFZd-TkVI1eDkkIl1wxAgZ_49GnaHooiAJHkSFAArXz2ikWI5cH_l2ohG7ChNYJ2fjTymRx3U-_O6CNJ42DEG3X51ApEzVzaPqQF_LLFbsqLQTeCrFR0KGQjGK0ObMQTZYAeCSOqPHZpdNryNWDymu1Vs1XRDqGAim5LIjR18xjYLejCeXZSIpiVTVmQ-U0IjGksZ5w";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}