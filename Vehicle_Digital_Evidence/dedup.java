package Vehicle_Digital_Evidence;

import java.io.*;
import java.security.*;
import java.util.*;

public class dedup {

    public static void main(String[] args) throws Exception {
        // 示例路径：可换成你自己的路径
        List<String> videoPaths = Arrays.asList("C:\\Users\\22867\\Desktop\\video1.mp4", "C:\\Users\\22867\\Desktop\\video3.mp4");
        int blockSize = 1024; // 1MB

        // 存放每个视频对应的哈希集合
        Map<String, Set<String>> videoHashMap = new HashMap<>();

        for (String videoPath : videoPaths) {
            File videoFile = new File(videoPath);
            String videoName = videoFile.getName().replaceAll("\\..+$", "");
            File outputDir = new File("blocks_" + videoName);
            outputDir.mkdirs();

            Set<String> hashSet = splitAndHash(videoFile, blockSize, outputDir);
            videoHashMap.put(videoName, hashSet);

            System.out.println("[" + videoName + "] Blocks: " + hashSet.size());
        }

        // 进行视频间重复块分析
        compareVideos(videoHashMap);
    }

    public static Set<String> splitAndHash(File file, int blockSize, File outputDir) throws Exception {
        Set<String> hashSet = new HashSet<>();
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[blockSize];
            int index = 0;
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] actualBytes = Arrays.copyOf(buffer, bytesRead);
                String hash = sha256(actualBytes);
                hashSet.add(hash);

                File blockFile = new File(outputDir, "block_" + index + ".bin");
                try (FileOutputStream out = new FileOutputStream(blockFile)) {
                    out.write(actualBytes);
                }
                index++;
            }
        }
        return hashSet;
    }

    public static String sha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void compareVideos(Map<String, Set<String>> videoHashMap) {
        List<String> videoNames = new ArrayList<>(videoHashMap.keySet());

        System.out.println("\n🔍 Cross-video duplicate block analysis:");

        for (int i = 0; i < videoNames.size(); i++) {
            for (int j = i + 1; j < videoNames.size(); j++) {
                String v1 = videoNames.get(i);
                String v2 = videoNames.get(j);

                Set<String> set1 = videoHashMap.get(v1);
                Set<String> set2 = videoHashMap.get(v2);

                Set<String> intersection = new HashSet<>(set1);
                intersection.retainAll(set2); // 取交集

                int duplicates = intersection.size();
                System.out.println(" - " + v1 + " vs " + v2 + " → " + duplicates + " duplicate blocks");

                // 可选：输出重复率
                double rate1 = 100.0 * duplicates / set1.size();
                double rate2 = 100.0 * duplicates / set2.size();
                System.out.printf("   ↳ Shared rate: %.2f%% (%s), %.2f%% (%s)\n\n", rate1, v1, rate2, v2);
            }
        }
    }
}
