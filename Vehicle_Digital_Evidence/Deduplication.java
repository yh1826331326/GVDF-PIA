package Vehicle_Digital_Evidence;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Deduplication {

    // 计算 SHA-256 哈希值的方法
    private static String getSHA256Hash(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    // 从文件夹中读取所有文件的内容，返回内容列表
    private static List<String> readFilesFromDirectory(String directoryPath) throws IOException {
        List<String> dataBlocks = new ArrayList<>();
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        String content = new String(Files.readAllBytes(filePath));
                        dataBlocks.add(content);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return dataBlocks;
    }

    public static Result computeDuplicateLabelsAndRate(List<String> dataBlocks) throws NoSuchAlgorithmException {
        // 哈希字典来记录每个加密数据块的出现次数
        Map<String, Integer> hashMap = new HashMap<>();

        for (String block : dataBlocks) {
            String blockHash = getSHA256Hash(block);
            hashMap.put(blockHash, hashMap.getOrDefault(blockHash, 0) + 1);
        }

        // 计算每个数据块是否重复的标签
        List<Boolean> duplicateLabels = new ArrayList<>();
        int totalBlocks = dataBlocks.size();
        int duplicateCount = 0;

        for (String block : dataBlocks) {
            String blockHash = getSHA256Hash(block);
            if (hashMap.get(blockHash) > 1) {
                duplicateLabels.add(true);
                duplicateCount++;
            } else {
                duplicateLabels.add(false);
            }
        }
        System.out.println("duplicateCount:"+duplicateCount);
        System.out.println("totalBlocks:"+totalBlocks);

        // 计算重复率
        double duplicateRate = (double) duplicateCount / totalBlocks;
        System.out.println("重复率："+duplicateRate*100+" %");

        return new Result(duplicateLabels, duplicateRate);
    }

    // 结果类，用来封装重复标签和重复率
    public static class Result {
        List<Boolean> duplicateLabels;
        double duplicateRate;

        public Result(List<Boolean> duplicateLabels, double duplicateRate) {
            this.duplicateLabels = duplicateLabels;
            this.duplicateRate = duplicateRate;
        }

        public List<Boolean> getDuplicateLabels() {
            return duplicateLabels;
        }

        public double getDuplicateRate() {
            return duplicateRate;
        }
    }

    public static void main(String[] args) {
        try {
            // 指定文件夹路径
            String directoryPath = "C:\\Users\\22867\\Desktop\\encBlocks";

            // 从文件夹中读取数据块
            List<String> dataBlocks = readFilesFromDirectory(directoryPath);

            // 计算重复标签和重复率
            Result result = computeDuplicateLabelsAndRate(dataBlocks);

            // 输出重复数据删除标签
            System.out.println("重复数据删除标签：" + result.getDuplicateLabels());

            // 输出重复率
            System.out.println("重复率：" + result.getDuplicateRate());

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
