package Vehicle_Digital_Evidence;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.bouncycastle.crypto.agreement.srp.SRP6Client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class SigmaGen {

    private static Pairing pairing;

    // 初始化jPBC
    public static void initializePairing() {
        PairingParameters pairingParameters = PairingFactory.getPairingParameters("a.properties");
        pairing = PairingFactory.getPairing(pairingParameters);
    }

    // 从指定目录中读取加密数据块文件
    public static List<byte[]> readEncryptedBlocksFromDirectory(String directoryPath) throws IOException {
        List<byte[]> blocks = new ArrayList<>();
        File directory = new File(directoryPath);
        // 获取目录下所有以 .dat 结尾的文件
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".dat"));

        if (files == null) {
            throw new IOException("Invalid directory or no .dat files found");
        }

        // 按文件名中的块索引排序
        Arrays.sort(files, Comparator.comparingInt(Vehicle_Digital_Evidence.SigmaGen::extractBlockIndex));

        // 读取每个文件并将其内容作为数据块
        for (File file : files) {
            byte[] block = Files.readAllBytes(file.toPath());
            blocks.add(block);
        }
        return blocks;
    }

    // 从文件名中提取块索引
    private static int extractBlockIndex(File file) {
        String name = file.getName();
        int start = name.lastIndexOf('_') + 1; // 找到最后一个下划线的位置
        int end = name.lastIndexOf('.');       // 找到点的位置
        String indexStr = name.substring(start, end); // 提取索引部分
        return Integer.parseInt(indexStr); // 转换为整数
    }

    // 从 properties 文件中加载 List<byte[]>
    public static List<byte[]> loadByteArraysFromProperties(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty.");
        }

        Properties properties = new Properties();
        List<byte[]> list = new ArrayList<>();

        // 读取文件
        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        }

        // 遍历 properties，按索引顺序加载 byte[]
        int index = 0;
        while (true) {
            String encoded = properties.getProperty(Integer.toString(index));
            if (encoded == null) {
                break; // 如果没有更多数据，退出循环
            }
            // Base64 解码并添加到 List<byte[]>
            byte[] byteArray = Base64.getDecoder().decode(encoded);
            list.add(byteArray);
            index++;
        }

        return list;
    }

    //异或方法
    public static byte[] xorElements(Element d, Element p_i) {
        // 获取 d 和 y_i 的字节数组
        byte[] dBytes = d.toBytes();
        byte[] p_iBytes = p_i.toBytes();

        // 确保两个字节数组的长度相同，按字节长度较大的来初始化
        byte[] resultBytes = new byte[Math.max(dBytes.length, p_iBytes.length)];

        // 按字节异或操作
        for (int i = 0; i < resultBytes.length; i++) {
            byte byteD = (i < dBytes.length) ? dBytes[i] : 0;  // 如果 dBytes 长度不足，用 0 填充
            byte byteP = (i < p_iBytes.length) ? p_iBytes[i] : 0;  // 如果 y_iBytes 长度不足，用 0 填充

            // 按位异或操作
            resultBytes[i] = (byte) (byteD ^ byteP);
        }

        // 返回异或结果的字节数组
        return resultBytes;
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

    //计算sigmas
    public static List<Element> generateVerificationTags(List<byte[]> encryptedBlocks, String idRSU, Element skRSU, Element u, List<Element> y_i) {
        List<Element> sigmas = new ArrayList<>();

        for (int i = 0; i < encryptedBlocks.size(); i++) {
            byte[] c_i = encryptedBlocks.get(i);

            // 1. 计算 H_2(ID_{RSU} || i)
            Element H2_value = hashToG1(idRSU + i);

            // 3. 计算 μ^{H(c_i)}
            Element mu_H_ci = u.powZn(y_i.get(i));

            // 4. 计算最终 σ_i
            Element sigma_i = H2_value.mul(mu_H_ci).powZn(skRSU);

            // 5. 存储 σ_i
            sigmas.add(sigma_i);
        }

        return sigmas;
    }

    //保存sigmas
    public static void saveSigmasToProperties(List<Element> sigmas, String filePath) {
        Properties properties = new Properties();

        // 存储总数，确保读取时按顺序解析
        properties.setProperty("total", String.valueOf(sigmas.size()));

        for (int i = 0; i < sigmas.size(); i++) {
            // 将 Element 转换为 Base64 编码字符串
            String encodedSigma = Base64.getEncoder().encodeToString(sigmas.get(i).toBytes());
            properties.setProperty("Sigma_" + i, encodedSigma);
        }

        // 存入 Properties 文件
        try (FileOutputStream output = new FileOutputStream(filePath)) {
            properties.store(output, "Sigma Verification Tags");
            System.out.println("Sigmas saved successfully to " + filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Sigmas to properties file: " + filePath, e);
        }
    }

    /**
     * 计算 H_2(x) 的哈希值，并映射到 Zr 群
     */
    private static Element hashToG1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return pairing.getG1().newElementFromHash(hashBytes, 0, hashBytes.length).getImmutable();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    //保存y_i
    public static void saveYListToProperties(List<Element> Y_list, String filePath) {
        Properties properties = new Properties();

        // 存储总数，确保读取时按顺序解析
        properties.setProperty("total", String.valueOf(Y_list.size()));

        for (int i = 0; i < Y_list.size(); i++) {
            // 将 Element 转换为 Base64 编码字符串
            String encodedY = Base64.getEncoder().encodeToString(Y_list.get(i).toBytes());
            properties.setProperty("Y_" + i, encodedY);
        }

        // 存入 Properties 文件
        try (FileOutputStream output = new FileOutputStream(filePath)) {
            properties.store(output, "Y_list Elements Storage");
            System.out.println("Y_list saved successfully to " + filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Y_list to properties file: " + filePath, e);
        }
    }

    public static void main(String[] args) throws IOException {

        initializePairing();

        Pairing bp=PairingFactory.getPairing("a.properties");//生成双线性配对要用椭圆曲线参数

        String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;

        String p_iFileName= dir +"p_i.properties";
        String BFileName= dir +"B.properties";
        String rFileName= dir +"r.properties";
        String pkFileName=dir +"pk.properties";
        String idRSU = "RSU456";
        String SK_RSUFileName =dir + "SK_RSU.properties";
        String uFileName=dir + "u.properties";
        String RFileName=dir + "RSU_R.properties";
        String SigmasFileName = dir +"Sigmas.properties";
        String y_iFileName= dir +"y_i.properties";

        List<byte[]> p=loadByteArraysFromProperties(p_iFileName);


        // 从文件加载B
        Properties BProp = loadPropFromFile(BFileName);
        String BString = BProp.getProperty("B");
        Element B=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(BString)).getImmutable();

        // 从文件加载r
        Properties rProp = loadPropFromFile(rFileName);
        String rString = rProp.getProperty("r");
        Element r=bp.getZr().newElementFromBytes(Base64.getDecoder().decode(rString)).getImmutable();

        // 从文件加载R
        Properties RProp = loadPropFromFile(RFileName);
        String RString = RProp.getProperty("R");
        Element R=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(RString)).getImmutable();

        // 从文件加载g
        Properties gProp = loadPropFromFile(pkFileName);
        String gString = gProp.getProperty("g");
        String pkString = gProp.getProperty("pk");
        Element g=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();
        Element pk=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(pkString)).getImmutable();

        // 从文件加载SK_RSU
        Properties SK_RSUProp = loadPropFromFile(SK_RSUFileName);
        String SK_RSUString = SK_RSUProp.getProperty("SK_RSU");
        Element SK_RSU=bp.getZr().newElementFromBytes(Base64.getDecoder().decode(SK_RSUString)).getImmutable();

        // 从文件加载u
        Properties uProp = loadPropFromFile(uFileName);
        String uString = uProp.getProperty("u");
        Element u=bp.getG1().newElementFromBytes(Base64.getDecoder().decode(uString)).getImmutable();

        Element B_r= B.powZn(r);
        byte[] B_r_bytes=B_r.toBytes();
        Element d=bp.getZr().newElementFromHash(B_r_bytes, 0, B_r_bytes.length).getImmutable(); // 计算哈希


        // 初始化存储 Y_i 的列表
        List<Element> Y_list = new ArrayList<>();

        //恢复重复数据删除标签y_i
        for (int i = 0; i < p.size(); i++) {
            Element p_i=bp.getZr().newElementFromBytes(p.get(i));
            byte[] y_i=xorElements(d,p_i);
            Element Y_i=bp.getZr().newElementFromBytes(y_i);
            Y_list.add(Y_i); // 存入列表
           // System.out.println("y_" + i + ": " + Y_i);
        }

        saveYListToProperties(Y_list,y_iFileName);

        // 读取加密块（假设文件路径为 "encrypted_file.dat"）
        String filePath = "C:\\Users\\22867\\Desktop\\encBlocks";
        List<byte[]> encryptedBlocks = readEncryptedBlocksFromDirectory(filePath);
        long time=System.currentTimeMillis();
        List<Element> Sigmas = generateVerificationTags(encryptedBlocks,idRSU,SK_RSU, u, Y_list);
        System.out.println(System.currentTimeMillis()-time+"ms");
        System.out.println(Sigmas);
        saveSigmasToProperties(Sigmas,SigmasFileName);

    }

}
