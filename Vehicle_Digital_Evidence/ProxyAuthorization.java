package Vehicle_Digital_Evidence;

import com.sun.jna.WString;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.field.base.AbstractElement;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Base64;
import java.util.Properties;

public class ProxyAuthorization {
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
           // System.out.println("File saved: " + fileName);  // 调试语句
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

    // 生成授权信息并返回
    public static void generateAuthorization(String pairingParametersFileName, String vehicleID, String RSUID, String M, Element g,Element SK_V, String Auth_VFileName, String XFileName, String xFileName) {

        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        // Step 1: 车辆组成员V选择随机值x ∈ Z_q^*
        Element x = bp.getZr().newRandomElement();
        Properties xProp = new Properties();
        xProp.setProperty("x", Base64.getEncoder().encodeToString(x.toBytes()));
        storePropToFile(xProp, xFileName);

        // 计算 X = g^x
        Element X = g.powZn(x);
        Properties XProp = new Properties();
        XProp.setProperty("X", Base64.getEncoder().encodeToString(X.toBytes()));
        storePropToFile(XProp, XFileName);

        // Step 2: 计算授权信息Auth_V = x + SK_V * H_1(ID_V || ID_RSU || X || M)
        Element H1 = bp.getZr().newElementFromHash((vehicleID + RSUID + X.toString() + M).getBytes(), 0, (vehicleID + RSUID + X.toString() + M).getBytes().length);
        Element Auth_V = x.add(SK_V.mul(H1));

        // 保存授权信息
        Properties Auth_VProp = new Properties();
        Auth_VProp.setProperty("Auth_V", Base64.getEncoder().encodeToString(Auth_V.toBytes()));
        storePropToFile(Auth_VProp, Auth_VFileName);


    }

    public static void verifyAuthorization(String pairingParametersFileName, String vehicleID, String RSUID, String M, Element g, Element A, Element PK_TA,String Auth_VFileName, String XFileName) {

        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        //从文件加载Auth_V
        Properties Auth_VProp = loadPropFromFile(Auth_VFileName);
        String Auth_VString=Auth_VProp.getProperty("Auth_V");
        Element Auth_V=bp.getZr().newElementFromBytes(Base64.getDecoder().decode(Auth_VString)).getImmutable();

        //从文件加载X
        Properties XProp = loadPropFromFile(XFileName);
        String XString=XProp.getProperty("X");
        Element X=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(XString)).getImmutable();

        Element pl=bp.pairing(g.powZn(Auth_V), g);
        System.out.println(pl);

        Element pr1=bp.pairing(X, g);

        // Step 1: 计算 H_1(ID_V || ID_RSU || X || M)
        Element H1 = bp.getZr().newElementFromHash((vehicleID + RSUID + X.toString() + M).getBytes(), 0, (vehicleID + RSUID + X.toString() + M).getBytes().length);
        Element H1_ID_V = bp.getZr().newElementFromHash(vehicleID.getBytes(), 0, vehicleID.getBytes().length).getImmutable();

        Element pr2=bp.pairing(g.powZn(H1), (A.mul(PK_TA)).powZn(H1_ID_V));
        Element pr=pr1.mul(pr2);
        System.out.println(pr);
        System.out.println(pl.isEqual(pr));

    }

    public static void SK_RSUGen(String pairingParametersFileName,Element g,Element SK_TA,Element PK_TA,String RSUID,String rFileName,String RSU_RFileName, String SK_RSUFileName ){
        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        Element r=bp.getZr().newRandomElement();
        Element R=g.powZn(r);

        // 保存r
        Properties rProp = new Properties();
        rProp.setProperty("r", Base64.getEncoder().encodeToString(r.toBytes()));
        storePropToFile(rProp, rFileName);

        // 保存R
        Properties RSU_RProp = new Properties();
        RSU_RProp.setProperty("R", Base64.getEncoder().encodeToString(R.toBytes()));
        storePropToFile(RSU_RProp, RSU_RFileName);

        Element H1_ID_RSU = bp.getZr().newElementFromHash(RSUID.getBytes(), 0, RSUID.getBytes().length).getImmutable();
        Element SK_RSU=(r.add(SK_TA)).mul(H1_ID_RSU);

        //保存SK_RSu
        Properties SK_RSUProp = new Properties();
        SK_RSUProp.setProperty("SK_RSU", Base64.getEncoder().encodeToString(SK_RSU.toBytes()));
        storePropToFile(SK_RSUProp, SK_RSUFileName);


        System.out.print("SK_RSU有效性");
        System.out.println((g.powZn(SK_RSU)).isEqual((R.mul(PK_TA)).powZn(H1_ID_RSU)));

    }

    public static void main(String[] args) {
        initializePairing();
        // 定义文件路径
        String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;
        String pairingParametersFileName = "a.properties";

        String SK_VFileName = dir + "SK_V.properties";
        String AFileName = dir + "A.properties";
        String skFileName = dir +"sk.properties";
        String pkFileName = dir + "pk.properties";
        String Auth_VFileName = dir + "Auth_V.properties";
        String rFileName= dir +"r.properties";
        String RSU_RFileName= dir +"RSU_R.properties";
        String SK_RSUFileName= dir + "SK_RSU.properties";
        String xFileName = dir + "x.properties";
        String XFileName = dir + "V_X.properties";

        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        // 假设有一个车辆身份ID和RSU的ID
        String vehicleID = "vehicle123";
        String RSUID = "RSU456";
        String M = "delegation data"; // 委托信息

        //从文件加载SK_V
        Properties SK_VProp = loadPropFromFile(SK_VFileName);
        String SK_VString=SK_VProp.getProperty("SK_V");
        Element SK_V=bp.getZr().newElementFromBytes(Base64.getDecoder().decode(SK_VString)).getImmutable();

        //从文件加载sk
        Properties skProp = loadPropFromFile(skFileName);
        String skString=skProp.getProperty("sk");
        Element SK_TA=bp.getZr().newElementFromBytes(Base64.getDecoder().decode(skString)).getImmutable();

        //从文件加载A
        Properties AProp = loadPropFromFile(AFileName);
        String AString=AProp.getProperty("A");
        Element A=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(AString)).getImmutable();

        // 从公钥文件加载g
        Properties gProp = loadPropFromFile(pkFileName);
        String gString = gProp.getProperty("g");
        String pkString = gProp.getProperty("pk");
        Element g= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();
        Element PK_TA= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(pkString)).getImmutable();

        long time=System.currentTimeMillis();
        //生成授权信息
        generateAuthorization(pairingParametersFileName, vehicleID, RSUID,M,g, SK_V,Auth_VFileName,XFileName,xFileName);
        //验证授权信息
        verifyAuthorization(pairingParametersFileName,vehicleID,RSUID,M,g,A,PK_TA, Auth_VFileName, XFileName);
        SK_RSUGen(pairingParametersFileName,g,SK_TA, PK_TA, RSUID, rFileName, RSU_RFileName, SK_RSUFileName);
        System.out.println(System.currentTimeMillis()-time+"ms");
    }
}
