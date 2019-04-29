import javax.swing.*;
import java.awt.*;
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


    int Stage_Reference = 0;
    ArrayList<Integer> Jump_Reference = new ArrayList<>();

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

    Thread videoThread;
    int videoState;
    AudioPlayer audioPlayer;
    Thread audioThread;

    public  VideoPlayer(){
        frame = new JFrame("VideoPlayer");
        frame.setPreferredSize(new Dimension(500, 400));
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
        byte[] bFile = new byte[WIDTH*HEIGHT*3];
        try{
            FileInputStream is  = new FileInputStream(f);
            is.skip((long)WIDTH*HEIGHT*3*curFrame);
            is.read(bFile);
            is.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        if(images[curImages] == null){
            images[curImages] = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        }

        if(curImages%ExtractRate == 0){
            if(reference_images[curImages/ExtractRate] == null){
                reference_images[curImages/ExtractRate] = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
            }
            if(matching_reference_images[curImages/ExtractRate] == null){
                matching_reference_images[curImages/ExtractRate] = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
            }
        }
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

                }
                ind++;
            }
        }
        matching_reference_images[(curFrame/ExtractRate)%(BufferSize/ExtractRate)] = temp;
        //Imgproc.cvtColor(temp,matching_reference_images[(curFrame/ExtractRate)%(BufferSize/ExtractRate)],Imgproc.COLOR_BGR2GRAY);
        //if(curImages%ExtractRate == 0)
            //processFrame();

        curImages = (curImages + 1) % BufferSize;
        return System.currentTimeMillis() - t;
    }

    //find logos in the frame
    public void processFrame(){
        String logo = "../dataset2/Brand Images/Mcdonalds_logo.bmp";

        Mat img1 = new Mat();
        //Mat img2 = new Mat(HEIGHT,WIDTH,CvType.CV_8UC3);
        Mat img2 = matching_reference_images[(curFrame/ExtractRate)%(BufferSize/ExtractRate)];
        img1 = Imgcodecs.imread(logo, Imgcodecs.IMREAD_COLOR);
        //Imgproc.cvtColor(Imgcodecs.imread(logo, Imgcodecs.IMREAD_COLOR),img1,Imgproc.COLOR_BGR2RGB);
        //-- Step 1: Detect the keypoints using SURF Detector, compute the descriptors
        int hessianThreshold = 400;
        int nOctaves = 4, nOctaveLayers = 5;
        boolean extended = false, upright = false;
        SIFT detector = SIFT.create(hessianThreshold, nOctaveLayers);
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint(), keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat(), descriptors2 = new Mat();
        detector.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
        detector.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);
        //-- Step 2: Matching descriptor vectors with a FLANN based matcher
        // Since SURF is a floating-point descriptor NORM_L2 is used
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptors1, descriptors2, knnMatches, 2);
        //-- Filter matches using the Lowe's ratio test
        float ratioThresh = 0.65f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);
        //-- Draw matches
        Mat imgMatches = new Mat();
        Features2d.drawMatches(img1, keypoints1, img2, keypoints2, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
        //-- Show detected matches
        Imgcodecs.imwrite("img_"+curFrame/ExtractRate+"_"+ listOfGoodMatches.size() +".png", imgMatches);
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

        if (images[curImages] == null) {
            images[curImages] = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        }

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
        System.out.println("Loading rgb and wav" + "...");
        String filename = "";
        String audio_filename="";
        for (File file : Videodir.listFiles()) {
            if (file.getName().endsWith((".rgb"))) {
                filename = file.getPath();
            }
            if (file.getName().endsWith((".wav"))) {
                audio_filename = file.getPath();
            }
        }
        if(filename == "")
            System.out.println("rgb file not found");
        else
            this.mainvideo = filename;
        if(audio_filename == "")
            System.out.println("wav file not found");
        else
            this.mainaudio = audio_filename;
        //data();
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
                    
                    //System.out.println(supposed_frame);
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
        Stage_Reference = 0; // in the case that the ad starts from the start
        curFrame = from;
        curImages = 0;
        System.out.println("Loading frames...");
        if(images == null){
            images = new BufferedImage[BufferSize];
            reference_images = new Mat[BufferSize/ExtractRate];
            matching_reference_images = new Mat[BufferSize/ExtractRate];
        }

        // compare histograms and find the original ad locations
        for(int i = 0; i < FrameNum; i++) {
            loadFrame();
            if (curFrame > 0 && curFrame % ExtractRate == 0) {
                Mat y_previous = new Mat();
                Mat y_current = new Mat();

                Imgproc.calcHist(Arrays.asList(reference_images[(curFrame / ExtractRate - 1) % (BufferSize / ExtractRate)]), new MatOfInt(0), new Mat(), y_previous, histSize, ranges);
                Imgproc.calcHist(Arrays.asList(reference_images[(curFrame / ExtractRate) % (BufferSize / ExtractRate)]), new MatOfInt(0), new Mat(), y_current, histSize, ranges);
                double res = Imgproc.compareHist(y_previous, y_current, Imgproc.CV_COMP_CORREL);
                Double d = new Double(res * 100);
                if (d < 60) {
                    System.out.println((curFrame / ExtractRate - 1) + " - " + curFrame / ExtractRate + " " + "** " + d + " **");
                    if (curFrame / ExtractRate - Stage_Reference == FrameRate / ExtractRate * 15) {
                        System.out.println("Ads is from " + Stage_Reference / (FrameRate / ExtractRate) + " sec to " + (curFrame / ExtractRate) / (FrameRate / ExtractRate) + " sec");
                        Jump_Reference.add((int) (Stage_Reference / (FrameRate / ExtractRate)));
                    }
                    Stage_Reference = curFrame / ExtractRate;
                }
            }

            curFrame++;
        }

        for(int i=0;i<Jump_Reference.size();i++)
            System.out.println("*** " + Jump_Reference.get(i));
        System.out.println("rewriting video file, please wait");
        //Find ad files in directory
        File f = new File(this.mainvideo);
        File af = new File(this.mainaudio);
        ArrayList<String> ad_strings = new ArrayList<>();
        ArrayList<File> ads = new ArrayList<>();
        ArrayList<File> ads_audio = new ArrayList<>();

        for (File file : Adsdir.listFiles()) {
            if (file.getName().endsWith((".rgb"))) {
                ad_strings.add(file.getPath());
            }
        }
        if(ad_strings.isEmpty())
            System.out.println("Ads not found");
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
            while(is.read(bFile) != -1){
                if(jump_reference_iter<Jump_Reference.size()) {
                    if (Jump_Reference.get(jump_reference_iter) * 30 <= output_curFrame &&
                            output_curFrame < (Jump_Reference.get(jump_reference_iter) + 15) * 30) {
                        ad_filestream.get(jump_reference_iter).read(bFile);
                        outStream.write(bFile);
                    } else {
                        outStream.write(bFile);
                    }
                    if ((Jump_Reference.get(jump_reference_iter) + 15) * 30 <= output_curFrame) {
                        ad_filestream.get(jump_reference_iter).close();
                        jump_reference_iter++;
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
        byte[] aFile = new byte[2 * 48000];
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
            //write to wav file
            while(is.read(aFile) != -1){
                if(jump_reference_iter < Jump_Reference.size()) {
                    if (Jump_Reference.get(jump_reference_iter) <= output_cursec &&
                            output_cursec < Jump_Reference.get(jump_reference_iter) + 15) {
                        ad_filestream.get(jump_reference_iter).read(aFile);
                        outStream.write(aFile);
                    } else {
                        outStream.write(aFile);
                    }
                    if (Jump_Reference.get(jump_reference_iter) + 15 <= output_cursec){
                        ad_filestream.get(jump_reference_iter).close();
                        jump_reference_iter++;
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
