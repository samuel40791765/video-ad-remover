import sun.jvm.hotspot.ui.JavaStackTracePanel;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.nio.file.*;
import java.nio.Buffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.*;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import javafx.scene.text.*;
import java.awt.Image;
import javax.swing.ImageIcon;
import java.io.FileWriter;
import java.io.StringWriter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class VideoPlayer1 implements ActionListener, ChangeListener, ListSelectionListener, MouseListener {
    static final int Width = 352;
    static final int Height = 288;

    private BufferedImage[] images;
    private ArrayList<Metadata> metalist;

    private JFrame frame;
    private JPanel leftpanel;
    private JPanel rightpanel;
    private JPanel downpanel;
    private JPanel down_panel;
    private JLabel leftLabel;
    private JLabel rightLabel;
    private JLabel textLabel;
    private JPanel panel;
    private JScrollPane scrollPane;
    private static int slidervalue = 1;
    private static int rightslidervalue = 1;
    private static String rgbstring = "0001";
    private static String right_rgbstring = "0001";

    private String Left_Folder = "";
    private String Left_Filename = "";
    private String Right_Folder = "";
    private String Right_Filename = "";

    private JButton ImportFirst;
    private JButton ImportSecondary;
    private JButton Createlink;
    private JButton Create;
    private JButton ConnectVideo;
    private JButton SaveFile;
    private JButton SetEndFrame;
    private JSlider slider;
    private JSlider rightslider;
    private String metastring;
    private JList<String> textList;
    private HashMap<String, Integer> map;
    private DefaultListModel<String> model;
    private boolean canclick;
    private int pressx, pressy = 0;
    private int releasex, releasey = 0;
    private BufferedImage previousimage = null;
    private int previousx, previousy = 0;
    BufferedImage image2;


    public VideoPlayer1(){
        images = new BufferedImage[9000];
        leftLabel = new JLabel();
        leftpanel = new JPanel();
        downpanel = new JPanel();
        down_panel = new JPanel();
        textLabel = new JLabel();
        rightLabel = new JLabel();
        rightpanel = new JPanel();
        panel = new JPanel();
        frame = new JFrame();
        slider = new JSlider(JSlider.HORIZONTAL, 0, 9000, 0);
        rightslider = new JSlider(JSlider.HORIZONTAL, 0, 9000, 0);
        metalist = new ArrayList<>();

        slider.addChangeListener(this);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        rightslider.addChangeListener(this);
        rightslider.setPaintLabels(true);
        rightslider.setPaintTicks(true);
        leftLabel.addMouseListener(this);
        model = new DefaultListModel<>();
        textList = new JList<String>(model);
        textList.addListSelectionListener(this);
        textList.addMouseListener(this);

        canclick = false;
        metastring = "";
    }

    public static void main(String[] args) {
        VideoPlayer1 videoPlayer = new VideoPlayer1();
        videoPlayer.SetDefaultFrame();
    }

    public void SetDefaultFrame(){
        panel.setPreferredSize(new Dimension(724, 200));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel label = new JLabel("Action:");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(label, c);

        ImportFirst = new JButton("Import Primary Video");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(ImportFirst, c);
        ImportFirst.addActionListener(this);

        ImportSecondary = new JButton("Import Secondary Video");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 1;
        panel.add(ImportSecondary, c);
        ImportSecondary.addActionListener(this);

        Createlink = new JButton("Create new Hyperlink");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 2;
        panel.add(Createlink, c);
        Createlink.addActionListener(this);

        SetEndFrame = new JButton("Set End Frame");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 3;
        panel.add(SetEndFrame, c);
        SetEndFrame.addActionListener(this);

        JLabel label2 = new JLabel("Select Link:");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 0;
        panel.add(label2, c);

        scrollPane = new JScrollPane(textList);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 3;
        c.gridy = 0;
        c.ipady = 80;
        c.gridheight = 4;
        c.gridwidth = 3;
        c.weightx = 0.1;
        panel.add(scrollPane, c);

        ConnectVideo = new JButton("Connect Video");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 0;
        c.ipadx = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 6;
        c.gridy = 1;
        panel.add(ConnectVideo, c);
        ConnectVideo.addActionListener(this);

        SaveFile = new JButton("Save File");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 6;
        c.gridy = 2;
        panel.add(SaveFile, c);
        SaveFile.addActionListener(this);

        panel.setBackground(Color.gray);

        frame.getContentPane().add(panel, BorderLayout.PAGE_START);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(724, 582);
        frame.getContentPane().setBackground(Color.gray);
        frame.setVisible(true);
    }

    public void UpdateRightImage(String rgbstring, String fileFolder, String filename, int slidervalue) {
        try {
            String fullName = fileFolder + "/" + filename + rgbstring + ".rgb";
            File file = new File(fullName);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int)len];
            int offset = 0;
            int numRead = 0;

            while(offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

//            System.out.println("Start loading frame: " + fullName);
            int index = 0;
            BufferedImage image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
            for(int y=0; y<Height; y++) {
                for(int x=0; x<Width; x++) {
                    byte r = bytes[index];
                    byte g = bytes[index+Height*Width];
                    byte b = bytes[index+Height*Width*2];
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    image.setRGB(x, y, pix);
                    index++;
                }
            }

            ImageIcon iconx = new ImageIcon(image);
            rightLabel.setIcon(iconx);
            rightLabel.setText("Frame" + slidervalue);
            rightpanel.revalidate();
            rightpanel.repaint();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void UpdateImage(String rgbstring, String fileFolder, String filename, int slidervalue) {
        if(images[slidervalue-1] == null) {
            try {
                String fullName = fileFolder + "/" + filename + rgbstring + ".rgb";
                File file = new File(fullName);
                InputStream is = new FileInputStream(file);


                long len = file.length();
                byte[] bytes = new byte[(int) len];
                int offset = 0;
                int numRead = 0;

                while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                    offset += numRead;
                }
                is.close();
//                System.out.println("Start loading frame: " + fullName);
                int index = 0;
                BufferedImage image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < Height; y++) {
                    for (int x = 0; x < Width; x++) {
                        byte r = bytes[index];
                        byte g = bytes[index + Height * Width];
                        byte b = bytes[index + Height * Width * 2];
                        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                        image.setRGB(x, y, pix);
                        index++;
                    }
                }

                images[slidervalue - 1] = image;
                ImageIcon iconx = new ImageIcon(images[slidervalue - 1]);
                leftLabel.setIcon(iconx);
                leftLabel.setText("Frame" + slidervalue);
                leftpanel.revalidate();
                leftpanel.repaint();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            BufferedImage image = images[slidervalue-1];
            ImageIcon iconx = new ImageIcon(image);
            leftLabel.setIcon(iconx);
            leftLabel.setText("Frame" + slidervalue);
            leftpanel.revalidate();
            leftpanel.repaint();
        }
    }

    public void LoadRightFrame(String fileFolder, String filename){
        try{

            String fullName = fileFolder + "/" + filename + right_rgbstring + ".rgb";
            File file = new File(fullName);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int)len];
            int offset = 0;
            int numRead = 0;

            while(offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            System.out.println("Start loading frame: " + fullName);
            int index = 0;
            BufferedImage image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
            for(int y=0; y<Height; y++) {
                for(int x=0; x<Width; x++) {
                    byte r = bytes[index];
                    byte g = bytes[index+Height*Width];
                    byte b = bytes[index+Height*Width*2];
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    image.setRGB(x, y, pix);
                    index++;
                }
            }
            is.close();
//            System.out.println("End loading frame: " + fullName);
            rightLabel.setIcon(new ImageIcon(image));
            rightLabel.setText("Frame" + rightslidervalue);
            rightLabel.setForeground(Color.white);
            rightLabel.setHorizontalTextPosition(JLabel.CENTER);
            rightLabel.setVerticalTextPosition(JLabel.BOTTOM);
            rightpanel.setLayout(new BorderLayout());
            down_panel.setPreferredSize(new Dimension(350, 50));
            rightslider.setPreferredSize(new Dimension(350, 50));
            down_panel.add(rightslider);
            rightpanel.add(rightLabel, BorderLayout.CENTER);
            rightpanel.add(down_panel, BorderLayout.SOUTH);
            rightpanel.setPreferredSize(new Dimension(350, 380));
            rightpanel.setBackground(Color.black);
            frame.getContentPane().add(rightpanel);
            frame.getContentPane().add(rightpanel, BorderLayout.EAST);
            frame.revalidate();
            frame.repaint();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LoadLeftFrame(String fileFolder, String filename){
        try{
            String fullName = fileFolder + "/" + filename + rgbstring + ".rgb";
            File file = new File(fullName);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int)len];
            int offset = 0;
            int numRead = 0;

            while(offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            System.out.println("Start loading frame: " + fullName);
            int index = 0;
            BufferedImage image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
            for(int y=0; y<Height; y++) {
                for(int x=0; x<Width; x++) {
                    byte r = bytes[index];
                    byte g = bytes[index+Height*Width];
                    byte b = bytes[index+Height*Width*2];
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    image.setRGB(x, y, pix);
                    index++;
                }
            }
            images[slidervalue-1] = image;
            is.close();
            leftLabel.setIcon(new ImageIcon(images[slidervalue-1]));
            leftLabel.setText("Frame" + slidervalue);
            leftLabel.setForeground(Color.white);
            leftLabel.setHorizontalTextPosition(JLabel.CENTER);
            leftLabel.setVerticalTextPosition(JLabel.BOTTOM);
            leftpanel.setLayout(new BorderLayout());
            downpanel.setPreferredSize(new Dimension(350, 50));
            slider.setPreferredSize(new Dimension(350, 50));
            downpanel.add(slider);
            leftpanel.setPreferredSize(new Dimension(350, 380));
            leftpanel.add(leftLabel, BorderLayout.LINE_START);
            leftpanel.add(downpanel, BorderLayout.SOUTH);
            leftpanel.setBackground(Color.black);

            frame.getContentPane().add(leftpanel);
            frame.getContentPane().add(leftpanel, BorderLayout.WEST);
            frame.revalidate();
            frame.repaint();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void PopoutDialog(BufferedImage image){
        canclick = false;
        JFrame frame = new JFrame("InputDialog Example #1");
        String name = JOptionPane.showInputDialog(frame, "What's Object Name? After Entered, Please Select End Frame.");

        if(name == null) {
            ImageIcon iconx = new ImageIcon(images[slidervalue - 1]);
            leftLabel.setIcon(iconx);
            leftLabel.setText("Frame" + slidervalue);
            leftpanel.revalidate();
            leftpanel.repaint();
            return;
        }
        images[slidervalue - 1] = image;

        int[] recentagle = new int[4];
        recentagle[0] = pressx;
        recentagle[1] = pressy;
        recentagle[2] = releasex;
        recentagle[3] = releasey;

        Metadata metadata = new Metadata(name, slidervalue, recentagle);
        metalist.add(metadata);
        model.addElement(name);
        textList.setSelectedIndex(textList.getModel().getSize()-1);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == this.ImportFirst) {
            if(metalist == null)
                metalist = new ArrayList<Metadata>();
            else{
                metalist.clear();
            }
            JFileChooser fc = new JFileChooser("/Users/taihsunchen/IdeaProjects/CSCI576_Final_Project");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                File f = fc.getSelectedFile();
                Left_Folder = f.getAbsolutePath();
                Left_Filename = f.getName();
                images = new BufferedImage[9000];
                LoadLeftFrame(Left_Folder, Left_Filename);
                slider.setValue(1);
                model.removeAllElements();
            }
        }
        else if(e.getSource() == this.ImportSecondary) {
            JFileChooser fc = new JFileChooser("/Users/taihsunchen/IdeaProjects/CSCI576_Final_Project");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                File f = fc.getSelectedFile();
                Right_Folder = f.getAbsolutePath();
                Right_Filename = f.getName();
                LoadRightFrame(Right_Folder, Right_Filename);
                rightslider.setValue(1);
            }
        }

        else if(e.getSource() == this.SetEndFrame) {
            int index = 0;
            if(textList.getSelectedIndex() == -1) {
                index = metalist.size()-1;
            }
            else {
                index = textList.getSelectedIndex();
            }
            //Metadata current_matadata = metalist.get(metalist.size()-1);
            System.out.println(index + " end_farme: " + slidervalue);
            Metadata current_matadata = metalist.get(index);
            current_matadata.SetEnd(slidervalue);
            //metalist.set(metalist.size()-1, current_matadata);
            metalist.set(index, current_matadata);
            JFrame frame = new JFrame("InputDialog Example #2");
            JOptionPane.showMessageDialog(frame, "Saved Last file successfully");
        }

        else if(e.getSource() == this.Createlink) {
            if(!canclick) {
                canclick =  !canclick;
            }
        }

        else if(e.getSource() == this.ConnectVideo) {
            if(textList.getSelectedValue() != null) {
                String selected = textList.getSelectedValue();
                for(int i=0; i<metalist.size(); i++) {
                    if(selected.equals(metalist.get(i).name)) {
                        Metadata currentmetadata = metalist.get(i);
                        currentmetadata.Connect(Right_Folder, rightslidervalue);
                        metalist.set(i, currentmetadata);
                    }
                }
            }
            else {
                Metadata currentmetadata = metalist.get(metalist.size()-1);
                currentmetadata.Connect(Right_Folder, rightslidervalue);
                metalist.set(metalist.size()-1, currentmetadata);
            }
        }

        else if(e.getSource() == this.SaveFile) {
            metastring = CalculateMetaData(metalist);
            FileWriter fw = null;
            BufferedWriter bw = null;
            String file = Left_Folder + "/metadata.txt";

            File deletefile = new File(file);
            try {
                if(deletefile.delete()) {
                    System.out.println(deletefile.getName() + "is deleted");
                } else {
                    System.out.println("Delete failed");
                }
            } catch(Exception e2) {
                e2.printStackTrace();
            }

            try{
                fw = new FileWriter(file);
                bw = new BufferedWriter(fw);
                bw.write(metastring);
                bw.flush();
                bw.close();
                fw.close();
            } catch(IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public String CalculateMetaData(ArrayList<Metadata> metalist) {
        StringBuffer sb = new StringBuffer();
        int size = metalist.size();
        int[] resultrecent = new int[4];

        for(int i=0; i<size; i++) {
            Metadata current_metadata = metalist.get(i);
            int start = current_metadata.start_frame;
            int end = current_metadata.end_frame;
            int[] recentagle = current_metadata.recentagle;
            String end_video = current_metadata.folder_name;
            int end_frame = current_metadata.relative_frame;

            sb.append("Name: " + current_metadata.name + "\n");
            sb.append(current_metadata.toString());


            try {
                String fullName = Left_Folder + "/" + Left_Filename + String.format("%04d", start) + ".rgb";
                File file = new File(fullName);
                InputStream is = new FileInputStream(file);

                long len = file.length();
                byte[] bytes = new byte[(int) len];
                int offset = 0;
                int numRead = 0;

                while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                    offset += numRead;
                }
                is.close();
                previousimage = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);

                int index = 0;
                for (int y = 0; y < Height; y++) {
                    for (int x = 0; x < Width; x++) {
                        byte r = bytes[index];
                        byte g = bytes[index + Height * Width];
                        byte b = bytes[index + Height * Width * 2];
                        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                        previousimage.setRGB(x, y, pix);
                        index++;
                    }
                }
            } catch(FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            previousx = recentagle[0];
            previousy = recentagle[1];
            image2 = null;
            for(int j=start+1; j<=end; j++) {
                int[] compare = new int[4];
                resultrecent = CalculateRecentagele(recentagle, Left_Folder, Left_Filename, j);
                recentagle = resultrecent;
                Metadata newmetadata = new Metadata(j, resultrecent, end_video, end_frame);
                sb.append(newmetadata.toString());
            }
        }

        return sb.toString();
    }

    public int[] CalculateRecentagele(int[] recentagle, String folder, String filename, int currentframe) {
        int x1 = recentagle[0];
        int y1 = recentagle[1];
        int x2 = recentagle[2];
        int y2 = recentagle[3];

        int distancex = x2-x1;
        int distancey = y2-y1;

        int[] result = new int[4];
        if(image2 == null){
            image2 = previousimage;
        }
        BufferedImage image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);


        try {
            String fullName = folder + "/" + filename + String.format("%04d", currentframe) + ".rgb";
            File file = new File(fullName);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int) len];
            int offset = 0;
            int numRead = 0;

            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            is.close();
            System.out.println("Start loading frame: " + fullName);
            int index = 0;
            for (int y = 0; y < Height; y++) {
                for (int x = 0; x < Width; x++) {
                    byte r = bytes[index];
                    byte g = bytes[index + Height * Width];
                    byte b = bytes[index + Height * Width * 2];
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    image.setRGB(x, y, pix);
                    index++;
                }
            }
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Double min = Double.MAX_VALUE;
        int minx = 0;
        int miny = 0;

        for(int k=-8; k<=8; k++) {
            for(int k2 = -8; k2 <= 8; k2++) {
                int currenty = y1 + k;
                int currentx = x1 + k2;

                if(currenty < 0 || currenty >= Height || currentx < 0 || currentx >= Width) {
                    continue;
                }

                double total = 0.0;
                for(int y = currenty, prevy = y1; y<= y2; y++, prevy++) {
                    for(int x = currentx, prevx = x1; x <= x2; x++, prevx++) {
                        if(y >= Height || prevy >= Height) break;
                        if(x >= Width || prevx >= Width) continue;

                        int rgb1 = image.getRGB(x, y);
                        int rgb2 = image2.getRGB(prevx, prevy);
                        int rgb3 = previousimage.getRGB(previousx, previousy);
                        total += 0.8*Distance(rgb1, rgb2);
                        total += 0.2*Distance(rgb1, rgb3);
                    }
                }

                if(Math.min(min, total) == total) {
                    min = total;
                    minx = currentx;
                    miny = currenty;
                }
            }
        }

        result[0] = minx;
        result[1] = miny;
        result[2] = minx+distancex;
        result[3] = miny+distancey;
        image2 = image;
        return result;
    }

    public double Distance(int rgb1, int rgb2) {
        Color c1 = new Color(rgb1);
        Color c2 = new Color(rgb2);
        return Math.pow(c1.getRed()-c2.getRed(), 2) + Math.pow(c1.getBlue()-c2.getBlue(), 2) + Math.pow(c1.getGreen()-c2.getGreen(), 2);
    }


    @Override
    public void stateChanged(ChangeEvent e) {
        if(e.getSource() == this.slider){
            slidervalue = slider.getValue();
            if(slidervalue == 0) slidervalue += 1;
            rgbstring = String.format("%04d", slidervalue);
            UpdateImage(rgbstring, Left_Folder, Left_Filename, slidervalue);
        }
        if(e.getSource() == this.rightslider){
            rightslidervalue = rightslider.getValue();
            if(rightslidervalue == 0) rightslidervalue += 1;
            right_rgbstring = String.format("%04d", rightslidervalue);
            UpdateRightImage(right_rgbstring, Right_Folder, Right_Filename, rightslidervalue);
        }
    }
//
    @Override
    public void valueChanged(ListSelectionEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getSource() == this.textList) {
            String selected = textList.getSelectedValue();
            for(int i=0; i<metalist.size() && selected != null; i++) {
                if(selected.equals(metalist.get(i).name)) {
                    int shouldtransform = metalist.get(i).start_frame;
                    slider.setValue(shouldtransform);
                    UpdateImage(rgbstring, Left_Folder, Left_Filename, shouldtransform);
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(e.getSource() == this.leftLabel) {
            if(canclick) {
                pressx = e.getX();
                pressy = e.getY();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(e.getSource() == this.leftLabel) {
            if (canclick) {
                releasex = e.getX();
                releasey = e.getY();

                BufferedImage image = null;

                if (images[slidervalue - 1] == null) {
                    try {
                        String fullName = Left_Folder + "/" + Left_Filename + rgbstring + ".rgb";
                        File file = new File(fullName);
                        InputStream is = new FileInputStream(file);

                        long len = file.length();
                        byte[] bytes = new byte[(int) len];
                        int offset = 0;
                        int numRead = 0;

                        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                            offset += numRead;
                        }
                        is.close();
                        int index = 0;
                        image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
                        for (int y = 0; y < Height; y++) {
                            for (int x = 0; x < Width; x++) {
                                byte r = bytes[index];
                                byte g = bytes[index + Height * Width];
                                byte b = bytes[index + Height * Width * 2];
                                int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                                image.setRGB(x, y, pix);
                                index++;
                            }
                        }
                        images[slidervalue - 1] = image;
                        image = new BufferedImage(images[slidervalue - 1].getColorModel(), images[slidervalue - 1].copyData(null), images[slidervalue - 1].isAlphaPremultiplied(), null);
                        Graphics2D g2d = image.createGraphics();
                        g2d.setColor(Color.RED);
                        g2d.drawRect(pressx, pressy, releasex - pressx, releasey - pressy);
                        g2d.dispose();
//                    images[slidervalue - 1] = image;
                        ImageIcon iconx = new ImageIcon(image);
                        leftLabel.setIcon(iconx);
                        leftLabel.setText("Frame" + slidervalue);
                        leftpanel.revalidate();
                        leftpanel.repaint();

                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    image = new BufferedImage(images[slidervalue - 1].getColorModel(), images[slidervalue - 1].copyData(null), images[slidervalue - 1].isAlphaPremultiplied(), null);
                    Graphics2D g2d = image.createGraphics();
                    g2d.setColor(Color.RED);
                    g2d.drawRect(pressx, pressy, releasex - pressx, releasey - pressy);
                    g2d.dispose();
                    //images[slidervalue - 1] = image;
                    ImageIcon iconx = new ImageIcon(image);
                    leftLabel.setIcon(iconx);
                    leftLabel.setText("Frame" + slidervalue);
                    leftpanel.revalidate();
                    leftpanel.repaint();
                }

                PopoutDialog(image);
            }
        }
        else{

        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}


