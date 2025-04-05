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

public class TimingProofVerification {

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

    public  static void Verify(String pairingParametersFileName,Element Y,Element g, Element gamma, Element beta, Element SSK_V,String vehicleID,Element B,Element PK_TA){

        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);
        Element C= g.powZn(Y);
        System.out.println("C="+C);
        Element D=(SSK_V.powZn(gamma)).mul(C.powZn(beta));
        System.out.println("D="+D);
        Element H2_ID_V = bp.getG1().newElementFromHash(vehicleID .getBytes(), 0, vehicleID .getBytes().length);
        Element H1_ID_V = bp.getZr().newElementFromHash(vehicleID.getBytes(), 0, vehicleID.getBytes().length).getImmutable();
        Element H2_ID_V_gamma=H2_ID_V.powZn(gamma);
        System.out.println(H2_ID_V_gamma);
        Element pl=bp.pairing(D, g);
        System.out.println("pl= "+pl);
        Element pr1=bp.pairing(H2_ID_V_gamma, PK_TA);
        System.out.println("pr1="+pr1);
        System.out.println("B="+B);
        System.out.println("C="+C);
        Element pr2=bp.pairing(B, C);
        System.out.println("pr2="+pr2);
        Element pr=pr1.mul(pr2);
        System.out.println("pr="+pr);
        System.out.println("时序验证结果="+pl.isEqual(pr));
    }

    public static void main(String[] args) {
        initializePairing();

        // 定义文件路径
        String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;
        String pairingParametersFileName = "a.properties";
        String pkFileName = dir + "pk.properties";
        String YFileName = dir + "Y.properties";
        String BFileName = dir + "B.properties";
        String gammaFileName = dir + "gamma.properties";
        //String betaFileName = dir + "beta.properties";
        String vehicleID = "vehicle123";
        String SSK_VFileName = dir + "SSK_V.properties";
        //long time=System.currentTimeMillis();
        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        // 从公钥文件加载g
        Properties gProp = loadPropFromFile(pkFileName);
        String gString = gProp.getProperty("g");
        String pkString = gProp.getProperty("pk");
        Element g= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();
        Element PK_TA= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(pkString)).getImmutable();

        //加载Y
        Properties YProp=loadPropFromFile(YFileName);
        String YString = YProp.getProperty("Y");
        Element Y= bp.getZr().newElementFromBytes(Base64.getDecoder().decode(YString)).getImmutable();

        //加载gamma
        Properties gammaProp=loadPropFromFile(gammaFileName);
        String gammaString = gammaProp.getProperty("gamma");
        Element gamma= bp.getZr().newElementFromBytes(Base64.getDecoder().decode(gammaString)).getImmutable();

        //加载beta
        Properties betaProp=loadPropFromFile(BFileName);
        String betaString = betaProp.getProperty("beta");
        Element beta= bp.getZr().newElementFromBytes(Base64.getDecoder().decode(betaString)).getImmutable();

        //加载B
        Properties BProp=loadPropFromFile(BFileName);
        String BString = BProp.getProperty("B");
        Element B= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(BString)).getImmutable();

        //加载SSK_V
        Properties SSK_VProp=loadPropFromFile(SSK_VFileName);
        String SSK_VString = SSK_VProp.getProperty("SSK_V");
        Element SSK_V= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(SSK_VString)).getImmutable();

        long time=System.currentTimeMillis();
        Verify(pairingParametersFileName, Y, g, gamma, beta, SSK_V, vehicleID, B, PK_TA);
        System.out.println("Time cost = "+(System.currentTimeMillis()-time)+" ms");
    }
}
