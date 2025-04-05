package Vehicle_Digital_Evidence;

import java.io.*;
import java.util.*;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.jpbc.PairingParameters;

public class ProofGen {

    private static Pairing pairing;

    // 初始化jPBC
    public static void initializePairing() {
        PairingParameters pairingParameters = PairingFactory.getPairingParameters("a.properties");
        pairing = PairingFactory.getPairing(pairingParameters);
    }


    public static Map<Integer, Element> readChallengesFromProperties(String filename, Pairing pairing) {
        Properties properties = new Properties();
        Map<Integer, Element> challengeMap = new LinkedHashMap<>(); // 保持原始顺序

        try (FileInputStream fis = new FileInputStream(filename)) {
            properties.load(fis);

            // 用 TreeMap 暂存，按 key 自然排序
            TreeMap<Integer, Element> sortedMap = new TreeMap<>();

            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("l_i_")) {
                    try {
                        int index = Integer.parseInt(key.substring(4)); // 提取索引
                        byte[] bytes = Base64.getDecoder().decode(properties.getProperty(key)); // Base64 解码
                        Element li = pairing.getZr().newElementFromBytes(bytes).getImmutable(); // 恢复 Zr 元素
                        sortedMap.put(index, li);
                    } catch (Exception e) {
                        System.err.println("Error decoding challenge value: " + key);
                        e.printStackTrace();
                    }
                }
            }

            // 保持存储顺序
            challengeMap.putAll(sortedMap);

        } catch (IOException e) {
            System.err.println("Error reading properties file: " + e.getMessage());
        }

        return challengeMap;
    }


    public static List<Element> readSigmasFromProperties(String filePath, Pairing pairing) {
        Properties properties = new Properties();
        List<Element> sigmas = new ArrayList<>();

        // 读取 Properties 文件
        try (FileInputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sigmas from properties file: " + filePath, e);
        }

        // 读取总数
        int total = Integer.parseInt(properties.getProperty("total", "0"));

        // 按顺序读取 Sigma 值
        for (int i = 0; i < total; i++) {
            String key = "Sigma_" + i;
            if (properties.containsKey(key)) {
                try {
                    byte[] sigmaBytes = Base64.getDecoder().decode(properties.getProperty(key));
                    Element sigma = pairing.getG1().newElementFromBytes(sigmaBytes).getImmutable();
                    sigmas.add(sigma);
                } catch (Exception e) {
                    System.err.println("Error decoding Sigma: " + key);
                    e.printStackTrace();
                }
            }
        }

        return sigmas;
    }

    //读取y_i
    public static List<Element> readYListFromProperties(String filePath, Pairing pairing) {
        Properties properties = new Properties();
        List<Element> Y_list = new ArrayList<>();

        // 读取 Properties 文件
        try (FileInputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Y_list from properties file: " + filePath, e);
        }

        // 读取总数
        int total = Integer.parseInt(properties.getProperty("total", "0"));

        // 按顺序读取 Y_i 值
        for (int i = 0; i < total; i++) {
            String key = "Y_" + i;
            if (properties.containsKey(key)) {
                try {
                    byte[] YBytes = Base64.getDecoder().decode(properties.getProperty(key));
                    Element Y_i = pairing.getZr().newElementFromBytes(YBytes).getImmutable();
                    Y_list.add(Y_i);
                } catch (Exception e) {
                    System.err.println("Error decoding Y_i: " + key);
                    e.printStackTrace();
                }
            }
        }

        return Y_list;
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

    public static void main(String[] args) throws IOException {

        // 初始化配对
        initializePairing(); // 先初始化配对

        // 初始化双线性配对 (默认使用Type A配对)
        Pairing bp=PairingFactory.getPairing("a.properties");//生成双线性配对要用椭圆曲线参数

        // 定义文件路径
        String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;
        String iFileName = dir + "i.properties";
        String l_iFileName = dir + "l_i.properties";
        String sigmasFileName = dir +"Sigmas.properties";
        String y_iFileName= dir +"y_i.properties";
        String deltaFileName=dir+"delta.properties";
        String varphiFileName=dir+"varphi.properties";


        Map<Integer, Element> challengeMap = readChallengesFromProperties(l_iFileName, bp);

        for (Map.Entry<Integer, Element> entry : challengeMap.entrySet()) {
            System.out.println("Index (i): " + entry.getKey() + ", Challenge Value (l_i): " + entry.getValue());
        }
        long time=System.currentTimeMillis();
        // 读取 `sigmas`
        List<Element> sigmas = readSigmasFromProperties(sigmasFileName, bp);
        System.out.println(sigmas);

        Element delta = bp.getG1().newOneElement().getImmutable(); // 乘法单位元 1 (在G1群中)

        for (Map.Entry<Integer, Element> entry : challengeMap.entrySet()) {
            int index=entry.getKey();
           // System.out.println(index);
            Element l_i=entry.getValue();
           // System.out.println(l_i);
            Element sigma=sigmas.get(index);
            System.out.println(sigma);
          //  System.out.println(sigma);
            Element sigma_l_i=sigma.powZn(l_i);
            delta=delta.duplicate().mul(sigma_l_i);
        }
        Properties deltaProp = new Properties();
        deltaProp.setProperty("delta",Base64.getEncoder().encodeToString(delta.toBytes()));
        storePropToFile(deltaProp,deltaFileName);
        System.out.println("delta:"+delta);

        List<Element> y_i=readYListFromProperties(y_iFileName,bp);
        System.out.println(y_i);
        Element varphi=bp.getZr().newZeroElement();
        for (Map.Entry<Integer, Element> entry : challengeMap.entrySet()) {
            int index=entry.getKey();
            // System.out.println(index);
            Element l_i=entry.getValue();
            // System.out.println(l_i);
            Element mid=l_i.mul(y_i.get(index));
            varphi=varphi.duplicate().add(mid);
        }
        System.out.println(System.currentTimeMillis()-time+"ms");
        Properties varphiProp = new Properties();
        varphiProp.setProperty("varphi",Base64.getEncoder().encodeToString(varphi.toBytes()));
        storePropToFile(varphiProp,varphiFileName);
        System.out.println("varphi:"+varphi);

    }
}
