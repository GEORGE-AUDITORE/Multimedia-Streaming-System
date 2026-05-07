package server;

public class VideoFile {
    private String movieName;
    private String resolution;
    private String format;
    private String fullFileName;

    public VideoFile(String movieName, String resolution, String format, String fullFileName){
        this.movieName = movieName;
        this.resolution = resolution;
        this.format = format;
        this.fullFileName = fullFileName;
    }

    public String getMovieName(){
        return movieName;
    }

    public String getResolution(){
        return resolution;
    }

    public String getFormat(){
        return format;
    }

    public String getFullFileName(){
        return fullFileName;
    }

    @Override
    public String toString(){
        return "VideoFile{" +
        "movieName = '" + movieName + '\'' +
        ", resolution = '" + resolution + '\'' +
        ", format = '" + format + '\'' + 
        ", fullFileName = '" + fullFileName + '\'' +
        '}'; 
    }
    
}
