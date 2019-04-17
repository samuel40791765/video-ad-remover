public class Metadata {
    public String name;
    public int start_frame;
    public int end_frame;
    public String folder_name;
    public int[] recentagle;
    public int relative_frame;

    public Metadata(String name, int start_frame, int[] recentagle) {
        this.name = name;
        this.start_frame = start_frame;
        this.recentagle = recentagle;
    }

    public Metadata(int start_frame, int[] recentagle, String folder_name, int relative_frame) {
        this.start_frame = start_frame;
        this.recentagle = recentagle;
        this.folder_name = folder_name;
        this.relative_frame = relative_frame;
    }
    public void SetEnd(int end_frame) {
        this.end_frame = end_frame;
    }

    public void Connect(String folder_name, int frame) {
        this.folder_name = folder_name;
        this.relative_frame = frame;
    }





    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(start_frame);
        sb.append(",");
        sb.append(recentagle[0] + "," + recentagle[1] + "," + recentagle[2] + "," + recentagle[3]);
        sb.append(",");
        sb.append(folder_name);
        sb.append(",");
        sb.append(relative_frame);
        sb.append("\n");
        return sb.toString();
    }
}
