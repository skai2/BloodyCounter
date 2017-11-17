package BPCounter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import javax.swing.JComboBox;
import org.opencv.core.Size;

/**
 *
 * @author SKai
 */
public class BPCounter extends JFrame {

    // Variables to save test images
    final static String format = "png";
    final static String fileName = "PartialScreenshot." + format;
    
    // Main Variables (Counter object and UI Elements)
    private static BloodPoints bps;
    private final static String category1 = "Objective";
    private static JLabel objectiveL = new JLabel();
    private final static String category2 = "Survival";
    private static JLabel survivalL = new JLabel();
    private final static String category3 = "Altruism";
    private static JLabel altruismL = new JLabel();
    private final static String category4 = "Boldness";
    private static JLabel boldnessL = new JLabel();
    private final static String category0 = "Unknown";
    private static JLabel unknownL = new JLabel();
    private final static String totals = "Total";
    private static JLabel totalL = new JLabel();
    private static JTextArea log = new JTextArea();
    private final static int windowX = 340;
    private final static int windowY = 300;
    
    // Settings and program variables
    private static int sleepTime = 0;
    private static Boolean noProcessing = false;
    private static Boolean debug = false;
    private static ITesseract tesseract;
    private static GraphicsEnvironment ge;
    private static GraphicsDevice mainScreen;
    private static Robot screenCapBot;
    private static Dimension screenSize;
    private int widthArea;
    private int heightArea;
    private static Rectangle captureArea;
    private static double captureSize;
    private static Queue<BloodPacket> packQueue;
    private SwingWorker fetchWorker;
    
    // Version String
    private final static String version = "17.11.17.20.38";

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            BPCounter prog = new BPCounter();
            prog.setVisible(true);
        });
    }

    public BPCounter() {
        bps = new BloodPoints();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.load("C:/OpenCV3.3/opencv/build/java/x64/opencv_java330.dll");
        
        // Prepare Tesseract and image queue
        tesseract = new Tesseract();
        tesseract.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345789+-!");
        packQueue = new LinkedList<BloodPacket>();

        // Prepare settings for capture
        ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        mainScreen = ge.getDefaultScreenDevice();
        try {screenCapBot = new Robot(mainScreen);} catch (Exception ex) {ex.printStackTrace();}
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        widthArea = (int) Math.round(screenSize.getHeight() / 4);
        heightArea = (int) Math.round(screenSize.getHeight() / 13.5);
        captureArea = new Rectangle((widthArea / 2), (screenSize.height / 2), (widthArea * 3 / 4), heightArea);
        captureSize = captureArea.height * captureArea.width;

        // Initiate
        InitUI();
        reset();
        fetchWorker = new BackgroundBPFetch();
        fetchWorker.execute();
    }

    private void InitUI() {
        setTitle("DBD - Bloody Counter");
        setLayout(new FlowLayout());
        setSize(windowX, windowY);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel frame1 = new JPanel();
        frame1.setPreferredSize(new Dimension((windowX / 4) - 10, windowY / 5));
        frame1.setLayout(new GridLayout(2, 1));
        frame1.setBackground(Color.LIGHT_GRAY);
        add(frame1);
        JPanel frame2 = new JPanel();
        frame2.setPreferredSize(new Dimension((windowX / 4) - 10, windowY / 5));
        frame2.setLayout(new GridLayout(2, 1));
        frame2.setBackground(Color.LIGHT_GRAY);
        add(frame2);
        JPanel frame3 = new JPanel();
        frame3.setPreferredSize(new Dimension((windowX / 4) - 10, windowY / 5));
        frame3.setLayout(new GridLayout(2, 1));
        frame3.setBackground(Color.LIGHT_GRAY);
        add(frame3);
        JPanel frame4 = new JPanel();
        frame4.setPreferredSize(new Dimension((windowX / 4) - 10, windowY / 5));
        frame4.setLayout(new GridLayout(2, 1));
        frame4.setBackground(Color.LIGHT_GRAY);
        add(frame4);

        frame1.add(new JLabel(category1));
        frame1.add(objectiveL);
        frame2.add(new JLabel(category2));
        frame2.add(survivalL);
        frame3.add(new JLabel(category3));
        frame3.add(altruismL);
        frame4.add(new JLabel(category4));
        frame4.add(boldnessL);

        add(new JLabel(category0 + ": "));
        add(unknownL);
        add(new JLabel("                    "));
        add(new JLabel(totals + ": "));
        add(totalL);

        JScrollPane scroll = new JScrollPane(log, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(new Dimension(windowX - 20, (windowY * 2 / 5) - 10));
//        scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
//            public void adjustmentValueChanged(AdjustmentEvent e) {
//                e.getAdjustable().setValue(e.getAdjustable().getMaximum());
//            }
//        });
        add(scroll);

        add(new JLabel("Sleep:"));
        JComboBox sleepBox = new JComboBox<Integer>();
        sleepBox.addItem(0);
        sleepBox.addItem(10);
        sleepBox.addItem(25);
        sleepBox.addItem(50);
        sleepBox.addItem(100);
        sleepBox.addItem(250);
        sleepBox.addItem(500);
        sleepBox.setSelectedItem(0);
        sleepBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JComboBox<Integer> combo = (JComboBox<Integer>) event.getSource();
                sleepTime = (int) combo.getSelectedItem();
//                System.out.println(sleepTime);
            }
        });
        add(sleepBox);

        JToggleButton processingToggle = new JToggleButton("No Pre-Processing");
        ItemListener processingListener = new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                int state = itemEvent.getStateChange();
                if (state == ItemEvent.SELECTED) {
                    noProcessing = true;
                } else {
                    noProcessing = false;
                }
            }
        };
        processingToggle.addItemListener(processingListener);
//        add(processingToggle);

        JToggleButton debugToggle = new JToggleButton("Debug");
        ItemListener debugListener = new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                int state = itemEvent.getStateChange();
                if (state == ItemEvent.SELECTED) {
                    debug = true;
                } else {
                    debug = false;
                }
            }
        };
        debugToggle.addItemListener(debugListener);
        add(debugToggle);

        JButton reset = new JButton("Reset");
        reset.addActionListener((ActionEvent event) -> {
            reset();
        });
        add(reset);

        add(new JLabel("version: " + version));
    }

    private void reset() {
        log.setText("");
        bps.objective = 0;
        bps.survival = 0;
        bps.altruism = 0;
        bps.boldness = 0;
        bps.unknown = 0;
        objectiveL.setText("" + bps.objective);
        survivalL.setText("" + bps.survival);
        altruismL.setText("" + bps.altruism);
        boldnessL.setText("" + bps.boldness);
        unknownL.setText("" + bps.unknown);
        totalL.setText("" + (bps.objective + bps.survival + bps.altruism + bps.boldness + bps.unknown));
        objectiveL.setForeground(Color.BLACK);
        survivalL.setForeground(Color.BLACK);
        altruismL.setForeground(Color.BLACK);
        boldnessL.setForeground(Color.BLACK);
        unknownL.setForeground(Color.BLACK);
        objectiveL.validate();
        survivalL.validate();
        altruismL.validate();
        boldnessL.validate();
        unknownL.validate();
        totalL.validate();
    }

    private class BackgroundBPFetch extends SwingWorker<Integer, String> {

        @Override
        protected Integer doInBackground() throws Exception {
//            double prevRatio = 0;
            Mat testBlack = new Mat();
            while (true) {
                // Begin by capturing screen
                long initTime = System.currentTimeMillis();
                BufferedImage screen = getScreenCapture(0);

                // Process capture
//                long procTime = System.currentTimeMillis();
                screen = processScreenCapture(screen);
//                System.out.println("Proc: " + (System.currentTimeMillis() - procTime));

                // Test black ratio to see if worth doing OCR
                Imgproc.cvtColor(img2Mat(screen), testBlack, Imgproc.COLOR_BGR2GRAY);
                double ratioBW = 1 - ((double) Core.countNonZero(testBlack) / captureSize);
//                System.out.println("Ratio: " + ratioBW);
                if (ratioBW > 0.01) {
//                    prevRatio = ratioBW;

                    // Do OCR
                    packQueue.add(new BloodPacket("err", -1, screen, System.currentTimeMillis()));
                    long readTime = System.currentTimeMillis();
                    BloodPacket pack = readBloodPacket(packQueue.poll());
                    System.out.println("Read: " + (System.currentTimeMillis() - readTime));

                    // Try adding packet to counter
//                    long addTime = System.currentTimeMillis();
                    bps.add(pack);
//                    System.out.println("Add: " + (System.currentTimeMillis() - addTime));

                    // Update counter UI
//                    long uiTime = System.currentTimeMillis();
                    objectiveL.setText("" + bps.objective);
                    survivalL.setText("" + bps.survival);
                    altruismL.setText("" + bps.altruism);
                    boldnessL.setText("" + bps.boldness);
                    unknownL.setText("" + bps.unknown);
                    totalL.setText("" + (bps.objective + bps.survival + bps.altruism + bps.boldness + bps.unknown));
                    if (bps.objective >= 5000) {
                        objectiveL.setForeground(Color.RED);
                    }
                    if (bps.survival >= 5000) {
                        survivalL.setForeground(Color.RED);
                    }
                    if (bps.altruism >= 5000) {
                        altruismL.setForeground(Color.RED);
                    }
                    if (bps.boldness >= 5000) {
                        boldnessL.setForeground(Color.RED);
                    }
                    if (bps.unknown >= 5000) {
                        unknownL.setForeground(Color.RED);
                    }
                    objectiveL.validate();
                    survivalL.validate();
                    altruismL.validate();
                    boldnessL.validate();
                    unknownL.validate();
                    totalL.validate();
//                    System.out.println("UI: " + (System.currentTimeMillis() - uiTime));
                }

                // Sleep
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.out.println("Full: " + (System.currentTimeMillis() - initTime));
            }
        }
    }

    private static double percDiff(double a, double b) {
        double temp = 1 - (a / b);
        return Math.abs(temp);
    }

    private static BufferedImage getScreenCapture(int position) {
        try {
            BufferedImage screenCapture = screenCapBot.createScreenCapture(captureArea);
//            ImageIO.write(screenCapture, format, new File(fileName));
            return screenCapture;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static BufferedImage processScreenCapture(BufferedImage image) {
        Mat originalMat = new Mat();
        Mat redMat = new Mat();
        Mat whiteMat = new Mat();
        Mat resultMat1 = new Mat();
        Mat resultMat2 = new Mat();
        Mat resultMat = new Mat();
        originalMat = img2Mat(image);

        originalMat.copyTo(resultMat1);
        Core.inRange(resultMat1, new Scalar(120, 120, 120), new Scalar(255, 255, 255), whiteMat);
        Imgproc.cvtColor(resultMat1, resultMat1, Imgproc.COLOR_BGR2HSV);
        Mat red1 = new Mat(), red2 = new Mat();
        Core.inRange(resultMat1, new Scalar(0, 70, 50), new Scalar(10, 255, 255), red1);
        Core.inRange(resultMat1, new Scalar(170, 70, 50), new Scalar(180, 255, 255), red2);
        Core.add(red1, red2, redMat);
        Core.add(whiteMat, redMat, resultMat1);

        Imgproc.cvtColor(originalMat, resultMat2, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(resultMat2, resultMat2, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 131, 2);
//        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3,3));;
//        Imgproc.erode(resultMat2, resultMat2, element);

        Core.multiply(resultMat1, resultMat2, resultMat);
        Core.bitwise_not(resultMat, resultMat);

        BufferedImage resultImage = mat2Img(resultMat);
        resultImage = img2BufferedImage(resultImage.getScaledInstance(resultImage.getWidth() * 3, resultImage.getHeight() * 3, Image.SCALE_SMOOTH));
        Imgproc.GaussianBlur(resultMat, resultMat, new Size(5, 5), 0);
        try {
//            ImageIO.write(resultImage, format, new File(fileName));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resultImage;
    }

    private static Mat img2Mat(BufferedImage in) {
        Mat out;
        byte[] data;
        int r, g, b;
        int width = in.getWidth();
        int height = in.getHeight();
        if (in.getType() == BufferedImage.TYPE_INT_RGB || in.getType() == BufferedImage.TYPE_INT_ARGB) {
            out = new Mat(height, width, CvType.CV_8UC3);
            data = new byte[height * width * (int) out.elemSize()];
            int[] dataBuff = in.getRGB(0, 0, width, height, null, 0, width);
            for (int i = 0; i < dataBuff.length; i++) {
                data[i * 3 + 2] = (byte) ((dataBuff[i] >> 16) & 0xFF);
                data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
                data[i * 3] = (byte) ((dataBuff[i] >> 0) & 0xFF);
            }
        } else {
            out = new Mat(height, width, CvType.CV_8UC1);
            data = new byte[height * width * (int) out.elemSize()];
            int[] dataBuff = in.getRGB(0, 0, width, height, null, 0, width);
            for (int i = 0; i < dataBuff.length; i++) {
                r = (byte) ((dataBuff[i] >> 16) & 0xFF);
                g = (byte) ((dataBuff[i] >> 8) & 0xFF);
                b = (byte) ((dataBuff[i] >> 0) & 0xFF);
                data[i] = (byte) ((0.21 * r) + (0.71 * g) + (0.07 * b)); //luminosity
            }
        }
        out.put(0, 0, data);
        return out;
    }

    private static BufferedImage mat2Img(Mat in) {
        BufferedImage out;
        int width = in.width();
        int height = in.height();
        byte[] data = new byte[width * height * (int) in.elemSize()];
        int type;
        in.get(0, 0, data);

        if (in.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        out = new BufferedImage(width, height, type);

        out.getRaster().setDataElements(0, 0, width, height, data);
        return out;
    }

    private static BufferedImage img2BufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    private BloodPacket readBloodPacket(BloodPacket pack) {
        try {
            String text = tesseract.doOCR(pack.snap);
            String token[] = text.split("\n");
            String name = "";
            name = name + token[0];
            for (int i = 1; i < token.length - 1; i++) {
                name = name + " " + token[i];
            }
            int points = 0;
            try {
                points = Integer.parseInt(token[token.length - 1].substring(1));
            } catch (Exception ex) {
                points = -1;
            }
            return new BloodPacket(name, points, pack.snap, pack.time);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    class BloodPacket {

        String category;
        String name, fname;
        int points;
        BufferedImage snap;
        long time;
        double accuracy = 0.0;
        final double precision = 0.7;

        public BloodPacket(String name, int points, BufferedImage snap, long time) {
            this.name = name;
            this.points = points;
            this.category = "Error";
            this.snap = snap;
            this.time = time;

            // UNCATEGORIZABLE YET
            classify(category0, "GOOD SKILL CHECK", 0, 150);
            classify(category0, "GREAT SKILL CHECK", 0, 150);
            classify(category0, "WAKE UP!", 0, 150);
            classify(category0, "HEAL", 1, 500);

            // OBJECTIVE ---------------------------------------
            classify(category1, "COOP ACTION", 1, 8000);
//            classify(category1, "GOOD SKILL CHECK");
//            classify(category1, "GREAT SKILL CHECK");
            classify(category1, "HEX SKILL CHECK", 0, 150);
            classify(category1, "REPAIRS", 1, 1250);
            classify(category1, "MAP SCOUT", 0, 150);
            classify(category1, "CHEST SEARCHED", 0, 250);
            classify(category1, "UNLOCKING", 1, 1250);
            classify(category1, "HATCH ESCAPE", 0, 2000);
            classify(category1, "HATCH OPEN", 0, 2000);

            // SURVIVAL ---------------------------------------
            classify(category2, "KILLER GRASP ESCAPE", 0, 500);
            classify(category2, "STRUGGLE", 5, 898);
//            classify(category2, "HEAL", 1, 300);
            classify(category2, "SURVIVED", 0, 5000);
            classify(category2, "TRAP ESCAPE", 0, 500);
            classify(category2, "HOOK ESCAPE", 0, 1500);
            classify(category2, "DISTRACTION", 0, 150);
//            classify(category2, "WAKE UP!");
            classify(category2, "SNAPPED OUT OF IT", 0, 200);

            // ALTRUISM ---------------------------------------
            classify(category3, "ASSIST", 250, 315);
            classify(category3, "DISTRACTION", 0, 250);
//            classify(category3, "HEAL", 400, 500);
            classify(category3, "TRAP RESCUE", 1000, 1250);
            classify(category3, "KILLER GRASP RESCUE", 0, 1250);
//            classify(category3, "GOOD SKILL CHECK");
//            classify(category3, "GREAT SKILL CHECK");
            classify(category3, "HOOK RESCUE", 1500, 1875);
            classify(category3, "REUNITED", 0, 200);
            classify(category3, "PROTECTION", 0, 200);
//            classify(category3, "WAKE UP!");

            // BOLDNESS ---------------------------------------
            classify(category4, "BOLD", 1, 250);
            classify(category4, "CHASED", 1, 8000);
            classify(category4, "ESCAPED", 0, 250);
            classify(category4, "KILLER STUN", 0, 1000);
            classify(category4, "KILLER BLIND", 0, 250);
            classify(category4, "KILLER BURN", 0, 350);
            classify(category4, "HOOK SABOTAGE", 0, 500);
            classify(category4, "TRAP DISARM", 0, 300);
            classify(category4, "HAG TRAP DISARM", 0, 400);
            classify(category4, "TRAP SABOTAGE", 0, 250);
            classify(category4, "CLEANSED", 600, 1000);
            classify(category4, "CLEANSING", 600, 1000);
//            classify(category4, "GOOD SKILL CHECK");
//            classify(category4, "GREAT SKILL CHECK");
            classify(category4, "BASEMENT TIME", 1, 8000);

//            System.out.println(this.category  + " (" +  this.accuracy*100 + "%) " + this.name  + " " +  this.points);
        }

        private void classify(String category, String testName, int min, int max) {
            double similarity = similarity(this.name, testName);
            if (similarity >= this.precision && similarity > this.accuracy) {
                if (min <= this.points && this.points <= max) {
                    this.accuracy = similarity;
                    this.category = category;
                    this.fname = testName;
                } else if (similarity >= 0.9) {
                    this.accuracy = similarity;
                    this.category = category;
                    this.fname = testName;
                    this.points = min;
                }
            }
        }
    }

    class BloodPoints {

        int objective, survival, altruism, boldness, unknown;
        long lastTime;
        final double precision = 0.5;
        BloodPacket lastPack;

        public BloodPoints() {
            this.objective = 0;
            this.survival = 0;
            this.altruism = 0;
            this.boldness = 0;
            this.unknown = 0;
            this.lastPack = new BloodPacket("Unknown", -1, null, 0);
            this.lastTime = -1;
        }

        public void add(BloodPacket pack) {
            if (lastTime == -1) {
                lastTime = System.currentTimeMillis();
            }
            long elapsedTime = pack.time - lastTime;
            if ((lastPack != null && elapsedTime < 3500
                    && (similarity(pack.name, lastPack.name) >= precision
                    && (((double) (pack.points / lastPack.points) > 0.8) || (double) (pack.points / lastPack.points) < 1.2)))
                    || pack.points < 0) {
                return;
            }
            lastPack = pack;
            lastTime = System.currentTimeMillis();
            switch (pack.category) {
                case category1:
                    this.objective += pack.points;
                    break;
                case category2:
                    this.survival += pack.points;
                    break;
                case category3:
                    this.altruism += pack.points;
                    break;
                case category4:
                    this.boldness += pack.points;
                    break;
                case category0:
                    this.unknown += pack.points;
                    break;
            }
            if (!debug && pack.category.equals("Error")) {
                return;
            }
            log.setText("\n" + log.getText());
            if (debug) {
                log.setText(pack.category + " - " + pack.name + ": " + pack.points + " (" + (int) Math.round(pack.accuracy * 100) + "%)" + log.getText());
            }
            else {
                log.setText(pack.category + " - " + pack.fname + ": " + pack.points + log.getText());
            }
            log.validate();
            try {
                File outputFile = new File("./debug/" + pack.category + "-" + pack.name + "-" + pack.points + "." + format);
                ImageIO.write(pack.snap, format, outputFile);
            } catch (Exception ex) {
//                    ex.printStackTrace();
            }
        }
    }

    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     */
    private double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
            /* both strings are zero length */ }
        /* // If you have StringUtils, you can use it to calculate the edit distance:
            return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
                               (double) longerLength; */
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;

    }

    // Example implementation of the Levenshtein Edit Distance
    // See http://rosettacode.org/wiki/Levenshtein_distance#Java
    private int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue),
                                costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }
}
