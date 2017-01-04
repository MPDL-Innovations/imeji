/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.history;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.collection.CollectionController;
import de.mpg.imeji.logic.controller.AlbumController;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * An imeji web page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class HistoryPage implements Serializable {
  private static final long serialVersionUID = -6620054520723398563L;
  private int pos = 0;
  private String url;
  private String title;
  private final ImejiPages imejiPage;
  private Map<String, String[]> params;
  private static final Logger LOGGER = Logger.getLogger(HistoryPage.class);


  /**
   * Constructor a an {@link HistoryPage}
   *
   * @param url
   * @param params
   * @param user
   * @throws Exception
   */
  public HistoryPage(String url, Map<String, String[]> params, User user) throws Exception {
    this.params = params;
    this.url = url;
    this.imejiPage = HistoryUtil.getImejiPage(getCompleteUrl());
    this.title = loadTitle(HistoryUtil.extractURI(getCompleteUrl()), user);
  }

  private HistoryPage(String url, Map<String, String[]> params, String title,
      ImejiPages imejiPage) {
    this.params = params;
    this.url = url;
    this.imejiPage = imejiPage;
    this.title = title;
  }

  /**
   * Return a new instance of the same page
   */
  public HistoryPage copy() {
    return new HistoryPage(new String(url), new HashMap<>(params), new String(title), imejiPage);
  }

  /**
   * Load the title the object of the current page according to its id
   *
   * @param uri
   * @return
   * @throws Exception
   */
  private String loadTitle(URI uri, User user) throws Exception {
    // TODO: find better way to "touch" objects for necessary information with permissions
    // or change when the title of the history page should be loaded (after successful request)
    // otherwise the redirect to proper pages comes from here in addition
    if (uri != null) {
      final String uriStr = UrlHelper.decode(uri.toString());
      if (ImejiPages.COLLECTION_HOME.matches(uriStr)) {
        return new CollectionController().retrieveLazy(uri, user).getMetadata().getTitle();
      } else if (ImejiPages.ALBUM_HOME.matches(uriStr)) {
        return new AlbumController().retrieveLazy(uri, user).getMetadata().getTitle();
      } else if (ImejiPages.ITEM_DETAIL.matches(uriStr)) {
        return new ItemService().retrieveLazy(uri, user).getFilename();
      } else if (ImejiPages.USER_GROUP == imejiPage) {
        final String groupUri = UrlHelper.decode(ObjectHelper.getId(uri));
        return new UserGroupService().read(URI.create(groupUri), user).getName();
      } else if (ImejiPages.USER == imejiPage) {
        final String email = UrlHelper.decode(ObjectHelper.getId(uri));
        if (user != null && email.equals(user.getEmail())) {
          return user.getPerson().getCompleteName();
        } else {
          return new UserService().retrieve(email, Imeji.adminUser).getPerson()
              .getCompleteName();
        }
      }
    }
    return "";
  }

  /**
   * Compares 2 {@link HistoryPage}
   *
   * @param page
   * @return
   */
  public boolean isSame(HistoryPage page) {
    if (isNull() || page == null || page.isNull()) {
      return false;
    } else if (isNull() && page.isNull()) {
      return true;
    } else {
      return url.equals(page.url);
    }
  }

  public boolean isNull() {
    // return (type == null && uri == null);
    return url == null;
  }

  public String getInternationalizedName() {
    try {
      final String inter =
          Imeji.RESOURCE_BUNDLE.getLabel(imejiPage.getLabel(), BeanHelper.getLocale());
      return title != null ? inter + " " + title : inter;
    } catch (final Exception e) {
      return imejiPage.getLabel();
    }
  }

  /**
   * Set a parameter (for instance q) with a new value. To get it as an RUL, call getCompleteUrl()
   *
   * @param param
   * @param value
   */
  public void setParamValue(String param, String value) {
    final String[] valueArray = {value};
    params.put(param, valueArray);
  }

  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  public String getCompleteUrlWithHistory() {
    final String delim = params.isEmpty() ? "?" : "&";
    return getCompleteUrl() + delim + "h=" + pos;
  }

  public String getCompleteUrl() {
    return url + HistoryUtil.paramsMapToString(params);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setParams(Map<String, String[]> params) {
    this.params = params;
  }

  public Map<String, String[]> getParams() {
    return params;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ImejiPages getImejiPage() {
    return imejiPage;
  }
}
