import javax.swing.*;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;                                                                                                                                          
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.xfeatures2d.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.Imgproc;                                                                                                       
import org.opencv.objdetect.CascadeClassifier;  


public class VideoPlayer implements ActionListener {
    final static int FrameNum = 9000;
    //final static int BufferSize = 600;
    final static int BufferSize = 3000;
    final static int ExtractRate = 10;
    final static double FrameRate = 30;
    final static int WIDTH = 480;
    final static int HEIGHT = 270;
    final static int START = 0, PAUSE = 1, STOP = 2;

    //OpenCV variables
    final MatOfFloat ranges = new MatOfFloat(0f, 256f);
    final MatOfInt histSize = new MatOfInt(129600);


    //int Stage_Reference = 0;
    ArrayList<Double> Stage_Reference = new ArrayList<>();
    ArrayList<Double> Jump_Reference = new ArrayList<>();
    ArrayList<String> logo_names = new ArrayList<>();
    ArrayList<Mat> logo_mat = new ArrayList<>();
    ArrayList<ArrayList<Integer>> frame_matches = new ArrayList<>();

    BufferedImage[] images;
    Mat[] reference_images;
    Mat[] matching_reference_images;
    File dir;
    File Videodir;
    File Adsdir;
    File logosdir;
    String mainvideo;
    String mainaudio;
    String path;
    String folder;
    int from;
    int curImages;
    int curFrame;
    JFrame frame;
    JLabel imageLabel;
    JButton openBtn;
    JPanel openPanel;
    JButton playBtn;
    JPanel playPanel;
    JButton pauseBtn;
    JPanel pausePanel;
    JButton stopBtn;
    JPanel stopPanel;
    JCheckBox adCheckbox;


    boolean SmartAdRemover=false;
    Thread videoThread;
    int videoState;
    AudioPlayer audioPlayer;
    Thread audioThread;

    public  VideoPlayer(){
        frame = new JFrame("VideoPlayer");
        frame.setPreferredSize(new Dimension(800, 500));
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);
        openBtn = new JButton("Open File");
        openBtn.addActionListener(this);
        openPanel = new JPanel();
        openPanel.add(openBtn);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        frame.add(openPanel, c);

        JPanel checkBoxPanel = new JPanel();
        adCheckbox = new JCheckBox("Smart Ad-Remover");
        checkBoxPanel.add(adCheckbox);
        c.gridx = 2;
        c.gridy = 0;
        frame.add(checkBoxPanel, c);

        playBtn = new JButton("Play");
        playBtn.addActionListener(this);
        playPanel = new JPanel();
        playPanel.add(playBtn);
        c.gridx = 0;
        c.gridy = 2;
        frame.add(playPanel, c);

        pauseBtn = new JButton("Pause");
        pauseBtn.addActionListener(this);
        pausePanel = new JPanel();
        pausePanel.add(pauseBtn);
        c.gridx = 1;
        c.gridy = 2;
        frame.add(pausePanel, c);

        stopBtn = new JButton("Stop");
        stopBtn.addActionListener(this);
        stopPanel = new JPanel();
        stopPanel.add(stopBtn);
        c.gridx = 2;
        c.gridy = 2;
        frame.add(stopPanel, c);

        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        c.weightx = 0.5;
        c.weighty = 0.5;
        c.gridx = 1;
        c.gridy = 1;
        frame.add(imageLabel, c);


        frame.pack();
        frame.setVisible(true);
    }

    //process the frame to find ads
    public long loadFrame(){
        long t = System.currentTimeMillis();
        //        System.out.println("Loading " + (k + 1) + "th frame.");
        File f = new File(this.mainvideo);
        File af = new File(this.mainaudio);
        byte[] bFile = new byte[WIDTH*HEIGHT*3];
        byte[] aFile = new byte[2*48000];
        try{
            FileInputStream is  = new FileInputStream(f);
            is.skip((long)WIDTH*HEIGHT*3*curFrame);
            is.read(bFile);
            is.close();

            FileInputStream as = new FileInputStream(af);
            as.skip(2*48000*curFrame + 44);
            as.read(aFile);
            as.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(images[curImages] == null)
            images[curImages] = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        if(curImages%ExtractRate == 0){
            if(reference_images[curImages/ExtractRate] == null)
                reference_images[curImages/ExtractRate] = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
            if(matching_reference_images[curImages/ExtractRate] == null)
                matching_reference_images[curImages/ExtractRate] = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
        }
        Mat ytemp = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
        Mat temp = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
        for(int h = 0, ind = 0; h < HEIGHT; h++){
            for(int w = 0; w < WIDTH; w++){
                byte r = bFile[ind];
                byte g = bFile[ind + HEIGHT * WIDTH];
                byte b = bFile[ind + HEIGHT * WIDTH * 2];
                int pixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                images[curImages].setRGB(w, h, pixel);

                if(curImages%ExtractRate == 0){
                    int R, G, B;
                    R = (int)r + 128;
                    G = (int)g + 128;
                    B = (int)b + 128;
                    int ycbcr[] = new int[3];
                    byte pixel_ycbcr[] = new byte[3];
                    ycbcr[0] = (int)(0.299*R+0.587*G+0.114*B);
                    ycbcr[1] = (int)(-0.1687*R-0.3313*G+0.5*B+128);
                    if (ycbcr[1] > 255) ycbcr[1] = 255;
                    ycbcr[2] = (int)(0.5*R-0.4187*G-0.0813*B+128);
                    if (ycbcr[2] > 255) ycbcr[2] = 255;
                    pixel_ycbcr[0] = (byte)(ycbcr[0]-128);
                    pixel_ycbcr[1] = (byte)(ycbcr[2]-128);
                    pixel_ycbcr[2] = (byte)(ycbcr[1]-128);
//                    temp.put(h, w, pixel_ycbcr);
                    reference_images[curImages/ExtractRate].put(h, w, pixel_ycbcr);
                    pixel_ycbcr[0] = b;
                    pixel_ycbcr[1] = g;
                    pixel_ycbcr[2] = r;
                    temp.put(h, w, pixel_ycbcr);
                    ytemp.put(h, w, pixel_ycbcr);
                }
                ind++;
            }
        }
        matching_reference_images[(curFrame/ExtractRate)%(BufferSize/ExtractRate)] = temp;
        if(curImages%ExtractRate == 0) {
            //System.out.println(curImages/ExtractRate + " : " +ByteBuffer.wrap(aFile).getDouble());
            reference_images[(curFrame / ExtractRate) % (BufferSize / ExtractRate)] = temp;
            //Imgproc.cvtColor(ytemp, reference_images[(curFrame / ExtractRate) % (BufferSize / ExtractRate)], Imgproc.COLOR_BGR2);
        }
        if(curImages%ExtractRate == 0 && SmartAdRemover)
            processFrame();

        curImages = (curImages + 1) % BufferSize;
        return System.currentTimeMillis() - t;
    }

    //find logos in the frame
    public void processFrame(){

        //Mat img1 = Imgcodecs.imread(logo, Imgcodecs.IMREAD_COLOR);
        Mat img2 = matching_reference_images[(curFrame/ExtractRate)%(BufferSize/ExtractRate)];
        //Imgproc.cvtColor(Imgcodecs.imread(logo, Imgcodecs.IMREAD_COLOR),img1,Imgproc.COLOR_BGR2RGB);

        // SIFT feature detection
        // create keypoints and descriptors for logo images and frames
        ArrayList<MatOfKeyPoint> logo_keypoints = new ArrayList<>();
        ArrayList<Mat> logo_descriptors = new ArrayList<>();
        ArrayList<List<MatOfDMatch>> logo_knnMatches = new ArrayList<>();

        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors2 = new Mat();

        //-- Step 1: Detect the keypoints using SURF Detector, compute the descriptors
        int hessianThreshold = 400;
        int nOctaves = 4, nOctaveLayers = 5;
        boolean extended = false, upright = false;
        //ORB detector = ORB.create();
        SIFT detector = SIFT.create(hessianThreshold, nOctaveLayers, 0.08);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        detector.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);
        //-- Step 2: Matching descriptor vectors with a FLANN based matcher
        // Since SURF is a floating-point descriptor NORM_L2 is used
        for(int i=0;i<logo_mat.size();i++) {
            logo_keypoints.add(new MatOfKeyPoint());
            logo_descriptors.add(new Mat());
            detector.detectAndCompute(logo_mat.get(i), new Mat(), logo_keypoints.get(i), logo_descriptors.get(i));
            logo_knnMatches.add(new ArrayList<>());
            if (!logo_descriptors.get(i).empty() &&  !descriptors2.empty())
                matcher.knnMatch(logo_descriptors.get(i), descriptors2, logo_knnMatches.get(i), 2);
        }

        //-- Filter matches using the Lowe's ratio test
        float ratioThresh = 0.65f;
        for(int j=0;j<logo_knnMatches.size();j++) {
            List<DMatch> listOfGoodMatches = new ArrayList<>();
            for (int i = 0; i < logo_knnMatches.get(j).size(); i++) {
                if (logo_knnMatches.get(j).get(i).rows() > 1) {
                    DMatch[] matches = logo_knnMatches.get(j).get(i).toArray();
                    if (matches[0].distance < ratioThresh * matches[1].distance) {
                        listOfGoodMatches.add(matches[0]);
                    }
                }
            }
            MatOfDMatch goodMatches = new MatOfDMatch();
            goodMatches.fromList(listOfGoodMatches);
            frame_matches.get(j).add(listOfGoodMatches.size());
//            //-- Draw matches
//            Mat imgMatches = new Mat();
//            Features2d.drawMatches(logo_mat.get(j), logo_keypoints.get(j), img2, keypoints2, goodMatches, imgMatches, Scalar.all(-1),
//                    Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
//            //-- Show detected matches
//
//            Imgcodecs.imwrite("img" + j +"_" +curFrame/ExtractRate+"_"+ listOfGoodMatches.size() +".png", imgMatches);
        }
    }

    // to play the outputted rgb file
    public long loadOutputFrame() {
        long t = System.currentTimeMillis();
        //        System.out.println("Loading " + (k + 1) + "th frame.");
        File f = new File("output.rgb");
        byte[] bFile = new byte[WIDTH * HEIGHT * 3];
        try {
            FileInputStream is = new FileInputStream(f);
            is.skip((long) WIDTH * HEIGHT * 3 * curFrame);
            is.read(bFile);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (images[curImages] == null)
            images[curImages] = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (int h = 0, ind = 0; h < HEIGHT; h++) {
            for (int w = 0; w < WIDTH; w++) {
                byte r = bFile[ind];
                byte g = bFile[ind + HEIGHT * WIDTH];
                byte b = bFile[ind + HEIGHT * WIDTH * 2];
                int pixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                images[curImages].setRGB(w, h, pixel);
                ind++;
            }
        }
        curImages = (curImages + 1) % BufferSize;
        return System.currentTimeMillis() - t;
    }

    public void loadVideo(){
        curFrame = from;
        curImages = 0;
        System.out.println("Loading frames...");
        if(images == null){
            images = new BufferedImage[BufferSize];
            reference_images = new Mat[BufferSize/ExtractRate];
            matching_reference_images = new Mat[BufferSize/ExtractRate];
        }
        for(int i = 0; i < BufferSize - 100; i++){
            loadOutputFrame();
            curFrame++;
        }
        System.out.println("Loaded successfully");
        imageLabel.setIcon(new ImageIcon(images[0]));
        imageLabel.revalidate();
        imageLabel.repaint();
        videoState = STOP;
    }

    public void loadAudio(){
        try {
            FileInputStream inputStream = new FileInputStream("output.wav");
            if (audioPlayer == null)
                audioPlayer = new AudioPlayer(inputStream, from / 48000 );
            else
                audioPlayer.setAudio(inputStream, from / 48000 );
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Loaded successfully");
    }

    public void loadFolder(String path, String folder, int from){
        videoState = STOP;
        this.dir = new File(path);
        this.path = path;
        this.folder = folder;
        this.from = from;
        this.Videodir = new File(dir.getPath() + "/Videos");
        this.Adsdir = new File(dir.getPath() + "/Ads");
        this.logosdir = new File(dir.getPath() + "/Brand Images");
        System.out.println(this.Videodir);

        // load the main rgb and wav file in Videos dir
        System.out.println("Loading rgb and wav" + "...");
        String filename = "";
        String audio_filename="";
        for (File file : Videodir.listFiles()) {
            if (file.getName().endsWith((".rgb")))
                filename = file.getPath();
            if (file.getName().endsWith((".wav")))
                audio_filename = file.getPath();
        }
        if(filename == "")
            System.out.println("rgb file not found");
        else
            this.mainvideo = filename;
        if(audio_filename == "")
            System.out.println("wav file not found");
        else
            this.mainaudio = audio_filename;

        if(adCheckbox.isSelected())
            this.SmartAdRemover = true;
        else
            this.SmartAdRemover = false;

        frame_matches.clear();
        logo_names.clear();
        logo_mat.clear();
        // load the logo files in Videos dir
        if(this.SmartAdRemover) {
            System.out.println("Loading logos" + "...");
            //load the logo rgb files
            ArrayList<String> logo_strings = new ArrayList<>();
            ArrayList<String> logo_name_temp = new ArrayList<>();
            for (File file : logosdir.listFiles()) {
                if (file.getName().endsWith((".rgb"))) {
                    logo_strings.add(file.getPath());
                    logo_name_temp.add(file.getName());
                }
            }
            if (logo_strings.isEmpty())
                System.out.println("Logos not found");
            else {
                for (int i = 0; i < logo_strings.size(); i++) {
                    File f = new File(logo_strings.get(i));
                    byte[] bFile = new byte[WIDTH*HEIGHT*3];
                    try{
                        FileInputStream is  = new FileInputStream(f);
                        is.read(bFile);
                        is.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    Mat temp = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
                    for(int h = 0, ind = 0; h < HEIGHT; h++){
                        for(int w = 0; w < WIDTH; w++) {
                            byte pixel[] = new byte[3];
                            pixel[0] = bFile[ind + HEIGHT * WIDTH * 2];
                            pixel[1] = bFile[ind + HEIGHT * WIDTH];
                            pixel[2] = bFile[ind];
                            temp.put(h, w, pixel);
                            ind++;
                        }
                    }
                    //matching_reference_images[(curFrame/ExtractRate)%(BufferSize/ExtractRate)] = temp;
                    this.logo_mat.add(temp);
                    this.logo_names.add(logo_name_temp.get(i).substring(0, logo_name_temp.get(i).indexOf("_")).toLowerCase());
                    this.frame_matches.add(new ArrayList<>());
                }
            }
        }
        for(int i=0;i<logo_names.size();i++)
            System.out.println(logo_names.get(i));
        //data();
        Stage_Reference.clear();
        Jump_Reference.clear();
        preprocessVideo();
        loadVideo();
        loadAudio();
    }

    public void loadFolder(int from){
        videoState = STOP;
        this.from = from;
        loadVideo();
        loadAudio();
    }

    public void playVideo(){
        videoState = START;
        videoThread = new Thread(){
            @Override
            public void run() {
                super.run();
                double t = 0;
                long pausetime=0; //time that the video has pause ()
                long startvideo=System.currentTimeMillis();
                for(int i = 0; i < FrameNum - from && videoState != STOP; i++){
                    long paintstart=System.currentTimeMillis();
                    long frame_offset = 0;
                    int supposed_frame = (int)(((System.currentTimeMillis() - startvideo - pausetime)/(double)1000)*FrameRate);
                    
                    // force to the supposed frame of this second
                    if((supposed_frame != i)) {
                        frame_offset = supposed_frame-i;
                        i = supposed_frame;
                    }

                    imageLabel.setIcon(new ImageIcon(images[i%BufferSize]));
                    imageLabel.revalidate();
                    imageLabel.repaint();
                    
                    for(int j=0;j<frame_offset + 1;j++) {
                        loadOutputFrame();
                        curFrame++;
                    }

                    //t += loadFrame(curFrame++);
                    long painttime = System.currentTimeMillis() - paintstart;
                    t+=painttime;
                    try {
                        //                        sleep(1000 / FrameRate - 3);
                        if(1000 / FrameRate - t >= 0){
                            TimeUnit.MICROSECONDS.sleep((long)(1000000 / FrameRate - t));
                            t = 0;
                        }
                        else {
                            //System.out.println("test");
                            t-= 1000 / FrameRate;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(videoState == PAUSE){
                        long pausestart=System.currentTimeMillis();
                        synchronized (this){
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        pausetime += System.currentTimeMillis() - pausestart;
                    }
                }
            }
        };
        videoThread.start();
    }

    public void playAudio(){
        audioThread = new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    audioPlayer.play();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        audioThread.start();
    }


    @Override
    public void actionPerformed(ActionEvent e){
        if(e.getSource() == this.openBtn){
            System.out.println("Open File clicked");
            JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //                path = file.getAbsolutePath();
                //                folder = file.getName();
                //                loadVideo(0);
                //                loadAudio(file.getAbsolutePath());
                loadFolder(file.getAbsolutePath(), file.getName(), 0);
            }
        }
        else if(e.getSource() == this.playBtn){
            System.out.println("Play clicked");
            if(images == null || audioPlayer == null){
                System.out.println("Please open a video file.");
                return;
            }
            if(videoState == START){
                return;
            }
            if(videoState == PAUSE){
                videoState = START;
                synchronized (videoThread){
                    videoThread.notify();
                }
            }else{
                playVideo();
            }
            playAudio();
        }
        else if(e.getSource() == this.pauseBtn){
            System.out.println("Pause clicked");
            videoState = PAUSE;
            if(audioPlayer == null){
                return;
            }
            audioPlayer.pause();
        }
        else if(e.getSource() == this.stopBtn){
            System.out.println("Stop clicked");
            videoState = STOP;
            loadFolder(0);
            if(audioPlayer == null){
                return;
            }
            audioPlayer.stop();
        }
    }

    //preprocess the entire video and remove ads from it
    public void preprocessVideo(){
        curFrame = from;
        curImages = 0;
        System.out.println("Loading frames...");
        if(images == null){
            images = new BufferedImage[BufferSize];
            reference_images = new Mat[BufferSize/ExtractRate];
            matching_reference_images = new Mat[BufferSize/ExtractRate];
        }

        // compare histograms and find the original ad locations
        Stage_Reference.add(0.0);
        for(int i = 0; i < FrameNum; i++) {
            loadFrame();
            if (curFrame > 0 && curFrame % ExtractRate == 0) {
                Mat y_previous = new Mat();
                Mat y_current = new Mat();

                Imgproc.calcHist(Arrays.asList(reference_images[(curFrame / ExtractRate - 1) % (BufferSize / ExtractRate)]), new MatOfInt(0), new Mat(), y_previous, histSize, ranges);
                Imgproc.calcHist(Arrays.asList(reference_images[(curFrame / ExtractRate) % (BufferSize / ExtractRate)]), new MatOfInt(0), new Mat(), y_current, histSize, ranges);
                double res = Imgproc.compareHist(y_previous, y_current, Imgproc.CV_COMP_CORREL);
                //System.out.println(res);
                // if the histogram is not nearly the same, we compare features within the frames as a second test
                if (res*100 < 70) {
                    MatOfKeyPoint keypoints = new MatOfKeyPoint(), keypoints2 = new MatOfKeyPoint();
                    Mat descriptors = new Mat(), descriptors2 = new Mat();
                    List<MatOfDMatch> knnMatches = new ArrayList<>();
                    //-- Step 1: Detect the keypoints using SURF Detector, compute the descriptors
                    int hessianThreshold = 400;
                    int nOctaves = 4, nOctaveLayers = 5;
                    boolean extended = false, upright = false;
                    //ORB detector = ORB.create();
                    //-- Step 2: Matching descriptor vectors with a FLANN based matcher
                    // Since SURF is a floating-point descriptor NORM_L2 is used
                    SIFT detector = SIFT.create(hessianThreshold, nOctaveLayers, 0.08);
                    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
                    detector.detectAndCompute(reference_images[(curFrame / ExtractRate - 1) % (BufferSize / ExtractRate)]
                            , new Mat(), keypoints2, descriptors2);
                    detector.detectAndCompute(reference_images[(curFrame / ExtractRate) % (BufferSize / ExtractRate)]
                            , new Mat(), keypoints, descriptors);
                    if (!descriptors.empty() &&  !descriptors2.empty())
                        matcher.knnMatch(descriptors, descriptors2, knnMatches, 2);

                    //-- Filter matches using the Lowe's ratio test
                    float ratioThresh = 0.65f;
                    List<DMatch> listOfGoodMatches = new ArrayList<>();
                    for (int k = 0; k < knnMatches.size(); k++) {
                        if (knnMatches.get(k).rows() > 1) {
                            DMatch[] matches = knnMatches.get(k).toArray();
                            if (matches[0].distance < ratioThresh * matches[1].distance) {
                                listOfGoodMatches.add(matches[0]);
                            }
                        }
                    }
//                    MatOfDMatch goodMatches = new MatOfDMatch();
//                    goodMatches.fromList(listOfGoodMatches);
//                    //-- Draw matches
//                    Mat imgMatches = new Mat();
//                    Features2d.drawMatches(reference_images[(curFrame / ExtractRate - 1) % (BufferSize / ExtractRate)]
//                            , keypoints, reference_images[(curFrame / ExtractRate) % (BufferSize / ExtractRate)],
//                            keypoints2, goodMatches, imgMatches, Scalar.all(-1),
//                            Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
//                    //-- Show detected matches
//
//                    Imgcodecs.imwrite("img_" +curFrame/ExtractRate+"_"+ listOfGoodMatches.size() +".png", imgMatches);
//
//                    DecimalFormat df = new DecimalFormat();
//                    df.setMaximumFractionDigits(2);
//                    System.out.println(df.format((curFrame / ExtractRate - 1)/ (FrameRate / ExtractRate)) +
//                            " - " + df.format((curFrame / ExtractRate)/ (FrameRate / ExtractRate)) + " " + "** " + res*100 + " **");
                    if(listOfGoodMatches.size()<10) {
                        Stage_Reference.add((curFrame / ExtractRate) / (FrameRate / ExtractRate));
                        System.out.println("potential ad frame change: " + (curFrame / ExtractRate) / (FrameRate / ExtractRate));
                    }
                }
            }
            curFrame++;
        }
        Stage_Reference.add(FrameNum/FrameRate);

        for(int i=0;i<Stage_Reference.size();i++){
            int j=0;
            boolean ad = false;
            while(Stage_Reference.get(i+j) < Stage_Reference.get(i) + 18){
                j++;
                ad = true;
                if(i+j == Stage_Reference.size())
                    break;
            }
            j--;
            if(Stage_Reference.get(i+j) < Stage_Reference.get(i) + 19 &&
                    Stage_Reference.get(i)+10 < Stage_Reference.get(i+j) && ad) {
                Jump_Reference.add(Stage_Reference.get(i));
                Jump_Reference.add(Stage_Reference.get(i+j));
            }
            i=i+j;
        }

        for(int i=0;i<Jump_Reference.size();i++)
            System.out.println("*** " + Jump_Reference.get(i));
        System.out.println("rewriting video file, please wait");
        //Find ad files in directory
        File f = new File(this.mainvideo);
        File af = new File(this.mainaudio);
        ArrayList<String> temp = new ArrayList<>();
        ArrayList<String> temp2 = new ArrayList<>();

        //all three of the ad information arrays below should be in the same order
        ArrayList<String> ad_strings = new ArrayList<>();
        ArrayList<File> ads = new ArrayList<>(); //will switch up array order based on logo detection
        ArrayList<File> ads_audio = new ArrayList<>(); //will switch up array order based on logo detection

        //switch up the order of the logo_names array to match when the logos appear in the video
        ArrayList<Integer> logo_location = new ArrayList<>();
        for(int i=0;i<frame_matches.size();i++){
            int high_output_amount = 0;
            int high_output_pos = 450;
            for(int j=0;j<frame_matches.get(i).size()-5;j++){
                int sum = 0;
                boolean high_output=true, firstchance=true;
                for(int k=0;k<5;k++) {
                    sum += frame_matches.get(i).get(j+k);
                    if(frame_matches.get(i).get(j+k) < 8){
                        if(firstchance)
                            firstchance=false;
                        else
                            high_output = false;
                    }
                }
                if(high_output && high_output_amount < sum) {
                    high_output_pos = j;
                    high_output_amount = sum;
                }
            }
            logo_location.add(high_output_pos);
        }
        for(int i=0;i<logo_location.size() - 1;i++){ //rearrange logo_names based on logo_location
            for (int j = 0; j < logo_location.size() - i - 1; j++)
            {
                if (logo_location.get(j) > logo_location.get(j+1))
                {
                    // swap arr[j] and arr[j+1]
                    int temp_num = logo_location.get(j);
                    logo_location.set(j, logo_location.get(j + 1));
                    logo_location.set(j + 1, temp_num);
                    String temp_str = logo_names.get(j);
                    logo_names.set(j, logo_names.get(j + 1));
                    logo_names.set(j + 1, temp_str);
                }
            }
        }
        System.out.println("Logo approximate seconds");
        for(int i=0;i<logo_names.size();i++)
            System.out.println(i +": "+logo_names.get(i) + " - " + logo_location.get(i)/ (FrameRate / ExtractRate) + " sec");


        //set up the logos to their corresponding ads
        for (File file : Adsdir.listFiles()) {
            if (file.getName().endsWith((".rgb"))) {
                temp.add(file.getPath());
                temp2.add(file.getName());
            }
        }
        for(int j=0;j<logo_names.size();j++) {
            for(int i=0;i<temp.size();i++){
                if (temp2.get(i).substring(0, temp2.get(i).indexOf("_")).toLowerCase().equals(logo_names.get(j)))
                    ad_strings.add(temp.get(i));
            }
        }

        // put the ads in the order according to the array "ad_strings"
        if(ad_strings.isEmpty())
            System.out.println("Ads not found or Smart Ad-Replacer is off");
        else{
            for(int i=0;i<ad_strings.size();i++) {
                ads.add(new File(ad_strings.get(i)));
                ads_audio.add(new File(ad_strings.get(i).substring(0, ad_strings.get(i).length() - 3) + "wav"));
            }
        }

        // Rewrite an output rgb file with the ads replaced
        byte[] bFile = new byte[WIDTH*HEIGHT*3];
        int output_curFrame = 0;
        int jump_reference_iter = 0;
        try{
            //creating buffers needed for writing rgb file
            FileInputStream is  = new FileInputStream(f);
            ArrayList<FileInputStream> ad_filestream = new ArrayList<>();
            FileOutputStream fos = new FileOutputStream("output.rgb");
            for(int i=0;i<ads.size();i++)
                ad_filestream.add(new FileInputStream(ads.get(i)));
            DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos));
            boolean ad_written = false;
            while(is.read(bFile) != -1){
                if(jump_reference_iter<Jump_Reference.size()) {
                    if (Jump_Reference.get(jump_reference_iter) * 30 - 10 <= output_curFrame &&
                            output_curFrame <= (Jump_Reference.get(jump_reference_iter + 1) * 30) + 5){
                        if(SmartAdRemover && !ad_written) {
                            while(ad_filestream.get(jump_reference_iter / 2).read(bFile) != -1)
                                outStream.write(bFile);
                            ad_written=true;
                        }
                    } else {
                        outStream.write(bFile);
                    }
                    if ((Jump_Reference.get(jump_reference_iter + 1) * 30 + 5<= output_curFrame)) {
                        jump_reference_iter+=2;
                        ad_written=false;
                    }
                }
                else
                    outStream.write(bFile);
                output_curFrame++;

            }
            outStream.close();
            is.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        //rewrite an audio file with the ad music in place
        byte[] aFile = new byte[2*48000 / 30];
        byte[] thr = new byte[44];
        int output_cursec = 0;
        jump_reference_iter = 0;
        try{
            //creating buffers needed for writing wav file
            FileInputStream is  = new FileInputStream(af);
            ArrayList<FileInputStream> ad_filestream = new ArrayList<>();
            FileOutputStream fos = new FileOutputStream("output.wav");
            for(int i=0;i<ad_strings.size();i++)
                ad_filestream.add(new FileInputStream(ads_audio.get(i)));
            DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos));
            //header file of wav
            is.read(thr);
            outStream.write(thr);
            boolean ad_written = false;
            //write to wav file
            while(is.read(aFile) != -1){
                if(jump_reference_iter < Jump_Reference.size()) {
                    if (Jump_Reference.get(jump_reference_iter) * 30 - 10<= output_cursec &&
                            output_cursec < Jump_Reference.get(jump_reference_iter + 1) * 30 + 5) {
                        if(SmartAdRemover && !ad_written) {
                            ad_filestream.get(jump_reference_iter / 2).read(thr);
                            for(int i=0;i<15 * 30;i++) {
                                ad_filestream.get(jump_reference_iter / 2).read(aFile);
                                outStream.write(aFile);
                            }
//                            while(ad_filestream.get(jump_reference_iter / 2).read(aFile) != -1)
//                                outStream.write(aFile);
                            ad_written=true;
                        }
                    } else {
                        outStream.write(aFile);
                    }
                    if (Jump_Reference.get(jump_reference_iter + 1) * 30 + 5 <= output_cursec) {
                        jump_reference_iter += 2;
                        ad_written = false;
                    }
                }
                else
                    outStream.write(aFile);

                output_cursec++;
            }
            outStream.close();
            is.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        //Load opencv
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); 
        new VideoPlayer();

    }
}
