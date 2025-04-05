package Vehicle_Digital_Evidence;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Chal{

    private static Pairing pairing;

    /**
     * 生成 c 个不重复的索引，确保范围在 [0, n-1] 之间
     */
    public static List<Integer> generateUniqueIndexes(String seed, int c, int n) {
        List<Integer> indexList = new ArrayList<>();
        Set<Integer> indexSet = new HashSet<>();
        Random random = new Random(seed.hashCode());

        while (indexSet.size() < c) {
            int i = random.nextInt(n); // 生成 [0, n-1] 范围内的索引
            if (indexSet.add(i)) { // 确保索引不重复
                indexList.add(i);
            }
        }
        return indexList;
    }

    /**
     * 计算挑战值 l_i (映射到 Zr 群)
     */
    public static Element calculateChallengeValue(Pairing pairing, String seed, int i) {
        String input = seed + i;
        byte[] hash = input.getBytes();
        return pairing.getZr().newElementFromBytes(hash).getImmutable();
    }

    /**
     * 保存索引 i 到 Properties 文件
     */
    public static void saveIndexesToProperties(String filename, List<Integer> indexes) {
        Properties properties = new Properties();
        for (int k = 0; k < indexes.size(); k++) {
            properties.setProperty("index_" + k, String.valueOf(indexes.get(k)));
        }

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            properties.store(fos, "Challenge Indexes");
            System.out.println("Indexes saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error saving indexes to properties file: " + e.getMessage());
        }
    }

//    /**
//     * 保存挑战值 l_i 到 Properties 文件
//     */
//    public static void saveChallengesToProperties(String filename, Map<Integer, Element> challengeMap) {
//        Properties properties = new Properties();
//        for (Map.Entry<Integer, Element> entry : challengeMap.entrySet()) {
//            properties.setProperty("l_i_" + entry.getKey(), entry.getValue().toString());
//        }
//
//        try (FileOutputStream fos = new FileOutputStream(filename)) {
//            properties.store(fos, "Challenge Values");
//            System.out.println("Challenges saved to " + filename);
//        } catch (IOException e) {
//            System.err.println("Error saving challenges to properties file: " + e.getMessage());
//        }
//    }

    /**
     * 保存挑战值 l_i 到 Properties 文件
     */
    public static void saveChallengesToProperties(String filename, Map<Integer, Element> challengeMap) {
        Properties properties = new Properties();
        for (Map.Entry<Integer, Element> entry : challengeMap.entrySet()) {
            // 使用 Base64 编码 Element 的字节表示
            String base64Value = Base64.getEncoder().encodeToString(entry.getValue().toBytes());
            properties.setProperty("l_i_" + entry.getKey(), base64Value);
        }

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            properties.store(fos, "Challenge Values");
            System.out.println("Challenges saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error saving challenges to properties file: " + e.getMessage());
        }
    }


    //计算文件夹中.dat个数
    public static int countDatFilesInDirectory(String directoryPath) {
        // 创建一个File对象表示指定目录
        File directory = new File(directoryPath);

        // 获取目录下所有的文件和文件夹
        File[] files = directory.listFiles();

        // 检查目录是否有效，以及是否包含文件
        if (files == null) {
            throw new IllegalArgumentException("Invalid directory path or directory is empty.");
        }

        int count = 0;

        // 遍历所有文件并检查是否以 ".dat" 结尾
        for (File file : files) {
            // 如果是文件并且扩展名是 ".dat"
            if (file.isFile() && file.getName().endsWith(".dat")) {
                count++;
            }
        }

        // 返回符合条件的文件数量
        return count;
    }

    /**
     * 计算 H_2(x) 的哈希值，并映射到 Zr 群
     */
    private static Element hashToG1(String input) {

        // 初始化双线性配对 (默认使用Type A配对)
        Pairing bp=PairingFactory.getPairing("a.properties");//生成双线性配对要用椭圆曲线参数

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bp.getG1().newElementFromHash(hashBytes, 0, hashBytes.length).getImmutable();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    /**
     * 计算 θ = ∏(H2(ID_RSU || i) ^ l_i)  (模 Zr 群运算)
     */
    public static Element computeTheta(Pairing pairing, String idRSU, List<Integer> indexes, Map<Integer, Element> challengeMap) {
        Element theta = pairing.getG1().newOneElement().getImmutable(); // 乘法单位元 1 (在G1群中)

        for (int i : indexes) {
           // System.out.println(i);
            Element h2 = hashToG1( idRSU+i); // H2(ID_RSU || i)
            Element li = challengeMap.get(i); // l_i
          //  System.out.println(li);

            // 计算 H2(ID_RSU || i) ^ l_i 并进行累乘
            Element result = h2.powZn(li).getImmutable();
            theta = theta.mul(result).getImmutable();
        }
        return theta;
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

    public static void main(String[] args) {
        // 初始化双线性配对 (默认使用Type A配对)
        Pairing bp=PairingFactory.getPairing("a.properties");//生成双线性配对要用椭圆曲线参数

        String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;
        String iFileName = dir + "i.properties";
        String l_iFileName = dir + "l_i.properties";
        String thetaFileName = dir + "theta.properties";

        String idRSU = "RSU456";

        // 指定输出文件夹路径
        String encryptedBlocksDir = "C:\\Users\\22867\\Desktop\\encBlocks";

        Random rand = new Random();
        String seed = String.valueOf(rand.nextInt(1_000_000)); // 生成随机种子
        int n =countDatFilesInDirectory(encryptedBlocksDir) ; // 数据总块数
        System.out.println(n);
        int c = 100;  // 挑战块数

        long time=System.nanoTime();
        // 生成唯一索引
        List<Integer> indexes = generateUniqueIndexes(seed, c, n);

        System.out.println(System.nanoTime()-time+" ns");
        // 计算挑战值 l_i 并映射到 Zr
        Map<Integer, Element> challengeMap = new LinkedHashMap<>();
        for (int i : indexes) {
            challengeMap.put(i, calculateChallengeValue(bp, seed, i));
        }

        // 输出计算结果
        System.out.println("Seed: " + seed);
        System.out.println("Total Blocks (n): " + n);
        System.out.println("Challenge Blocks (c): " + c);
        System.out.println("\nGenerated Indexes and Challenge Values in Zr:");

        for (Map.Entry<Integer, Element> entry : challengeMap.entrySet()) {
            System.out.println("Index (i): " + entry.getKey() + ", Challenge Value (l_i): " + entry.getValue());
        }

        // 保存索引 i 和挑战值 l_i 到不同的 properties 文件
        saveIndexesToProperties(iFileName, indexes);
        saveChallengesToProperties(l_iFileName, challengeMap);

        // 计算 θ
        Element theta = computeTheta(bp, idRSU, indexes, challengeMap);
        Properties thetaProp = new Properties();                                      // 保存主密钥到文件
        thetaProp.setProperty("theta", Base64.getEncoder().encodeToString(theta.toBytes())); //将主密钥 sk 转换为字节数组，再编码为 Base64 字符串，并存储在 Properties 对象中，键名为 "sk"。
        storePropToFile(thetaProp, thetaFileName);                                      //方法将 Properties 对象保存到指定的主密钥文件 mskFileName。
    }
}

