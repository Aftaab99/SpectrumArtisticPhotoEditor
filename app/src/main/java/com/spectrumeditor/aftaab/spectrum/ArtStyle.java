package com.spectrumeditor.aftaab.spectrum;

class ArtStyle {
    private String styleName;
    private int styleResource;
    private String styleModelName;
    enum StyleStatusType {READY, CURRENT, NOT_APPLIED}
    private StyleStatusType status;

    ArtStyle(String styleName, int styleResource, String styleModelName) {
        this.styleName = styleName;
        this.status = StyleStatusType.NOT_APPLIED;
        this.styleModelName = styleModelName;
        this.styleResource = styleResource;
    }

    StyleStatusType getStatus() {
        return status;
    }

    void setStyleStatus(StyleStatusType s) {
        this.status = s;
    }

    String getStyleName() {
        return styleName;
    }

    int getStatusResource() {

        if (status == StyleStatusType.CURRENT)
            return R.drawable.ic_style_current;
        else if (status == StyleStatusType.READY)
            return R.drawable.ic_style_ready;
        else
            return R.drawable.ic_style_not_applied;
    }

    String getModelName() {
        return styleModelName;
    }

    int getStyleResource() {
        return styleResource;
    }
}
