
package it.jaschke.alexandria.data;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class VolumeInfo {

    public String title;
    public String subtitle;
    public List<String> authors = new ArrayList<String>();
    public String publisher;
    public String publishedDate;
    public String description;
    public List<IndustryIdentifier> industryIdentifiers = new ArrayList<IndustryIdentifier>();
    public ReadingModes readingModes;
    public Integer pageCount;
    public String printType;
    public List<String> categories = new ArrayList<String>();
    public Float averageRating;
    public Integer ratingsCount;
    public String maturityRating;
    public Boolean allowAnonLogging;
    public String contentVersion;
    public ImageLinks imageLinks;
    public String language;
    public String previewLink;
    public String infoLink;
    public String canonicalVolumeLink;

}
