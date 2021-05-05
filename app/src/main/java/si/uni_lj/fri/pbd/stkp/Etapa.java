package si.uni_lj.fri.pbd.stkp;

public class Etapa {

    private String name;
    private String desc;
    private String link;
    private String category;
    private String gpxFileName;
    private boolean expanded;

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Etapa (String name, String desc, String link, String category, String gpxFileName) {
        this.name = name;
        this.desc = desc;
        this.link = link;
        this.category = category;
        this.gpxFileName = gpxFileName;
        this.expanded = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getGpxFileName() {
        return gpxFileName;
    }

    public void setGpxFileName(String gpxFileName) {
        this.gpxFileName = gpxFileName;
    }
}
