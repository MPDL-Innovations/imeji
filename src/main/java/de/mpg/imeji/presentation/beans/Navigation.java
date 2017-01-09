/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.beans;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.Serializable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Defines the page names and Path for imeji. All changes here must be synchronized with
 * WEB-INF/pretty-config.xml The Pages are used by the History
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Navigation implements Serializable {
  private static final Logger LOGGER = Logger.getLogger(Navigation.class);
  private static final long serialVersionUID = -4318697194892200726L;
  // Url of the FW
  public static String frameworkUrl;
  // Url of the application
  public static String applicationUrl;
  // Pages of imeji
  public static final Page HOME = new Page("HomePage", "");
  public static final Page SEARCH = new Page("Search", "search");
  public static final Page HELP = new Page("Help", "help");
  public static final Page BROWSE = new Page("Browse", "browse");
  public static final Page ITEM = new Page("Item", "item");
  public static final Page COLLECTION = new Page("collection", "collection");
  public static final Page ALBUM = new Page("album", "album");
  public static final Page PROFILE = new Page("Profile", "profile");
  public static final Page ALBUMS = new Page("albums", "albums");
  public static final Page COLLECTIONS = new Page("Collections", "collections");
  public static final Page EXPORT = new Page("export", "export");
  public static final Page EDIT = new Page("Edit", "edit");
  public static final Page INFOS = new Page("Info", "infos");
  public static final Page CREATE = new Page("Create", "create");
  public static final Page UPLOAD = new Page("Upload collection", "upload");
  public static final Page SHARE = new Page("Share", "share");
  public static final Page USER = new Page("User", "user");
  public static final Page USERS = new Page("Users", "users");
  public static final Page ADMIN = new Page("Admin", "admin");
  public static final Page OPENSEADRAGON = new Page("Openseadragon", "openseadragon");
  public static final Page SINGLEUPLOAD = new Page("Single upload", "singleupload");
  public static final Page REGISTRATION = new Page("Registration", "register");
  public static final Page IMPRINT = new Page("IMPRINT", "imprint");
  public static final Page TERMS_OF_USE = new Page("Terms of use", "terms_of_use");
  public static final String spaceCommonSlug = "space/";
  public static final String spacesAllSlug = "spaces";


  /**
   * Application bean managing navigation
   *
   * @throws Exception
   */
  public Navigation() {
    try {
      frameworkUrl = PropertyReader.getProperty("escidoc.framework_access.framework.url");
      if (frameworkUrl != null) {
        frameworkUrl = StringHelper.normalizeURI(frameworkUrl);
      }
      applicationUrl = Imeji.PROPERTIES.getApplicationURL();
    } catch (final Exception e) {
      LOGGER.error("Error initializing NavigationBean", e);
    }
  }

  public String getApplicationUrl() {
    return applicationUrl;
  }

  public String getApplicationSpaceUrl() {
    return applicationUrl + getSpacePath();
  }

  public String getApplicationUri() {
    return applicationUrl.substring(0, applicationUrl.length() - 1);
  }

  public String getOpenseadragonUrl() {
    return applicationUrl + OPENSEADRAGON.getPath();
  }

  public String getDomain() {
    return applicationUrl.replaceAll("imeji/", "");
  }

  public String getHomeUrl() {
    // TODO NB.15.04. Change this String
    if (!"".equals(getSpacePath())) {
      return applicationUrl + getSpacePath();
    }
    return getApplicationUri();
  }

  public String getBrowseUrl() {
    return applicationUrl + getSpacePath() + BROWSE.getPath();
  }

  public String getItemUrl() {
    return applicationUrl + getSpacePath() + ITEM.getPath() + "/";
  }

  public String getCollectionUrl() {
    return applicationUrl + getSpacePath() + COLLECTION.getPath() + "/";
  }

  public String getAlbumUrl() {
    return applicationUrl + getSpacePath() + ALBUM.getPath() + "/";
  }

  public String getProfileUrl() {
    return applicationUrl + getSpacePath() + PROFILE.getPath() + "/";
  }

  public String getAlbumsUrl() {
    return applicationUrl + getSpacePath() + ALBUMS.getPath();
  }

  public String getCollectionsUrl() {
    return applicationUrl + getSpacePath() + COLLECTIONS.getPath();
  }

  public String getSpacesUrl() {
    // No need to getSpacePath() Space Path HERE
    return applicationUrl + "spaces";
  }

  public String getImprintUrl() {
    return applicationUrl + getSpacePath() + IMPRINT.getPath();
  }

  public String getSingleUploadUrl() {
    return applicationUrl + getSpacePath() + SINGLEUPLOAD.getPath();
  }

  public String getCreateCollectionUrl() {
    return applicationUrl + getSpacePath() + CREATE.getPath() + COLLECTION.getPath();
  }

  public String getCreateAlbumUrl() {
    return applicationUrl + getSpacePath() + CREATE.getPath() + ALBUM.getPath();
  }

  public String getSearchUrl() {
    return applicationUrl + getSpacePath() + SEARCH.getPath();
  }

  public String getHelpUrl() {
    return applicationUrl + getSpacePath() + HELP.getPath();
  }

  public String getExportUrl() {
    return applicationUrl + getSpacePath() + EXPORT.getPath();
  }

  public String getShareUrl() {
    return applicationUrl + getSpacePath() + SHARE.getPath();
  }

  public String getUserUrl() {
    return applicationUrl + getSpacePath() + USER.getPath();
  }

  public String getUsersUrl() {
    return applicationUrl + getSpacePath() + USERS.getPath();
  }

  public String getAdminUrl() {
    return applicationUrl + getSpacePath() + ADMIN.getPath();
  }

  public String getRegistrationUrl() {
    return applicationUrl + getSpacePath() + REGISTRATION.getPath();
  }

  public String getAutocompleteUrl() {
    return applicationUrl + "autocompleter";
  }

  public String getFileUrl() {
    return applicationUrl + "file/?id=";
  }

  public String getTermsOfUseUrl() {
    return applicationUrl + TERMS_OF_USE.path;
  }

  /*
   * Paths
   */
  public String getCollectionPath() {
    return COLLECTION.path;
  }

  public String getInfosPath() {
    return INFOS.path;
  }

  public String getBrowsePath() {
    return BROWSE.path;
  }

  public String getEditPath() {
    return EDIT.path;
  }

  public String getItemPath() {
    return ITEM.path;
  }

  public String getUploadPath() {
    return UPLOAD.path;
  }

  public String getSpacePath() {
    if (!(isNullOrEmpty(
        ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getSpaceId()))) {
      return spaceCommonSlug + StringHelper
          .normalizeURI(((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getSpaceId());
    }
    return "";
  }

  public String getInternalStorageBase() {
    return Imeji.PROPERTIES.getInternalStorageBase();
  }


  /**
   * An html page
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public static class Page implements Serializable {
    private static final long serialVersionUID = -5718218208615761900L;
    private String name;
    private String path;

    public Page(String name, String path) {
      this.name = name;
      this.path = path;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String file) {
      this.path = file;
    }

    public boolean hasSamePath(String path) {
      return this.path.equals(path) || (this.path + "/").equals(path)
          || ("/" + this.path).equals(path) || ("/" + this.path + "/").equals(path);
    }

  }
}
