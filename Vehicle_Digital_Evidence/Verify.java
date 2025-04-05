package Vehicle_Digital_Evidence;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

public class Verify {

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

    public static void main(String[] args) {
        // 定义文件路径
        String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;
        String pairingParametersFileName = "a.properties";
        String deltaFileName= dir+"delta.properties";
        String pkFileName= dir+"pk.properties";
        String idRSU = "RSU456";
        String uFileName=dir + "u.properties";
        String RFileName=dir + "RSU_R.properties";
        String thetaFileName=dir + "theta.properties";
        String varphiFileName=dir + "varphi.properties";
        // 从参数文件加载配对
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        // 从文件加载delta
        Properties deltaProp = loadPropFromFile(deltaFileName);
        String deltaString = deltaProp.getProperty("delta");
        Element delta= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(deltaString)).getImmutable();

        // 从公钥文件加载g,pk_TA
        Properties gProp = loadPropFromFile(pkFileName);
        String  gString = gProp.getProperty("g");
        Element g= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();
        String pkString = gProp.getProperty("pk");
        Element pk_TA=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(pkString)).getImmutable();

        // 从文件加载theta
        Properties thetaProp = loadPropFromFile(thetaFileName);
        String thetaString = thetaProp.getProperty("theta");
        Element theta= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(thetaString)).getImmutable();

        // 从文件加载u
        Properties uProp = loadPropFromFile(uFileName);
        String uString = uProp.getProperty("u");
        Element u= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(uString)).getImmutable();

        // 从文件加载varphi
        Properties varphiProp = loadPropFromFile(varphiFileName);
        String varphiString = varphiProp.getProperty("varphi");
        Element varphi= bp.getZr().newElementFromBytes(Base64.getDecoder().decode(varphiString)).getImmutable();

        // 从文件加载R
        Properties RProp = loadPropFromFile(RFileName);
        String RString = RProp.getProperty("R");
        Element R=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(RString)).getImmutable();
        long time=System.nanoTime();

        Element H1_ID_RSU = bp.getZr().newElementFromHash(idRSU.getBytes(), 0, idRSU.getBytes().length).getImmutable();
        System.out.println(System.nanoTime()-time+"ms");

//       long time=System.currentTimeMillis();
        Element pl=bp.pairing(delta, g);
        System.out.println(pl);
        Element pr1=theta.mul(u.powZn(varphi));
        Element mid=R.mul(pk_TA);
       // long time=System.currentTimeMillis();
        Element pr2=(mid).powZn(H1_ID_RSU);
        //System.out.println(System.currentTimeMillis()-time+"ms");
        Element pr=bp.pairing(pr1,pr2);
        System.out.println(pr);
        //long time=System.currentTimeMillis();
        System.out.println("双线性配对验证结果："+pr.isEqual(pl));
        //System.out.println(System.currentTimeMillis()-time1+"ms");
    }
}
