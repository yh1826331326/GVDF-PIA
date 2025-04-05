package Vehicle_Digital_Evidence;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.jpbc.Field;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DataEncrypted {

    private static Pairing pairing;

    // 初始化jPBC
    public static void initializePairing() {
        PairingParameters pairingParameters = PairingFactory.getPairingParameters("a.properties");
        pairing = PairingFactory.getPairing(pairingParameters);
    }

    // 将文件分块
    public static List<byte[]> splitFile(File file, int blockSize) throws IOException {
        List<byte[]> blocks = new ArrayList<>(); // 存储每个文件块的列表
        try (FileInputStream fis = new FileInputStream(file)) { // 创建文件输入流读取文件
            byte[] buffer = new byte[blockSize]; // 每次读取的字节数
            int bytesRead; // 实际读取的字节数
            while ((bytesRead = fis.read(buffer)) != -1) { // 当文件未读取完时继续读取
                byte[] block = new byte[bytesRead]; // 创建实际大小的块
                System.arraycopy(buffer, 0, block, 0, bytesRead); // 将读取的数据复制到块中
                blocks.add(block); // 将块添加到列表中
            }
        }
        return blocks; // 返回文件块列表
    }

    // 生成从数据块派生的加密密钥
    public static SecretKey deriveKeyFromData(byte[] dataBlock) throws NoSuchAlgorithmException {
        // 获取SHA-256消息摘要实例
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        // 对数据块进行哈希处理，生成256位的摘要，并取前128位作为AES密钥
        byte[] keyBytes = sha256.digest(dataBlock);
        // 返回AES密钥规范
        return new SecretKeySpec(keyBytes, 0, 16, "AES");
    }

    // 对数据块进行加密（使用AES对称加密）
    public static byte[] encryptBlock(byte[] block, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES"); // 获取AES加密实例
        cipher.init(Cipher.ENCRYPT_MODE, key); // 初始化加密模式和密钥
        return cipher.doFinal(block); // 加密数据块并返回加密后的字节数组
    }

    // 保存块到文件
    public static void saveBlockToFile(byte[] block, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) { // 创建文件输出流
            fos.write(block); // 将块写入文件
        }
    }

    //将密钥保存到文件
    public static void saveKeyToFile(SecretKey key, String directory, int blockIndex) throws IOException {
        // 将密钥转换为Base64编码的字符串
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());

        // 构造文件路径，文件名为 block_{index}_key.dat
        String keyFilePath = directory + File.separator + "block_key_" + blockIndex + ".dat";

        // 将密钥保存到文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(keyFilePath))) {
            writer.write(encodedKey);
        }
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

    // 方法：对两个 Element 对象进行按位异或操作，返回 byte[] 类型的结果
    public static byte[] xorElements(Element d, Element y_i) {
        // 获取 d 和 y_i 的字节数组
        byte[] dBytes = d.toBytes();
        byte[] y_iBytes = y_i.toBytes();

        // 确保两个字节数组的长度相同，按字节长度较大的来初始化
        byte[] resultBytes = new byte[Math.max(dBytes.length, y_iBytes.length)];

        // 按字节异或操作
        for (int i = 0; i < resultBytes.length; i++) {
            byte byteD = (i < dBytes.length) ? dBytes[i] : 0;  // 如果 dBytes 长度不足，用 0 填充
            byte byteY = (i < y_iBytes.length) ? y_iBytes[i] : 0;  // 如果 y_iBytes 长度不足，用 0 填充

            // 按位异或操作
            resultBytes[i] = (byte) (byteD ^ byteY);
        }

        // 返回异或结果的字节数组
        return resultBytes;
    }



    // 计算哈希值 H_1(c_i || t_i || h_{i-1})
    public static byte[] calculateHash(byte[] encryptedBlock, long time, byte[] h_prev) {
        // 如果 h_prev 为 null, 我们不将其包含在计算中
        byte[] concatenated = null;

        // 拼接加密块和时间戳
        byte[] timeBytes = longToBytes(time);
        if (h_prev != null) {
            concatenated = new byte[encryptedBlock.length + 8 + h_prev.length];
            System.arraycopy(h_prev, 0, concatenated, 0, h_prev.length);
            System.arraycopy(encryptedBlock, 0, concatenated, h_prev.length, encryptedBlock.length);
            System.arraycopy(timeBytes, 0, concatenated, h_prev.length + encryptedBlock.length, timeBytes.length);
        } else {
            concatenated = new byte[encryptedBlock.length + 8];
            System.arraycopy(encryptedBlock, 0, concatenated, 0, encryptedBlock.length);
            System.arraycopy(timeBytes, 0, concatenated, encryptedBlock.length, timeBytes.length);
        }

        // 调用哈希函数
        return hash(concatenated);
    }

    // 计算哈希函数（这里使用 SHA-256，或者可以用你自己的实现）
    private static byte[] hash(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    // 将 long 类型转换为字节数组（8 字节）
    private static byte[] longToBytes(long x) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (x & 0xff);
            x >>= 8;
        }
        return result;
    }



    public static void saveTimeToProperties(List<Long> timeList, String filePath) {
        // 创建一个 Properties 对象
        Properties properties = new Properties();

        // 遍历时间戳列表，将每个时间戳添加到 Properties 中
        for (int i = 0; i < timeList.size(); i++) {
            properties.setProperty("time_" + i, String.valueOf(timeList.get(i)));
        }

        // 将 Properties 保存到文件
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            properties.store(out, "Time List"); // "Time List" 是文件的注释
            System.out.println("Time list saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 将 List<byte[]> 保存到 properties 文件
    public static void saveByteArraysToProperties(List<byte[]> list, String filePath) throws IOException {
        if (list == null || filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("List or file path cannot be null or empty.");
        }

        Properties properties = new Properties();

        // 将每个 byte[] 转换为 Base64 编码的字符串，并使用索引作为键
        for (int i = 0; i < list.size(); i++) {
            byte[] byteArray = list.get(i);
            if (byteArray == null) {
                throw new IllegalArgumentException("Byte array at index " + i + " is null.");
            }
            String encoded = Base64.getEncoder().encodeToString(byteArray);
            properties.setProperty(Integer.toString(i), encoded);
        }

        // 保存到文件
        try (OutputStream output = new FileOutputStream(filePath)) {
            properties.store(output, "List<byte[]> data");
        }
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




    public static void main(String[] args) {
        try{

            initializePairing();

            Pairing bp=PairingFactory.getPairing("a.properties");//生成双线性配对要用椭圆曲线参数

            String dir = "E:" + File.separator + "JAVA_code" + File.separator + "JavaStudy" + File.separator + "src" + File.separator + "Vehicle_Digital_Evidence" + File.separator;
            String pkFileName = dir + "pk.properties";
            String BFileName = dir + "B.properties";
            String rFileName= dir +"r.properties";
            String p_iFileName= dir +"p_i.properties";
            String timeFileName = dir +"time.properties";
            String gammaFileName = dir +"gamma.properties";
            String YFileName = dir +"Y.properties";
            long time=System.currentTimeMillis();
            // 从公钥文件加载g
            Properties gProp = loadPropFromFile(pkFileName);
            String gString = gProp.getProperty("g");
            Element g= bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();

            Element beta = bp.getZr().newRandomElement().getImmutable();
            Element B= g.powZn(beta);

            // 保存B
            Properties BProp = new Properties();
            BProp.setProperty("beta", Base64.getEncoder().encodeToString(beta.toBytes()));
            BProp.setProperty("B", Base64.getEncoder().encodeToString(B.toBytes()));
            storePropToFile(BProp, BFileName);

            //加载r
            Properties rProp = loadPropFromFile(rFileName);
            String rString = rProp.getProperty("r");
            Element r= bp.getZr().newElementFromBytes(Base64.getDecoder().decode(rString)).getImmutable();

            Element g_r_beta=(g.powZn(r)).powZn(beta);
            byte[] g_r_beta_bytes = g_r_beta.toBytes(); // 将 Element 转换为字节数组
            Element d = bp.getZr().newElementFromHash(g_r_beta_bytes, 0, g_r_beta_bytes.length).getImmutable(); // 计算哈希

            File file = new File("C:\\Users\\22867\\Desktop\\Halevi_PoW.pdf"); // 文件路径


            int blockSize = 16; // 定义块大小

            // 指定输出文件夹路径
            String encryptedBlocksDir = "C:\\Users\\22867\\Desktop\\encBlocks";
            String keysDirectory="C:\\Users\\22867\\Desktop\\keys";

            // 创建输出文件夹并检查是否创建成功
            File encryptedDir = new File(encryptedBlocksDir);
            File keysDir = new File(keysDirectory);

            if (!encryptedDir.exists() && !encryptedDir.mkdirs()) {
                System.err.println("Failed to create directory: " + encryptedBlocksDir);
                return;
            }
            if (!keysDir.exists() && !keysDir.mkdirs()) {
                System.err.println("Failed to create directory: " + keysDirectory);
                return;
            }

            // 将文件分块
            List<byte[]> blocks = splitFile(file, blockSize); // 分割文件为块
            List<byte[]> encryptedBlocks = new ArrayList<>(); // 存储加密块的列表

            List<byte[]> p = new ArrayList<>();
            List<Long> Time = new ArrayList<>();


            Field Zr=bp.getZr();//生成整数群
            Element Y=Zr.newZeroElement();

            byte[] h_pre=null;
            // 对每个块进行加密
            for (int i = 0; i < blocks.size(); i++) {
                byte[] block = blocks.get(i); // 获取当前块
                SecretKey key=deriveKeyFromData(block);//生成当前块的密钥
                saveKeyToFile(key,keysDirectory,i);      //保存密钥

                byte[] encryptedBlock = encryptBlock(block, key); // 加密当前块
                encryptedBlocks.add(encryptedBlock); // 将加密块添加到列表

                //计算重复数据删除标签和Y
                Element y_i=bp.getZr().newElementFromHash(encryptedBlock, 0, encryptedBlock.length).getImmutable();
                //System.out.println(y_i);
              //  System.out.println(y_i.toString());
                Y=Y.duplicate().add(y_i);

                byte[] p_i=xorElements(y_i,d);
                p.add(p_i);

                byte[] h;
               // long time=System.currentTimeMillis();
                Time.add(time);
                if(i==0){
                    h=calculateHash(encryptedBlock,time,null);
                }else{
                    h=calculateHash(encryptedBlock,time,h_pre);
                }
               // System.out.println("i:"+i+" h: "+h+" h_pre "+h_pre);

                h_pre = h;

                // 保存加密块到文件
                String encryptedBlockFilePath = encryptedBlocksDir + File.separator + "encrypted_block_" + i + ".dat";
                saveBlockToFile(encryptedBlock, encryptedBlockFilePath); // 保存加密块
            }
            System.out.println(System.currentTimeMillis()-time+"ms");

            // 保存gamma
            Element gamma = bp.getZr().newElementFromBytes(h_pre);
            Properties gammaProp = new Properties();
            gammaProp.setProperty("gamma", Base64.getEncoder().encodeToString(gamma.toBytes()));
            storePropToFile(gammaProp, gammaFileName);

            //保存Y
            Properties YProp = new Properties();
            YProp.setProperty("Y", Base64.getEncoder().encodeToString(Y.toBytes()));
            storePropToFile(YProp, YFileName);

            saveTimeToProperties(Time,timeFileName);
            saveByteArraysToProperties(p,p_iFileName);


        }catch (Exception e) {
            e.printStackTrace(); // 打印异常堆栈信息
        }
    }
}
