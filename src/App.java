import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.util.*;
import java.util.Map.Entry;

public class App {

    public static BufferedImage convertToGrayscale(BufferedImage RGBImage) {
        BufferedImage grayImage = new BufferedImage(
            RGBImage.getWidth(),
            RGBImage.getHeight(),
            BufferedImage.TYPE_BYTE_GRAY);
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(RGBImage, grayImage);
        return grayImage;
    }

    public static List<int[]> divideIntoBlocks(BufferedImage grayImage, int blockSize) {
        List<int[]> blocks = new ArrayList<>();
        int imgWidth = grayImage.getWidth();
        int imgHeight = grayImage.getHeight();
        for (int y = 0; y <= imgHeight - blockSize; y += blockSize) {
            for (int x = 0; x <= imgWidth - blockSize; x += blockSize) {
                int[] blockVector = new int[blockSize * blockSize];
                int index = 0;
                for (int dy = 0; dy < blockSize; dy++) {
                    for (int dx = 0; dx < blockSize; dx++) {
                        int pixel = grayImage.getRGB(x + dx, y + dy) & 0xFF;
                        blockVector[index++] = pixel;
                    }
                }
                blocks.add(blockVector);
            }
        }
        return blocks;
    }

    public static double[] computeAverageVector(List<int[]> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return new double[0];
        }
        int vectorLength = blocks.get(0).length;
        double[] avgVector = new double[vectorLength];
        for (int[] block : blocks) {
            for (int i = 0; i < vectorLength; i++) {
                avgVector[i] += block[i];
            }
        }
        int blockCount = blocks.size();
        for (int i = 0; i < vectorLength; i++) {
            avgVector[i] /= blockCount;
        }
        return avgVector;
    }

    public static double Compute_Distance(int[] vector1, double[] vector2) {
        double distance = 0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            distance += diff * diff;
        }
        return Math.sqrt(distance);
    }

    public static double[][] splitVector(double[] vector) {
        int length = vector.length;
        double[] plusHalf = new double[length];
        double[] minusHalf = new double[length];
        for (int i = 0; i < length; i++) {
            plusHalf[i] = vector[i] + 5.0;
            minusHalf[i] = vector[i] - 5.0;
        }
        return new double[][]{plusHalf, minusHalf};
    }

    public static HashMap<String, double[]> Create_Code_Book(List<int[]> blocks, int codeBookSize) {
        if (blocks == null || blocks.isEmpty()) {
            return new HashMap<>();
        }
        List<double[]> startvector = new ArrayList<>();
        startvector.add(computeAverageVector(blocks));
        while (startvector.size() < codeBookSize) {
            List<double[]> newvector = new ArrayList<>();
            for (double[] vector : startvector) {
                double[][] split = splitVector(vector);
                double[] plusHalf = split[0];
                double[] minusHalf = split[1];
                List<int[]> plusHalfBlocks = new ArrayList<>();
                List<int[]> minusHalfBlocks = new ArrayList<>();
                for (int[] block : blocks) {
                    double distToPlus = Compute_Distance(block, plusHalf);
                    double distToMinus = Compute_Distance(block, minusHalf);
                    if (distToPlus < distToMinus) {
                        plusHalfBlocks.add(block);
                    } else {
                        minusHalfBlocks.add(block);
                    }
                }
                double[] plusAvg = plusHalfBlocks.isEmpty() ? plusHalf : computeAverageVector(plusHalfBlocks);
                double[] minusAvg = minusHalfBlocks.isEmpty() ? minusHalf : computeAverageVector(minusHalfBlocks);
                newvector.add(plusAvg);
                newvector.add(minusAvg);
            }
            startvector = newvector;
        }
        boolean changed;
        HashMap<Integer, List<int[]>> assignments = new HashMap<>();
        Random random = new Random();
        do {
            changed = false;
            assignments.clear();
            for (int i = 0; i < startvector.size(); i++) {
                assignments.put(i, new ArrayList<>());
            }
            for (int[] block : blocks) {
                int bestIndex = 0;
                double bestDist = Double.MAX_VALUE;
                for (int i = 0; i < startvector.size(); i++) {
                    double dist = Compute_Distance(block, startvector.get(i));
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestIndex = i;
                    }
                }
                assignments.get(bestIndex).add(block);
            }
            for (int i = 0; i < startvector.size(); i++) {
                if (assignments.get(i).isEmpty() && !blocks.isEmpty()) {
                    assignments.get(i).add(blocks.get(random.nextInt(blocks.size())));
                    changed = true;
                }
            }
            for (int i = 0; i < startvector.size(); i++) {
                List<int[]> group = assignments.get(i);
                if (!group.isEmpty()) {
                    double[] newvector = computeAverageVector(group);
                    if (!Arrays.equals(startvector.get(i), newvector)) {
                        startvector.set(i, newvector);
                        changed = true;
                    }
                }
            }
        } while (changed);
        HashMap<String, double[]> codeBook = new HashMap<>();
        int keyLength = (int) Math.ceil(Math.log(codeBookSize) / Math.log(2));
        for (int i = 0; i < startvector.size(); i++) {
            String binaryCode = String.format("%" + keyLength + "s", Integer.toBinaryString(i)).replace(' ', '0');
            codeBook.put(binaryCode, startvector.get(i));
        }
        return codeBook;
    }

    public static List<String> create_compressed_img(List<int[]> blocks, HashMap<String, double[]> codeBook) {
        List<String> compressedBlocks = new ArrayList<>();
        for (int[] block : blocks) {
            double minDistance = Double.MAX_VALUE;
            String minCode = null;
            for (Entry<String, double[]> entry : codeBook.entrySet()) {
                String code = entry.getKey();
                double[] centroid = entry.getValue();
                double distance = Compute_Distance(block, centroid);
                if (distance < minDistance) {
                    minDistance = distance;
                    minCode = code;
                }
            }
            compressedBlocks.add(minCode != null ? minCode : "");
        }
        return compressedBlocks;
    }

    public static List<int[]> Decompress(List<String> compressedBlocks, HashMap<String, double[]> codeBook, int blockLength) {
        List<int[]> decompressedBlocks = new ArrayList<>();
        for (String binaryCode : compressedBlocks) {
            double[] centroid = codeBook.get(binaryCode);
            if (centroid != null) {
                int[] vectorInt = new int[centroid.length];
                for (int i = 0; i < centroid.length; i++) {
                    vectorInt[i] = (int) Math.round(centroid[i]);
                }
                decompressedBlocks.add(vectorInt);
            } else {
                decompressedBlocks.add(new int[blockLength]);
            }
        }
        return decompressedBlocks;
    }

    public static BufferedImage reconstructImage(List<int[]> decompressedBlocks, int imgWidth, int imgHeight, int blockSize) {
        BufferedImage reconstructedImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        int blockIndex = 0;
        for (int y = 0; y <= imgHeight - blockSize && blockIndex < decompressedBlocks.size(); y += blockSize) {
            for (int x = 0; x <= imgWidth - blockSize && blockIndex < decompressedBlocks.size(); x += blockSize) {
                int[] block = decompressedBlocks.get(blockIndex);
                int pixelIndex = 0;
                for (int dy = 0; dy < blockSize; dy++) {
                    for (int dx = 0; dx < blockSize; dx++) {
                        if (pixelIndex < block.length) {
                            int pixelValue = Math.max(0, Math.min(255, block[pixelIndex]));
                            int rgb = (pixelValue << 16) | (pixelValue << 8) | pixelValue;
                            reconstructedImage.setRGB(x + dx, y + dy, rgb);
                            pixelIndex++;
                        }
                    }
                }
                blockIndex++;
            }
        }
        return reconstructedImage;
    }

    public static double Compute_MSE(BufferedImage originalImage, BufferedImage reconstructedImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int totalPixels = width * height;
        double totalError = 0.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalPixel = originalImage.getRGB(x, y) & 0xFF;
                int reconstructedPixel = reconstructedImage.getRGB(x, y) & 0xFF;
                int error = originalPixel - reconstructedPixel;
                totalError += error * error;
            }
        }
        return totalError / totalPixels;
    }

    public static int Compute_img_size(BufferedImage image, int bitsPerPixel) {
        int width = image.getWidth();
        int height = image.getHeight();
        return width * height * bitsPerPixel / 8;
    }

    public static double Compute_CodeBook_size(HashMap<String, double[]> codeBook) {
        if (codeBook == null || codeBook.isEmpty()) {
            return 0;
        }
        String anyKey = codeBook.keySet().iterator().next();
        int keyLength = anyKey.length();
        return Math.pow(2, keyLength);
    }

    public static void saveCodeBook(HashMap<String, double[]> codeBook, String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(codeBook);
        }
    }


    public static HashMap<String, double[]> loadCodeBook(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (HashMap<String, double[]>) ois.readObject();
        }
    }

    public static void saveCompressedBlocks(List<String> compressedBlocks, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String code : compressedBlocks) {
                writer.write(code);
                writer.newLine();
            }
        }
    }

    public static List<String> loadCompressedBlocks(String filePath) throws IOException {
        List<String> compressedBlocks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                compressedBlocks.add(line);
            }
        }
        return compressedBlocks;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nVector Quantization Image Compression");
            System.out.println("1: Compress");
            System.out.println("2: Decompress");
            System.out.println("3: Exit");
            System.out.print("Choose an option (1-3): ");
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number (1-3).");
                continue;
            }

            if (choice == 3) {
                System.out.println("Exiting...");
                break;
            }

            if (choice == 1) {
                try {
                    System.out.print("Enter input grayscale image path (e.g., assets\\girlgray.bmp): ");
                    String inputImagePath = scanner.nextLine();
                    BufferedImage grayImage = ImageIO.read(new File(inputImagePath));

                    System.out.print("Enter block size (e.g., 4 or 8 for 4x4 or 8x8 blocks): ");
                    int blockSize;
                    try {
                        blockSize = Integer.parseInt(scanner.nextLine());
                        if (blockSize <= 0 || blockSize > Math.min(grayImage.getWidth(), grayImage.getHeight())) {
                            System.out.println("Invalid block size. Must be positive and not exceed image dimensions.");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid block size. Please enter a positive integer.");
                        continue;
                    }

                    System.out.print("Enter codebook size (e.g., 16, 32, 64): ");
                    int codeBookSize;
                    try {
                        codeBookSize = Integer.parseInt(scanner.nextLine());
                        if (codeBookSize <= 0) {
                            System.out.println("Invalid codebook size. Must be positive.");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid codebook size. Please enter a positive integer.");
                        continue;
                    }

                    System.out.print("Enter output path for compressed data (e.g., assets\\compressed.txt): ");
                    String compressedPath = scanner.nextLine();
                    String codeBookPath = "assets\\codebook.ser";

                    List<int[]> blocks = divideIntoBlocks(grayImage, blockSize);
                    System.out.println("Total blocks: " + blocks.size());
                    if (blocks.isEmpty()) {
                        System.out.println("No blocks generated. Check image dimensions and block size.");
                        continue;
                    }

                    for (int i = 0; i < Math.min(5, blocks.size()); i++) {
                        System.out.println("Block " + i + ": " + Arrays.toString(blocks.get(i)));
                    }

                    HashMap<String, double[]> codeBook = Create_Code_Book(blocks, codeBookSize);
                    List<String> compressedBlocks = create_compressed_img(blocks, codeBook);

                    saveCodeBook(codeBook, codeBookPath);
                    saveCompressedBlocks(compressedBlocks, compressedPath);

                    int keyLength = codeBook.isEmpty() ? 0 : codeBook.keySet().iterator().next().length();
                    int imgSize = Compute_img_size(grayImage, 8);
                    int codeBookStorageSize = codeBookSize * blockSize * blockSize * 8;
                    int compressedDataSize = compressedBlocks.size() * keyLength / 8;
                    double compressionRatio = (double) imgSize / (compressedDataSize + codeBookStorageSize);

                    System.out.println("Image Size (bytes): " + imgSize);
                    System.out.println("Image size: " + grayImage.getWidth() + "x" + grayImage.getHeight());
                    System.out.println("Codebook Size (entries): " + Compute_CodeBook_size(codeBook));
                    System.out.println("Compressed Data Size (bytes): " + compressedDataSize);
                    System.out.println("Codebook Storage Size (bytes, approx): " + codeBookStorageSize);
                    System.out.println("Compression Ratio: " + compressionRatio);

                    int count = 0;
                    for (Entry<String, double[]> entry : codeBook.entrySet()) {
                        if (count >= 5) break;
                        System.out.println("Binary Code: " + entry.getKey());
                        System.out.println("Centroid: " + Arrays.toString(entry.getValue()));
                        count++;
                    }

                } catch (IOException e) {
                    System.out.println("Error during compression: " + e.getMessage());
                }

            } else if (choice == 2) {
                try {
                    System.out.print("Enter compressed data path (e.g., assets\\compressed.txt): ");
                    String compressedPath = scanner.nextLine();
                    String codeBookPath = "assets\\codebook.ser";
                    System.out.print("Enter output image path (e.g., assets\\reconstruct.bmp): ");
                    String outputImagePath = scanner.nextLine();
                    System.out.print("Enter original image width: ");
                    int imgWidth;
                    try {
                        imgWidth = Integer.parseInt(scanner.nextLine());
                        if (imgWidth <= 0) {
                            System.out.println("Invalid width. Must be positive.");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid width. Please enter a positive integer.");
                        continue;
                    }
                    System.out.print("Enter original image height: ");
                    int imgHeight;
                    try {
                        imgHeight = Integer.parseInt(scanner.nextLine());
                        if (imgHeight <= 0) {
                            System.out.println("Invalid height. Must be positive.");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid height. Please enter a positive integer.");
                        continue;
                    }
                    System.out.print("Enter block size used during compression (e.g., 4 or 8): ");
                    int blockSize;
                    try {
                        blockSize = Integer.parseInt(scanner.nextLine());
                        if (blockSize <= 0 || blockSize > Math.min(imgWidth, imgHeight)) {
                            System.out.println("Invalid block size. Must be positive and not exceed image dimensions.");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid block size. Please enter a positive integer.");
                        continue;
                    }

                    HashMap<String, double[]> codeBook = loadCodeBook(codeBookPath);
                    List<String> compressedBlocks = loadCompressedBlocks(compressedPath);

                    List<int[]> decompressedBlocks = Decompress(compressedBlocks, codeBook, blockSize * blockSize);
                    BufferedImage reconstructedImage = reconstructImage(decompressedBlocks, imgWidth, imgHeight, blockSize);

                    ImageIO.write(reconstructedImage, "bmp", new File(outputImagePath));
                    System.out.println("Reconstructed image saved to: " + outputImagePath);

                    System.out.print("Enter original grayscale image path for MSE calculation (or press Enter to skip): ");
                    String originalImagePath = scanner.nextLine();
                    if (!originalImagePath.isEmpty()) {
                        BufferedImage originalImage = ImageIO.read(new File(originalImagePath));
                        double mse = Compute_MSE(originalImage, reconstructedImage);
                        System.out.println("Mean Squared Error (MSE): " + mse);
                    }

                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Error during decompression: " + e.getMessage());
                }

            } else {
                System.out.println("Invalid option. Please choose 1, 2, or 3.");
            }
        }
        scanner.close();
    }
}