package Vehicle_Digital_Evidence;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import java.util.*;

public class DataDecrypted {

    // 聚合文件夹中的所有 .dat 文件块到一个文件中,与下一个方法绑定
    public static void aggregateBlocksFromFolder(String folderPath, String outputPath) throws IOException {
        // 获取文件夹中的所有文件
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".dat"));  // 只选择以 .dat 结尾的文件

        if (files == null) {
            throw new IllegalArgumentException("Invalid folder path: " + folderPath);
        }

        // 按文件名中的块索引排序
        Arrays.sort(files, Comparator.comparingInt(Vehicle_Digital_Evidence.DataDecrypted::extractBlockIndex));

        // 创建文件输出流
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            for (File file : files) {
                if (file.isFile()) {
                    // 将文件内容读取到字节数组中
                    byte[] buffer = Files.readAllBytes(file.toPath());
                    // 将字节数组写入输出文件
                    fos.write(buffer);
                }
            }
        }
    }

    // 从文件名中提取块索引
    private static int extractBlockIndex(File file) {
        String name = file.getName();
        int start = name.lastIndexOf('_') + 1; // 找到最后一个下划线的位置
        int end = name.lastIndexOf('.');       // 找到点的位置
        String indexStr = name.substring(start, end); // 提取索引部分
        return Integer.parseInt(indexStr); // 转换为整数
    }

    // 从文件读取块
    public static byte[] readBlockFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] block = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(block);
        }
        return block;
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

    public static void saveBlockToFile(byte[] block, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) { // 创建文件输出流
            fos.write(block); // 将块写入文件
        }
    }

    // 从文件中读取密钥，并将其转换为 SecretKey
    public static SecretKey readKeyFromFile(String directory, int blockIndex) throws IOException {
        // 构造文件路径
        String keyFilePath = directory + File.separator + "block_key_" + blockIndex + ".dat";

        // 读取文件中的Base64编码密钥
        String encodedKey;
        try (BufferedReader reader = new BufferedReader(new FileReader(keyFilePath))) {
            encodedKey = reader.readLine(); // 假设密钥是以一行字符串的形式保存
        }

        // 将Base64编码的密钥转换为字节数组
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

        // 使用SecretKeySpec将字节数组转换为SecretKey
        return new SecretKeySpec(keyBytes, "AES");
    }
    // 解密数据块
    public static byte[] decryptBlock(byte[] encryptedBlock, int blockIndex, String directory) throws Exception {
        // 从文件夹中读取密钥
        SecretKey key = readKeyFromFile(directory, blockIndex);

        // 获取AES解密实例
        Cipher cipher = Cipher.getInstance("AES");

        // 初始化解密模式和密钥
        cipher.init(Cipher.DECRYPT_MODE, key);

        // 解密数据块并返回解密后的字节数组
        return cipher.doFinal(encryptedBlock);
    }

    public static void main(String[] args) {
        try{
            long time=System.currentTimeMillis();
            String encryptedBlockFilePath = "C:\\Users\\22867\\Desktop\\encBlocks";
            String decryptedBlocksDir = "C:\\Users\\22867\\Desktop\\decBlocks";
            String keysDirectory="C:\\Users\\22867\\Desktop\\keys";

            File decryptedDir = new File(decryptedBlocksDir);

            if (!decryptedDir.exists() && !decryptedDir.mkdirs()) {
                System.err.println("Failed to create directory: " + decryptedBlocksDir);
                return;
            }

            int count=countDatFilesInDirectory(encryptedBlockFilePath);
            // 解密加密块并保存
            List<byte[]> decryptedBlocks = new ArrayList<>(); // 存储解密块的列表
            for (int i = 0; i < count; i++) {

                // 从文件读取加密块
                String encryptedBlockDir = encryptedBlockFilePath + File.separator + "encrypted_block_" + i + ".dat";
                byte[] encryptedBlock = readBlockFromFile(encryptedBlockDir);

                // 解密块
                byte[] decryptedBlock = decryptBlock(encryptedBlock, i, keysDirectory);
                decryptedBlocks.add(decryptedBlock);

                // 保存解密块到文件
                String decryptedBlockFilePath = decryptedBlocksDir + File.separator + "decrypted_block_" + i + ".dat";
                saveBlockToFile(decryptedBlock, decryptedBlockFilePath);
            }

            // 聚合解密后的块恢复原文件
            String restoredFilePath = "C:\\Users\\22867\\Desktop\\RestoredFile.pdf";
            aggregateBlocksFromFolder(decryptedBlocksDir, restoredFilePath); // 聚合解密块恢复文件
            System.out.println(System.currentTimeMillis()-time+"ms");

        }catch (Exception e) {
            e.printStackTrace(); // 打印异常堆栈信息
        }
    }
}

