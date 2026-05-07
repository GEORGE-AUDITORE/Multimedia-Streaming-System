package server;

public class MissingVideoVersion {
    private String movieName;
    private String resolution;
    private String format;
    private String expectedFileName;

    public MissingVideoVersion(String movieName, String resolution, String format){
        this.movieName = movieName;
        this.resolution = resolution;
        this.format = format;
        this.expectedFileName = movieName + "-" + resolution + "." + format;
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

    public String getExpectedFileName(){
        return expectedFileName;
    }

    @Override
    public String toString(){
        return "MissingVideoVersion{" +
        "movieName='" + movieName + '\'' +
            ", resolution='" + resolution + '\'' +
            ", format='" + format + '\'' +
            ", expectedFileName='" + expectedFileName + '\'' +
            '}';
    }
    
}
