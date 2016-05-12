package de.mpg.imeji.rest.to;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;

@XmlRootElement
@XmlType(propOrder = {"versionOf", "profile"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionTO extends ContainerTO implements Serializable {

  private static final long serialVersionUID = 7039960402363523772L;

  private CollectionProfileTO profile = new CollectionProfileTO();

  public CollectionProfileTO getProfile() {
    return profile;
  }

  public void setProfile(CollectionProfileTO profile) {
    this.profile = profile;
  }
}
