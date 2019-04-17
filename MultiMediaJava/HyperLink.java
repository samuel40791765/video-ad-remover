import java.awt.*;

public class HyperLink {
    private Point p1;
    private Point p2;
    private String path;
    private String folder;
    private int relatedFrame;
    private Color color;

    public HyperLink(int x1, int y1, int x2, int y2, String path, int rf, Color c){
        p1 = new Point(x1, y1);
        p2 = new Point(x2, y2);
        this.path = path;
        String[] s = path.split("/");
        this.folder = s[s.length - 1];
        relatedFrame = rf;
        color = c;
    }

    public Point getP1(){
        return p1;
    }

    public Point getP2(){
        return p2;
    }

    public String getPath(){
        return path;
    }

    public String getFolder(){
        return folder;
    }

    public int getRelatedFrame(){
        return relatedFrame;
    }

    public Color getColor(){
        return color;
    }
}
