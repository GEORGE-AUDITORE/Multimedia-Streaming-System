package server;

public class ConversionTask {
    private VideoFile sourceVideo;
    private MissingVideoVersion targetVersion;

    public ConversionTask(VideoFile sourceVideo, MissingVideoVersion targetVersion){
        this.sourceVideo = sourceVideo;
        this.targetVersion = targetVersion;
    }

    public VideoFile getSourceVideo(){
        return sourceVideo;
    }

    public MissingVideoVersion getTargetVersion(){
        return targetVersion;
    }

    @Override
    public String toString(){
        return "ConversionTask{" +
        "sourceVideo=" + sourceVideo.getFullFileName() +
        ", targetVersion=" + targetVersion.getExpectedFileName() +
        '}';
    }
}
