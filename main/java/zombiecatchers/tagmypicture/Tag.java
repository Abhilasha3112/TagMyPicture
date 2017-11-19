package zombiecatchers.tagmypicture;

/**
 * Created by Allen on 22/01/2017.
 */

public class Tag {
    private long tagId;
    public long getId() {
        return tagId;
    }
    public void setId(long id) {
        this.tagId = id;
    }

    private String tagName;
    public String gettagName() {
        return tagName;
    }
    public void settagName(String id) {
        this.tagName = id;
    }

    private String coverPic;
    public String getCoverPic() {
        return coverPic;
    }
    public void setCoverPic(String coverPic) {
        this.coverPic = coverPic;
    }

    private int countOfPics;
    public int getCountOfPics(){ return countOfPics; }
    public void setCountOfPics(int countOfPics){ this.countOfPics=countOfPics; }

    public void settag(long tagId,String tagName) {
        this.tagId = tagId;
        this.tagName=tagName;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return tagName;
    }
}
