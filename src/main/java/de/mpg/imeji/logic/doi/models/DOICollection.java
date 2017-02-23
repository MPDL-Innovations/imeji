package de.mpg.imeji.logic.doi.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "resource")
public class DOICollection implements Serializable {
  private static final long serialVersionUID = 5767739527817909134L;
  private DOIIdentifier identifier;
  private final DOICreators creators = new DOICreators();
  private final List<DOITitle> titles = new ArrayList<DOITitle>();
  private final String publisher = "Max-Planck-Gesellschaft zur Förderung der Wissenschaften e. V.";
  private String publicationYear;


  @XmlElement(name = "creators")
  public DOICreators getCreators() {
    return creators;
  }

  @XmlElement(name = "titles")
  public List<DOITitle> getTitles() {
    return titles;
  }

  @XmlElement(name = "publisher")
  public String getPublisher() {
    return publisher;
  }

  public String getPublicationYear() {
    return publicationYear;
  }

  public void setPublicationYear(String publicationYear) {
    this.publicationYear = publicationYear;
  }

  public DOIIdentifier getIdentifier() {
    return identifier;
  }

  public void setIdentifier(DOIIdentifier identifier) {
    this.identifier = identifier;
  }


}
