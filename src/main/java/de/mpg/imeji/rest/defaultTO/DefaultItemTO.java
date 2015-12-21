package de.mpg.imeji.rest.defaultTO;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import de.mpg.imeji.rest.to.PropertiesTO;

@XmlRootElement
@XmlType(propOrder = {"id", "createdBy", "modifiedBy", "createdDate", "modifiedDate",
    "versionDate", "status", "visibility", "version", "discardComment", "collectionId", "filename",
    "mimetype", "checksumMd5", "webResolutionUrlUrl", "thumbnailUrl", "fileUrl", "metadata"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefaultItemTO extends PropertiesTO implements Serializable {

  private static final long serialVersionUID = -1870847854605861134L;

  private String visibility;

  private String collectionId;

  private String filename;

  private String mimetype;

  private String checksumMd5;

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  private URI webResolutionUrlUrl;

  private URI thumbnailUrl;

  private URI fileUrl;

  private long fileSize;

  private Map<String, JsonNode> metadata = new HashMap<String, JsonNode>();

  public String getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(String collectionId) {
    this.collectionId = collectionId;
  }

  public Map<String, JsonNode> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, JsonNode> metadata) {
    this.metadata = metadata;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getMimetype() {
    return mimetype;
  }

  public void setMimetype(String mimetype) {
    this.mimetype = mimetype;
  }

  public String getChecksumMd5() {
    return checksumMd5;
  }

  public void setChecksumMd5(String checksumMd5) {
    this.checksumMd5 = checksumMd5;
  }

  public URI getWebResolutionUrlUrl() {
    return webResolutionUrlUrl;
  }

  public void setWebResolutionUrlUrl(URI webResolutionUrlUrl) {
    this.webResolutionUrlUrl = webResolutionUrlUrl;
  }

  public URI getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(URI thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public URI getFileUrl() {
    return fileUrl;
  }

  public void setFileUrl(URI fileUrl) {
    this.fileUrl = fileUrl;
  }



}
