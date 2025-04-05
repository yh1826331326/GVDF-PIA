package Vehicle_Digital_Evidence;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import javax.xml.stream.events.EntityReference;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

public class VehicleRegistration {
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

    public static void registerVehicle(String pairingParametersFileName,String vehicleID,String skFileName,String pkFileName,String AFileName,String SK_VFileName,String SSK_VFileName) {

        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        // Step 1: TA选择一个随机的α ∈ Z_q^*
        Element alpha = bp.getZr().newRandomElement().getImmutable();

        // 从公钥文件加载g
        Properties gProp = loadPropFromFile(pkFileName);
        String  gString = gProp.getProperty("g");
        Element g= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();

        // 计算 A = g^α并保存A
        Element A=g.powZn(alpha);
        Properties AProp = new Properties();
        AProp.setProperty("A", Base64.getEncoder().encodeToString(A.toBytes()));
        storePropToFile(AProp, AFileName);

        //从私钥文件加载sk
        Properties skProp = loadPropFromFile(skFileName);
        String  skString = skProp.getProperty("sk");
        Element sk_TA= bp.getZr().newElementFromBytes(Base64.getDecoder().decode(skString)).getImmutable();

        // 计算 SK_V = (α + SK_TA) * H_1(ID_V)
        Element H1_ID_V = bp.getZr().newElementFromHash(vehicleID.getBytes(), 0, vehicleID.getBytes().length).getImmutable();
        Element SK_V = (alpha.add(sk_TA)).mul(H1_ID_V);
        Properties SK_VProp = new Properties();
        SK_VProp.setProperty("SK_V", Base64.getEncoder().encodeToString(SK_V.toBytes()));
        storePropToFile(SK_VProp, SK_VFileName);

        Element H1_ID_v = bp.getG1().newElementFromHash(vehicleID.getBytes(), 0, vehicleID.getBytes().length).getImmutable();
        // 计算身份签名 SSK_V = H_1(ID_V)^SK_TA
        Element SSK_V = H1_ID_v.powZn(sk_TA);
        Properties SSK_VProp = new Properties();
        SSK_VProp.setProperty("SSK_V", Base64.getEncoder().encodeToString(SSK_V.toBytes()));
        storePropToFile(SSK_VProp, SSK_VFileName);
    }

    public static void verify(String pairingParametersFileName,String vehicleID, String pkFileName,String AFileName,String SK_VFileName){
        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        // 从公钥文件加载g
        Properties gProp = loadPropFromFile(pkFileName);
        String gString = gProp.getProperty("g");
        String pkString = gProp.getProperty("pk");
        Element g= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();
        Element pk_TA= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(pkString)).getImmutable();

        //从文件加载SK_V
        Properties SK_VProp = loadPropFromFile(SK_VFileName);
        String SK_VString=SK_VProp.getProperty("SK_V");
        Element SK_V=bp.getZr().newElementFromBytes(Base64.getDecoder().decode(SK_VString)).getImmutable();

        //从文件加载A
        Properties AProp = loadPropFromFile(AFileName);
        String AString=AProp.getProperty("A");
        Element A=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(AString)).getImmutable();

        Element H1_ID_V = bp.getZr().newElementFromHash(vehicleID.getBytes(), 0, vehicleID.getBytes().length).getImmutable();
        System.out.println("g^{SK_V}= "+g.powZn(SK_V));
        System.out.println("(A*PK_TA)^{H1_ID_V}= "+A.mul(pk_TA).powZn(H1_ID_V));
        System.out.println("g^{SK_V}=(A*PK_TA)^{H1_ID_V}的结果为："+g.powZn(SK_V).isEqual((A.mul(pk_TA)).powZn(H1_ID_V)));
        System.out.println("车辆注册成功！");
    }

    public static void main(String[] args) {
        // 初始化配对
        initializePairing(); // 先初始化配对

        // 定义文件路径
        String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;
        String pairingParametersFileName = "a.properties";
        String skFileName = dir + "sk.properties";
        String pkFileName = dir + "pk.properties";
        String AFileName = dir + "A.properties";
        String SSK_VFileName = dir + "SSK_V.properties";
        String SK_VFileName = dir + "SK_V.properties";
        String vehicleID = "vehicle123"; // 车辆ID
        long time=System.currentTimeMillis();
        registerVehicle(pairingParametersFileName, vehicleID, skFileName, pkFileName, AFileName, SK_VFileName, SSK_VFileName);
        System.out.println(System.currentTimeMillis()-time+"ms");
        verify(pairingParametersFileName, vehicleID, pkFileName, AFileName, SK_VFileName);
        System.out.println(System.currentTimeMillis()-time+"ms");
    }
}
