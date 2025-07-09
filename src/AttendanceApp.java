import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;

class Utils {
    public BufferedImage toBufferedImage(Mat matrix) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (matrix.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
        byte[] buffer = new byte[bufferSize];
        matrix.get(0, 0, buffer); // Get all pixels
        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        return image;
    }
}

public class AttendanceApp {
    static {
        System.load("D:/jdk-24.0.1/bin/opencv/build/java/x64/opencv_java460.dll");
    }

    private JFrame frame;
    private JLabel imageLabel;
    static HashMap<String, Boolean> attendanceMap;
    private VideoCapture capture;
    private boolean isCameraRunning = false;
    private CascadeClassifier faceDetector;
    private Map<String, Mat> referenceImages;
    private Mat currentFrame;

    // Database credentials
    public static final String dbURL = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres";
    public static final String dbUsername = "postgres.wrwnqmgputrgyvqhlqex";
    public static final String dbPassword = "Dinesh@2004";

    public static void main(String[] args) {
        AttendanceApp app = new AttendanceApp();
        app.showLoginScreen();
    }

    private void showLoginScreen() {
        JFrame loginFrame = new JFrame("Faculty Login");
        loginFrame.setSize(400, 500); // Increased size to accommodate new fields and title
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set background color
        loginFrame.getContentPane().setBackground(new Color(138, 43, 226)); // Violet shade

        loginFrame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Title label for the interface
        JLabel titleLabel = new JLabel("USER INTERFACE", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(75, 0, 130)); // Deep blue background for title
        titleLabel.setPreferredSize(new Dimension(400, 40)); // Set size for title label

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Title spans across the entire width
        loginFrame.add(titleLabel, gbc);

        // Create panel for username and password fields with violet border and titled
        // border "Login"
        JPanel loginPanel = new JPanel();
        loginPanel.setBackground(new Color(238, 130, 238)); // Violet color for login section
        loginPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(75, 0, 130), 2), "Login",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), Color.WHITE)); // Deep blue border with title "Login"
        loginPanel.setLayout(new GridLayout(2, 2, 10, 10)); // Grid layout for 2x2

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        loginPanel.add(usernameLabel);

        JTextField usernameField = new JTextField(15);
        loginPanel.add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        loginPanel.add(passwordLabel);

        JPasswordField passwordField = new JPasswordField(15);
        loginPanel.add(passwordField);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        loginFrame.add(loginPanel, gbc);

        // Create panel for adding new faculty with deep blue border and titled border
        // "Add Faculty"
        JPanel facultyPanel = new JPanel();
        facultyPanel.setBackground(new Color(72, 61, 139)); // Deep blue color for faculty section
        facultyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(138, 43, 226), 2), "Add Faculty",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), Color.WHITE)); // Violet border with title "Add Faculty"
        facultyPanel.setLayout(new GridLayout(3, 2, 10, 10)); // Grid layout for 3x2

        JLabel nameLabel = new JLabel("Faculty Name:");
        nameLabel.setForeground(Color.WHITE);
        facultyPanel.add(nameLabel);

        JTextField nameField = new JTextField(15);
        facultyPanel.add(nameField);

        JLabel newUsernameLabel = new JLabel("New Username:");
        newUsernameLabel.setForeground(Color.WHITE);
        facultyPanel.add(newUsernameLabel);

        JTextField newUsernameField = new JTextField(15);
        facultyPanel.add(newUsernameField);

        JLabel newPasswordLabel = new JLabel("New Password:");
        newPasswordLabel.setForeground(Color.WHITE);
        facultyPanel.add(newPasswordLabel);

        JPasswordField newPasswordField = new JPasswordField(15);
        facultyPanel.add(newPasswordField);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        loginFrame.add(facultyPanel, gbc);

        // Buttons for login and adding faculty
        JButton loginButton = new JButton("Login");
        loginButton.setBackground(new Color(75, 0, 130)); // Indigo
        loginButton.setForeground(Color.WHITE);
        gbc.gridx = 1;
        gbc.gridy = 3;
        loginFrame.add(loginButton, gbc);

        JButton addFacultyButton = new JButton("Add Faculty");
        addFacultyButton.setBackground(new Color(0, 255, 127)); // Medium Sea Green
        addFacultyButton.setForeground(Color.WHITE);
        gbc.gridx = 1;
        gbc.gridy = 4;
        loginFrame.add(addFacultyButton, gbc);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (authenticateUser(username, password)) {
                loginFrame.dispose();
                initGUI();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid credentials. Please try again.");
            }
        });

        addFacultyButton.addActionListener(e -> {
            String facultyName = nameField.getText();
            String newUsername = newUsernameField.getText();
            String newPassword = new String(newPasswordField.getPassword());

            if (addFacultyToDatabase(facultyName, newUsername, newPassword)) {
                JOptionPane.showMessageDialog(loginFrame, "Faculty added successfully!");
                nameField.setText("");
                newUsernameField.setText("");
                newPasswordField.setText("");
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Failed to add faculty.");
            }
        });

        loginFrame.setVisible(true);
    }

    private boolean addFacultyToDatabase(String facultyName, String username, String password) {
        try (Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword)) {
            String query = "INSERT INTO faculty (username, password) VALUES ( ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // If the insert was successful, return true
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database connection failed!");
        }
        return false;
    }

    private boolean authenticateUser(String username, String password) {
        return username.equals("admin") && password.equals("admin");

    }

    private JLabel titleLabel; // Declare a JLabel for the title

    private void initGUI() {
        frame = new JFrame("Face Detection and Attendance");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Set background color
        frame.getContentPane().setBackground(new Color(186, 85, 211)); // Orchid shade

        // Title Label
        titleLabel = new JLabel("Face Detection and Attendance", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE); // Set title color
        frame.add(titleLabel, BorderLayout.NORTH); // Add the title at the top

        imageLabel = new JLabel();
        frame.add(imageLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(147, 112, 219)); // Medium Purple

        JButton cameraButton = new JButton("Start Camera");
        cameraButton.setBackground(new Color(75, 0, 130)); // Indigo
        cameraButton.setForeground(Color.WHITE);
        cameraButton.addActionListener(e -> startCamera());
        buttonPanel.add(cameraButton);

        JButton stopCameraButton = new JButton("Stop Camera");
        stopCameraButton.setBackground(new Color(138, 43, 226)); // Violet
        stopCameraButton.setForeground(Color.WHITE);
        stopCameraButton.addActionListener(e -> stopCamera());
        buttonPanel.add(stopCameraButton);

        JButton detectFacesButton = new JButton("Detect Faces");
        detectFacesButton.setBackground(new Color(199, 21, 133)); // Medium Violet Red
        detectFacesButton.setForeground(Color.WHITE);
        detectFacesButton.addActionListener(e -> detectFaces());
        buttonPanel.add(detectFacesButton);

        JButton showReportButton = new JButton("Show Attendance Report");
        showReportButton.setBackground(new Color(218, 112, 214)); // Orchid
        showReportButton.setForeground(Color.WHITE);
        showReportButton.addActionListener(e -> displayAttendanceReport());
        buttonPanel.add(showReportButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        attendanceMap = new HashMap<>();
        referenceImages = new HashMap<>();
        faceDetector = new CascadeClassifier("lib/haarcascade_frontalface_default.xml"); // Path to Haar Cascade
        if (faceDetector.empty()) {
            JOptionPane.showMessageDialog(frame, "Error loading face detector!");
            System.exit(1);
        }
        preloadReferenceImages("images/"); // Change to your folder path
    }

    private void preloadReferenceImages(String folderPath) {
        File folder = new File(folderPath);
        for (File file : folder.listFiles()) {
            if (file.isFile() && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
                Mat image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                Imgproc.resize(image, image, new Size(100, 100)); // Resize for uniformity
                String name = file.getName().replaceFirst("[.][^.]+$", ""); // Extract name without extension
                referenceImages.put(name, image);
                attendanceMap.put(name, false); // Initialize as absent
            }
        }
    }

    private void startCamera() {
        if (isCameraRunning) {
            JOptionPane.showMessageDialog(frame, "Camera is already running");
            return;
        }

        capture = new VideoCapture(Videoio.CAP_DSHOW + 0);
        if (!capture.isOpened()) {
            JOptionPane.showMessageDialog(frame, "Could not open webcam");
            return;
        }

        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);

        isCameraRunning = true;

        // Update title to reflect camera status
        titleLabel.setText("Camera Running - Face Detection and Attendance");

        new Thread(() -> {
            currentFrame = new Mat();
            Utils imageProcessor = new Utils();

            while (isCameraRunning) {
                capture.read(currentFrame);
                if (!currentFrame.empty()) {
                    Image tempImage = imageProcessor.toBufferedImage(currentFrame);
                    ImageIcon imageIcon = new ImageIcon(tempImage, "Captured Video");
                    imageLabel.setIcon(imageIcon);
                    frame.pack();
                }
            }

            capture.release();
        }).start();
    }

    private void stopCamera() {
        if (!isCameraRunning) {
            JOptionPane.showMessageDialog(frame, "Camera is not running");
            return;
        }

        isCameraRunning = false;
        JOptionPane.showMessageDialog(frame, "Camera has been stopped");

        // Update title to reflect camera status
        titleLabel.setText("Camera Stopped - Face Detection and Attendance");
    }

    private void detectFaces() {
        if (currentFrame == null || currentFrame.empty()) {
            JOptionPane.showMessageDialog(frame, "No frame available for detection");
            return;
        }

        Mat processedFrame = currentFrame.clone();
        processFrame(processedFrame); // Call processFrame to handle face detection and attendance update

        // Update title to reflect current action
        titleLabel.setText("Detecting Faces - Face Detection and Attendance");

        // Display the processed frame on the GUI
        Utils imageProcessor = new Utils();
        Image tempImage = imageProcessor.toBufferedImage(processedFrame);
        imageLabel.setIcon(new ImageIcon(tempImage, "Processed Frame"));
        frame.repaint();
    }

    private void displayAttendanceReport() {
        JFrame reportFrame = new JFrame("Attendance Report");
        reportFrame.setSize(400, 300);

        // Set background color
        reportFrame.getContentPane().setBackground(new Color(138, 43, 226)); // Violet

        DefaultTableModel model = new DefaultTableModel(new String[] { "Name", "Attendance" }, 0);
        attendanceMap.forEach((name, present) -> model.addRow(new Object[] { name, present ? "Present" : "Absent" }));

        JTable table = new JTable(model);
        table.setBackground(new Color(230, 230, 250)); // Lavender
        table.setForeground(new Color(75, 0, 130)); // Indigo
        table.setFont(new Font("Arial", Font.BOLD, 14));

        reportFrame.add(new JScrollPane(table));
        reportFrame.setVisible(true);

        // Update title when displaying the attendance report
        titleLabel.setText("Attendance Report - Face Detection and Attendance");
    }

    private void processFrame(Mat frame) {
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame); // Enhance contrast

        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(grayFrame, faces);

        for (Rect rect : faces.toArray()) {
            // Extract the face ROI (region of interest)
            Mat faceROI = grayFrame.submat(rect);
            Imgproc.resize(faceROI, faceROI, new Size(100, 100)); // Resize for matching

            // Recognize the face by comparing with reference images
            String recognizedName = recognizeFace(faceROI);

            if (recognizedName != null && !attendanceMap.getOrDefault(recognizedName, false)) {
                // Mark attendance
                attendanceMap.put(recognizedName, true);

                // Draw a green rectangle around the detected face
                Imgproc.rectangle(frame,
                        new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0), 2);

                // Add the recognized name as a label above the rectangle
                Imgproc.putText(frame, recognizedName,
                        new Point(rect.x, rect.y - 10), // Above the rectangle
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.75, // Font scale
                        new Scalar(255, 255, 255), // White text
                        2); // Thickness
            }
        }

        if (faces.empty()) {
            System.out.println("No faces detected in the current frame.");
        }
    }

    private String recognizeFace(Mat face) {
        for (Map.Entry<String, Mat> entry : referenceImages.entrySet()) {
            String name = entry.getKey();
            Mat referenceFace = entry.getValue();

            Mat result = new Mat();
            Imgproc.matchTemplate(face, referenceFace, result, Imgproc.TM_CCOEFF_NORMED);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            double score = mmr.maxVal;

            System.out.println("Comparing with " + name + ", Score: " + score);

            if (score < 0) { // Adjust threshold if necessary
                System.out.println("Match found: " + name);
                return name;
            }
        }
        System.out.println("No match found for the detected face.");
        return null;
    }
}
