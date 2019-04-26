import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;                                                                                                                                          
import org.opencv.core.Core;                                                                                                             
import org.opencv.core.CvType;                                                                                                           
import org.opencv.core.Mat;                                                                                                              
import org.opencv.core.MatOfByte;                                                                                                        
import org.opencv.core.MatOfFloat;                                                                                                       
import org.opencv.core.MatOfInt;                                                                                                         
import org.opencv.core.MatOfPoint;                                                                                                       
import org.opencv.core.MatOfRect;                                                                                                        
import org.opencv.core.Rect;                                                                                                             
import org.opencv.core.Scalar;                                                                                                           
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
    
    int Stage_Reference = 0;

    Map<Integer, List<HyperLink>> matadataMap;
    BufferedImage[] images;
    Mat[] reference_images;
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
    Thread loadFrameThread;
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
        imageLabel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Mouse clicked at (" + e.getX() + ", " + e.getY() + ")");
                //                System.out.println(curFrame - BufferSize + 100);
                if(matadataMap.containsKey(curFrame - BufferSize + 100)){
                    List<HyperLink> list = matadataMap.get(curFrame - BufferSize + 100);
                    for(HyperLink hl: list){
                        Point p1 = hl.getP1();
                        Point p2 = hl.getP2();
                        if(e.getX() >= p1.getX() && e.getX() <= p2.getX() && e.getY() >= p1.getY() && e.getY() <= p2.getY()){
                            loadFolder(hl.getPath(), hl.getFolder(), hl.getRelatedFrame());

                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        c.weightx = 0.5;
        c.weighty = 0.5;
        c.gridx = 1;
        c.gridy = 1;
        frame.add(imageLabel, c);


        frame.pack();
        frame.setVisible(true);
    }

    public void loadMetadata(){
        matadataMap = new HashMap<>();
        String filename =  path + "/" + "metadata.txt";
        FileReader fr = null;
        BufferedReader br = null;
        try{
            fr = new FileReader(filename);
            br = new BufferedReader(fr);
            String str;
            while((str = br.readLine()) != null){
                if(!str.startsWith("Name")){
                    String[] s = str.split(",");
                    matadataMap.putIfAbsent(Integer.valueOf(s[0]), new ArrayList<>());
                    List<HyperLink> list = matadataMap.get(Integer.valueOf(s[0]));
                    int x1 = Integer.valueOf(s[1]);
                    int y1 = Integer.valueOf(s[2]);
                    int x2 = Integer.valueOf(s[3]);
                    int y2 = Integer.valueOf(s[4]);
                    HyperLink hl = new HyperLink(x1, y1, x2, y2, s[5], Integer.valueOf(s[6]), Color.RED);
                    list.add(hl);

                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }


    }

    public long loadFrame(){
        long t = System.currentTimeMillis();
        String filename = path + "/" + folder + ".rgb";
        //        System.out.println("Loading " + (k + 1) + "th frame.");
        File f = new File(filename);
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
                reference_images[curImages/ExtractRate] = new Mat(270, 480, CvType.CV_8UC3); 
            }    
        }

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
                    pixel_ycbcr[1] = (byte)(ycbcr[1]-128);
                    pixel_ycbcr[2] = (byte)(ycbcr[2]-128);

                    reference_images[curImages/ExtractRate].put(h, w, pixel_ycbcr);  
                }

                ind++;
            }
        }
        //old code
        if(false){
            Graphics2D g2d = images[curImages].createGraphics();
            List<HyperLink> list = matadataMap.get(0);
            for(int i = 0; i < list.size(); i++){
                HyperLink hl = list.get(i);
                g2d.setColor(hl.getColor());
                Point p1 = hl.getP1();
                Point p2 = hl.getP2();
                g2d.drawRect(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
            }
            g2d.dispose();
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
        }
        for(int i = 0; i < BufferSize - 100; i++){
            //            String filename = path + "/" + folder + String.format("%04d", i+1) + ".rgb";
            //            File f = new File(filename);
            //            byte[] bFile = new byte[(int) f.length()];
            //            try{
            //                FileInputStream is  = new FileInputStream(f);
            //                is.read(bFile);
            //                is.close();
            //            }catch (Exception e){
            //                e.printStackTrace();
            //            }
            //            if(images[i] == null)
            //                images[i] = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            //            for(int h = 0, ind = 0; h < HEIGHT; h++){
            //                for(int w = 0; w < WIDTH; w++){
            //                    byte r = bFile[ind];
            //                    byte g = bFile[ind + HEIGHT * WIDTH];
            //                    byte b = bFile[ind + HEIGHT * WIDTH * 2];
            //                    int pixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
            //                    images[i].setRGB(w, h, pixel);
            //                    ind++;
            //                }
            //            }
            loadFrame();

            if(curFrame > 0 && curFrame%ExtractRate == 0){

                MatOfFloat ranges = new MatOfFloat(0f, 256f);
                MatOfInt histSize = new MatOfInt(129600);

                Mat y_previous = new Mat(); 
                Mat y_current = new Mat(); 

                Imgproc.calcHist(Arrays.asList(reference_images[curFrame/ExtractRate-1]), new MatOfInt(0), new Mat(), y_previous, histSize, ranges);
                Imgproc.calcHist(Arrays.asList(reference_images[curFrame/ExtractRate]), new MatOfInt(0), new Mat(), y_current, histSize, ranges);
                double res = Imgproc.compareHist(y_previous, y_current, Imgproc.CV_COMP_CORREL);
                Double d = new Double(res * 100);  
                if(d < 60){            
                    //System.out.println((curFrame/ExtractRate-1) + " - " + curFrame/ExtractRate + " " + "** " + d + " **");   
                    if(curFrame/ExtractRate - Stage_Reference == FrameRate/ExtractRate*15){
                        System.out.println("Ads is from " + Stage_Reference/(FrameRate/ExtractRate) + " sec to " + (curFrame/ExtractRate)/(FrameRate/ExtractRate) + " sec");
                    }
                    Stage_Reference = curFrame/ExtractRate;
                }
            }

            curFrame++;
        }
        System.out.println("Loaded successfully");
        imageLabel.setIcon(new ImageIcon(images[0]));
        imageLabel.revalidate();
        imageLabel.repaint();
        videoState = STOP;
    }

    public void loadAudio(){
        System.out.println("Loading " + folder + ".wav" + "...");
        String filename = path + "/" + folder + ".wav";
        try {
            FileInputStream inputStream = new FileInputStream(filename);
            if (audioPlayer == null)
                audioPlayer = new AudioPlayer(inputStream, from / 30 * 48000 );
            else
                audioPlayer.setAudio(inputStream, from / 30 * 48000 );
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Loaded successfully");
    }

    public void loadFolder(String path, String folder, int from){
        videoState = STOP;
        this.path = path;
        this.folder = folder;
        this.from = from;
        //data();
        loadVideo();
        loadAudio();
    }

    public void loadFolder(int from){
        videoState = STOP;
        this.from = from;
        //loadMetadata();
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
                    
                    if((supposed_frame != i)) {
                        frame_offset = supposed_frame-i;
                        i = supposed_frame;
                    }

                    imageLabel.setIcon(new ImageIcon(images[i%BufferSize]));
                    imageLabel.revalidate();
                    imageLabel.repaint();
                    
                    for(int j=0;j<frame_offset + 1;j++) {
                    
                        loadFrame();

                        if(curFrame > 0 && curFrame%ExtractRate == 0){

                            MatOfFloat ranges = new MatOfFloat(0f, 256f);
                            MatOfInt histSize = new MatOfInt(129600);

                            Mat y_previous = new Mat(); 
                            Mat y_current = new Mat(); 

                            Imgproc.calcHist(Arrays.asList(reference_images[(curFrame/ExtractRate-1)%(BufferSize/ExtractRate)]), new MatOfInt(0), new Mat(), y_previous, histSize, ranges);
                            Imgproc.calcHist(Arrays.asList(reference_images[(curFrame/ExtractRate)%(BufferSize/ExtractRate)]), new MatOfInt(0), new Mat(), y_current, histSize, ranges);
                            double res = Imgproc.compareHist(y_previous, y_current, Imgproc.CV_COMP_CORREL);
                            Double d = new Double(res * 100);  
                            if(d < 60){            
                                //System.out.println((curFrame/ExtractRate-1) + " - " + curFrame/ExtractRate + " " + "** " + d + " **");   
                                if(curFrame/ExtractRate - Stage_Reference == FrameRate/ExtractRate*15){
                                    System.out.println("Ads is from " + Stage_Reference/(FrameRate/ExtractRate) + " sec to " + (curFrame/ExtractRate)/(FrameRate/ExtractRate) + " sec");
                                }
                                Stage_Reference = curFrame/ExtractRate;
                            }
                        }

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
                            System.out.println("test");
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
            JFileChooser fc = new JFileChooser();
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


    public static void main(String[] args){
        //Load opencv
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); 
        new VideoPlayer();

    }
}
