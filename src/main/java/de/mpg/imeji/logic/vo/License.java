package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.util.DateHelper;

/**
 * A License for imeji objects
 *
 * @author saquet
 *
 */
@j2jResource("http://imeji.org/terms/license")
@j2jId(getMethod = "getId", setMethod = "setId")
public class License implements Serializable {
  private static final long serialVersionUID = -966062330323435843L;
  private URI id;
  @j2jLiteral("http://imeji.org/terms/label")
  private String label;
  @j2jLiteral("http://imeji.org/terms/name")
  private String name;
  @j2jLiteral("http://imeji.org/terms/url")
  private String url;
  @j2jLiteral("http://imeji.org/terms/start")
  private long start = -1;
  @j2jLiteral("http://imeji.org/terms/end")
  private long end = -1;

  public License() {

  }

  public void initWithSerializedString(String s) {
    if (s != null) {
      String[] entries = s.split(",");
      if (entries.length == 3) {
        label = entries[0];
        name = entries[1];
        url = entries[2];
      }
    }
  }

  public String toSerializedString() {
    try {
      return Arrays.asList(URLEncoder.encode(label, "UTF-8"), URLEncoder.encode(name, "UTF-8"),
          URLEncoder.encode(url, "UTF-8")).toString();
    } catch (Exception e) {
      return "";
    }
  }

  @Override
  public License clone() {
    final License clone = new License();
    clone.setId(id);
    clone.setLabel(label);
    clone.setName(name);
    clone.setEnd(end);
    clone.setStart(start);
    clone.setUrl(url);
    return clone;
  }

  public License(ImejiLicenses lic) {
    this.name = lic.name();
    this.url = lic.getUrl();
    this.label = lic.getLabel();
  }

  /**
   * Return the timestamp of the license as a string
   *
   * @return
   */
  public String getTimestamp() {
    final String s = start > 0 ? DateHelper.printDate(DateHelper.getDate(start)) : "...";
    final String e = end > 0 ? DateHelper.printDate(DateHelper.getDate(end)) : "...";
    return s + " - " + e;
  }

  public String getStartTime() {
    return start > 0 ? DateHelper.printDate(DateHelper.getDate(start)) : null;
  }

  public String getEndTime() {
    return end > 0 ? DateHelper.printDate(DateHelper.getDate(end)) : null;
  }

  /**
   * True if this license is emtpy
   *
   * @return
   */
  public boolean isEmtpy() {
    return StringHelper.isNullOrEmptyTrim(name) && StringHelper.isNullOrEmptyTrim(url);
  }

  /**
   * @return the id
   */
  public URI getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(URI id) {
    this.id = id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return the start
   */
  public long getStart() {
    return start;
  }

  /**
   * @param start the start to set
   */
  public void setStart(long start) {
    this.start = start;
  }

  /**
   * @return the end
   */
  public long getEnd() {
    return end;
  }

  /**
   * @param end the end to set
   */
  public void setEnd(long end) {
    this.end = end;
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
  }



}
