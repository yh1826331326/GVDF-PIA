package Vehicle_Digital_Evidence;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

public class KeyGen {

    private static Pairing pairing;

    // 初始化jPBC
    public static void initializePairing() {
        PairingParameters pairingParameters = PairingFactory.getPairingParameters("a.properties");
        pairing = PairingFactory.getPairing(pairingParameters);
    }

    // 将属性保存到文件
    public static void storePropToFile(Properties prop, String fileName) {
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            prop.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();//打印异常的堆栈跟踪信息，帮助调试。
            System.out.println(fileName + " save failed");
            throw new RuntimeException("Failed to save properties to file: " + fileName, e);
        }
    }

    // 从文件加载属性
    public static Properties loadPropFromFile(String fileName) {
        Properties prop = new Properties();
        try (FileInputStream in = new FileInputStream(fileName)) {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(fileName + " load failed");
            throw new RuntimeException("Failed to load properties from file: " + fileName, e);
        }
        return prop;
    }

    // 系统初始化，生成主密钥和公共参数
    public static void setup(String pairingParametersFileName, String pkFileName, String skFileName,String uFileName) {
        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        // 生成主密钥 sk
        Element sk = bp.getZr().newRandomElement().getImmutable();
        Properties skProp = new Properties();                                      // 保存主密钥到文件
        skProp.setProperty("sk", Base64.getEncoder().encodeToString(sk.toBytes())); //将主密钥 sk 转换为字节数组，再编码为 Base64 字符串，并存储在 Properties 对象中，键名为 "sk"。
        storePropToFile(skProp, skFileName);                                      //方法将 Properties 对象保存到指定的主密钥文件 mskFileName。

        //生成参数u
        Element u=bp.getG1().newRandomElement().getImmutable();
        Properties uProp = new Properties();
        uProp.setProperty("u",Base64.getEncoder().encodeToString(u.toBytes()));
        storePropToFile(uProp,uFileName);

        // 生成公共参数 g 和 gx
        Element g = bp.getG1().newRandomElement().getImmutable();
        Element gsk = g.powZn(sk).getImmutable();
        // 保存公共参数到文件
        Properties pkProp = new Properties();
        pkProp.setProperty("g", Base64.getEncoder().encodeToString(g.toBytes()));
        pkProp.setProperty("pk", Base64.getEncoder().encodeToString(gsk.toBytes()));
        storePropToFile(pkProp, pkFileName);
    }

    public static void main(String[] args) {
        // 初始化配对
        initializePairing(); // 先初始化配对

        // 定义文件路径
        String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;
        String pairingParametersFileName = "a.properties";
        String pkFileName = dir + "pk.properties";
        String skFileName = dir + "sk.properties";
        String uFileName=dir + "u.properties";

        long time=System.currentTimeMillis();
        // 进行系统初始化
        setup(pairingParametersFileName, pkFileName, skFileName,uFileName);

        System.out.println(System.currentTimeMillis()-time+"ms");

        // 从私钥文件加载用户私钥sk
        Properties skProp = loadPropFromFile(skFileName);
        String skString = skProp.getProperty("sk");
        System.out.println("私钥sk_TA:"+skString);

        // 从公钥文件加载用户私钥pk
        Properties pkProp = loadPropFromFile(pkFileName);
        String   pkString   = pkProp.getProperty("pk");
        System.out.println("公钥pk_TA："+pkString);

        // 加载u
        Properties uProp = loadPropFromFile(uFileName);
        String uString = uProp.getProperty("u");
        System.out.println("u:"+uString);

        System.out.println("setup success！");
    }
}
