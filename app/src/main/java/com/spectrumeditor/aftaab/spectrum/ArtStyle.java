package com.spectrumeditor.aftaab.spectrum;

public class ArtStyle {
    private String styleName;
    private String status;
    private int styleResource;
    private String styleModelName;

    public static final String ARTSTYLE_READY = "READY";
    public static final String ARTSTYLE_CURRENT = "CURRENT";
    public static final String ARTSTYLE_NOTAPPLIED = "NOT_APPLIED";

    public ArtStyle(String styleName, String status, int styleResource, String styleModelName) {
        this.styleName = styleName;
        this.status = status;
        this.styleModelName = styleModelName;
        this.styleResource = styleResource;
    }

    public String getStatus() {
        return status;
    }

    public void setStyleStatus(String status) {
        if (status.equals(ARTSTYLE_READY) || status.equals(ARTSTYLE_CURRENT) || status.equals(ARTSTYLE_NOTAPPLIED))
            this.status = status;
    }

    public String getStyleName() {
        return styleName;
    }

    public int getStatusResource() {

        if (status.equals(ARTSTYLE_CURRENT))
            return R.drawable.ic_style_current;
        else if (status.equals(ARTSTYLE_READY))
            return R.drawable.ic_style_ready;
        else
            return R.drawable.ic_style_not_applied;
    }

    public String getModelName() {
        return styleModelName;
    }

    public int getStyleResource() {
        return styleResource;
    }
}
