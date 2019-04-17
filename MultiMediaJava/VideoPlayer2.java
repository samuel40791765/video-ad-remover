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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoPlayer2 implements ActionListener {
    final static int FrameNum = 9000;
    final static int BufferSize = 300;
    final static int FrameRate = 30;
    final static int WIDTH = 352;
    final static int HEIGHT = 288;
    final static int START = 0, PAUSE = 1, STOP = 2;
    Map<Integer, List<HyperLink>> matadataMap;
    BufferedImage[] images;
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

    Thread loadVideoThread;
    Thread videoThread;
    int videoState;
    AudioPlayer audioPlayer;
    Thread audioThread;
    public  VideoPlayer2(){
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
                System.out.println(curFrame);
                if(matadataMap.containsKey(curFrame)){
                    List<HyperLink> list = matadataMap.get(curFrame);
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

    public void loadFrame(int k){
        String filename = path + "/" + folder + String.format("%04d", k+1) + ".rgb";
//        System.out.println("Loading " + (k + 1) + "th frame.");
        File f = new File(filename);
        byte[] bFile = new byte[(int) f.length()];
        try{
            FileInputStream is  = new FileInputStream(f);
            is.read(bFile);
            is.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(images[k] == null)
            images[k] = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for(int h = 0, ind = 0; h < HEIGHT; h++){
            for(int w = 0; w < WIDTH; w++){
                byte r = bFile[ind];
                byte g = bFile[ind + HEIGHT * WIDTH];
                byte b = bFile[ind + HEIGHT * WIDTH * 2];
                int pixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                images[k].setRGB(w, h, pixel);
                ind++;
            }
        }
        if(matadataMap.containsKey(k)){
            Graphics2D g2d = images[k].createGraphics();
            List<HyperLink> list = matadataMap.get(k);
            for(int i = 0; i < list.size(); i++){
                HyperLink hl = list.get(i);
                g2d.setColor(hl.getColor());
                Point p1 = hl.getP1();
                Point p2 = hl.getP2();
                g2d.drawRect(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
            }
            g2d.dispose();
        }
    }


    public void loadVideo(){
        System.out.println("Loading frames...");
        if(images == null)
            images = new BufferedImage[FrameNum];
        loadVideoThread = new Thread(){
            @Override
            public void run() {
                super.run();
                int tmp = curImages;
                int tmpFrom = from;
                if(tmp + tmpFrom < 9000){
                    for(int i = 0; i < tmpFrom; i++){
                        loadFrame(i % FrameNum);
                        curImages++;
                    }
                }
                int tmp2 = curImages;
                for(int i = 0; i < FrameNum - tmp2; i++){
                    loadFrame((i + tmpFrom + tmp) % FrameNum);
                    curImages++;
//                    System.out.println("Loading " + (i + from + 1) + " frame.");
                }
            }
        };
        loadVideoThread.start();
        try {
            Thread.sleep(500);
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Loaded successfully");
    }

    public void loadAudio(){
        System.out.println("Loading " + folder + ".wav" + "...");
        String filename = path + "/" + folder + ".wav";
        try {
            FileInputStream inputStream = new FileInputStream(filename);
            if (audioPlayer == null)
                audioPlayer = new AudioPlayer(inputStream, from / 30 * 44100 );
            else
                audioPlayer.setAudio(inputStream, from / 30 * 44100 );
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
        loadMetadata();
        curImages = 0;
        loadVideo();
        curFrame = from;
        imageLabel.setIcon(new ImageIcon(images[curFrame]));
        imageLabel.revalidate();
        imageLabel.repaint();
        loadAudio();
    }

    public void loadFolder(int from){
        videoState = STOP;
        if(curImages != 9000){
            loadVideo();
        }
        this.from = from;
        curFrame = from;
        imageLabel.setIcon(new ImageIcon(images[curFrame]));
        imageLabel.revalidate();
        imageLabel.repaint();
        audioPlayer.setPos(from);
    }

    public void playVideo(){
        videoState = START;
        videoThread = new Thread(){
            @Override
            public void run() {
                super.run();
                long t = 0;
                for(int i = from; i < FrameNum && videoState != STOP; i++){
//                    t = System.currentTimeMillis();
                    imageLabel.setIcon(new ImageIcon(images[i]));
                    imageLabel.revalidate();
                    imageLabel.repaint();
                    curFrame++;
//                    System.out.println(System.currentTimeMillis() - t);
                    try {
                        sleep(1000 / FrameRate);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(videoState == PAUSE){
                        synchronized (this){
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
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
            JFileChooser fc = new JFileChooser("/Users/taihsunchen/IdeaProjects/CSCI576_Final_Project");
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
            loadVideoThread.interrupt();
            loadFolder(0);
            if(audioPlayer == null){
                return;
            }
            audioPlayer.stop();
        }
    }


    public static void main(String[] args){

        new VideoPlayer2();

    }
}
