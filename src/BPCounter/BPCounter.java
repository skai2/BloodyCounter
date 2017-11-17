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
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
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

/**
 *
 * @author SKai
 */
public class BPCounter extends JFrame {

    final static String format = "png";
    final static String fileName = "PartialScreenshot." + format;
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
    private static int sleepTime = 25;
    private static Boolean noProcessing = false;
    private static Boolean debug = false;
    private final static String version = "17.11.17.01.21";

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

        InitUI();
        new BackgroundBPFetch().execute();
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
        sleepBox.addItem(10);
        sleepBox.addItem(25);
        sleepBox.addItem(50);
        sleepBox.addItem(100);
        sleepBox.addItem(250);
        sleepBox.addItem(500);
        sleepBox.addItem(1000);
        sleepBox.setSelectedItem(100);
        sleepBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JComboBox<Integer> combo = (JComboBox<Integer>) event.getSource();
                sleepTime = (int)combo.getSelectedItem();
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
        });
        add(reset);

        add(new JLabel("version: " + version));
    }

    private class BackgroundBPFetch extends SwingWorker<Integer, String> {

        @Override
        protected Integer doInBackground() throws Exception {
            while (true) {
                BufferedImage screen = getScreenCapture(0);
                if (!noProcessing) {
                    screen = processScreenCapture(screen);
                }
                BloodPacket pack = readBloodPacket(screen);
                bps.add(pack);

//                screen = getScreenCapture(1);
//                if (!noProcessing) screen = processScreenCapture(screen);
//                pack = readBloodPacket(screen);
//                bps.add(pack);
//                
//                screen = getScreenCapture(2);
//                if (!noProcessing) screen = processScreenCapture(screen);
//                pack = readBloodPacket(screen);
//                bps.add(pack);
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
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static BufferedImage getScreenCapture(int position) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice mainScreen = ge.getDefaultScreenDevice();
            Robot screenCapBot = new Robot(mainScreen);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int widthArea = (int) Math.round(screenSize.getHeight() / 4);
            int heightArea = (int) Math.round(screenSize.getHeight() / 13.5);
            Rectangle captureArea = new Rectangle(widthArea / 2, (screenSize.height / 2) + (heightArea * position), widthArea, heightArea);
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

    private BloodPacket readBloodPacket(BufferedImage image) {
        ITesseract tesseract = new Tesseract();
        tesseract.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345789+-!");
        try {
            String text = tesseract.doOCR(image);
//            System.out.println(text);
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
            return new BloodPacket(name, points);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    class BloodPacket {

        String category;
        String name;
        int points;
        double accuracy = 0.0;
        final double precision = 0.7;

        public BloodPacket(String name, int points) {
            this.name = name;
            this.points = points;
            this.category = "Error";

            // UNCATEGORIZABLE YET
            classify(category0, "GOOD SKILL CHECK");
            classify(category0, "GREAT SKILL CHECK");
            classify(category0, "WAKE UP!");

            // OBJECTIVE ---------------------------------------
            classify(category1, "COOP ACTION");
//            classify(category1, "GOOD SKILL CHECK");
//            classify(category1, "GREAT SKILL CHECK");
            classify(category1, "HEX SKILL CHECK");
            classify(category1, "REPAIRS");
            classify(category1, "MAP SCOUT");
            classify(category1, "CHEST SEARCHED");
            classify(category1, "UNLOCKING");
            classify(category1, "HATCH ESCAPE");
            classify(category1, "HATCH OPEN");

            // SURVIVAL ---------------------------------------
            classify(category2, "KILLER GRASP ESCAPE");
            classify(category2, "STRUGGLE");
            classify(category2, "SELF-HEALING");
            classify(category2, "SURVIVED");
            classify(category2, "TRAP ESCAPE");
            classify(category2, "HOOK ESCAPE");
            classify(category2, "DISTRACTION");
//            classify(category2, "WAKE UP!");
            classify(category2, "SNAPPED OUT OF IT");

            // ALTRUISM ---------------------------------------
            classify(category3, "ASSIST");
            classify(category3, "DISTRACTION");
            classify(category3, "HEAL");
            classify(category3, "TRAP RESCUE");
            classify(category3, "KILLER GRASP RESCUE");
//            classify(category3, "GOOD SKILL CHECK");
//            classify(category3, "GREAT SKILL CHECK");
            classify(category3, "HOOK RESCUE");
            classify(category3, "REUNITED");
            classify(category3, "PROTECTION");
//            classify(category3, "WAKE UP!");

            // BOLDNESS ---------------------------------------
            classify(category4, "BOLD");
            classify(category4, "CHASED");
            classify(category4, "ESCAPED");
            classify(category4, "KILLER STUN");
            classify(category4, "KILLER BLIND");
            classify(category4, "KILLER BURN");
            classify(category4, "HOOK SABOTAGE");
            classify(category4, "TRAP DISARM");
            classify(category4, "HAG TRAP DISARM");
            classify(category4, "TRAP SABOTAGE");
            classify(category4, "CLEANSED");
            classify(category4, "CLEANSING");
//            classify(category4, "GOOD SKILL CHECK");
//            classify(category4, "GREAT SKILL CHECK");
            classify(category4, "BASEMENT TIME");

//            System.out.println(this.category  + " (" +  this.accuracy*100 + "%) " + this.name  + " " +  this.points);
        }

        private void classify(String category, String testName) {
            double temp = similarity(this.name, testName);
            if (temp >= this.precision && temp > this.accuracy) {
                this.accuracy = temp;
                this.category = category;
            }
        }
    }

    class BloodPoints {

        int objective, survival, altruism, boldness, unknown;
        long lastTime;
        final double precision = 0.7;
        BloodPacket lastPack;

        public BloodPoints() {
            this.objective = 0;
            this.survival = 0;
            this.altruism = 0;
            this.boldness = 0;
            this.unknown = 0;
            this.lastPack = new BloodPacket("Unknown", -1);
            this.lastTime = -1;
        }

        public void add(BloodPacket pack) {
            if (lastTime == -1) {
                lastTime = System.currentTimeMillis();
            }
            long elapsedTime = System.currentTimeMillis() - lastTime;
            if    ((lastPack != null && elapsedTime < 3500
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
                log.setText(" (" + (int) Math.round(pack.accuracy * 100) + "%)" + log.getText());
            }
            log.setText(pack.category + " - " + pack.name + ": " + pack.points + log.getText());
            log.validate();
//            System.out.println(pack.category  + " (" +  pack.accuracy*100 + "%) " + pack.name  + " " +  pack.points);
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
