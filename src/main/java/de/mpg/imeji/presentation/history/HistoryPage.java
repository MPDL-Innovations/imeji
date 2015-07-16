/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.history;

import java.net.URI;
import java.util.Map;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ObjectLoader;

/**
 * An imeji web page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class HistoryPage {
  private int pos = 0;
  private String url;
  private String title;
  private ImejiPages imejiPage;
  private Map<String, String[]> params;

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
      String uriStr = UrlHelper.decode(uri.toString());
      if (ImejiPages.COLLECTION_HOME.matches(uriStr)) {
        return ObjectLoader.loadCollectionLazy(uri, user).getMetadata().getTitle();
      } else if (ImejiPages.ALBUM_HOME.matches(uriStr)) {
        return ObjectLoader.loadAlbumLazy(uri, user).getMetadata().getTitle();
      } else if (ImejiPages.ITEM_DETAIL.matches(uriStr)) {
        return ObjectLoader.loadItem(uri, user).getFilename();
      } else if (ImejiPages.USER_GROUP == imejiPage) {
        String groupUri = UrlHelper.decode(ObjectHelper.getId(uri));
        return ObjectLoader.loadUserGroupLazy(URI.create(groupUri), user).getName();
      } else if (ImejiPages.USER == imejiPage) {
        String email = UrlHelper.decode(ObjectHelper.getId(uri));
        return ObjectLoader.loadUser(email, user).getName();
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
    if (isNull() || page == null || page.isNull())
      return false;
    else if (isNull() && page.isNull())
      return true;
    else
      return url.equals(page.url);
    // return page.imejiPage.equals(imejiPage);
  }

  public boolean isNull() {
    // return (type == null && uri == null);
    return url == null;
  }

  public String getInternationalizedName() {
    try {
      String inter =
          ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getLabel(imejiPage
              .getLabel());
      return title != null ? inter + " " + title : inter;
    } catch (Exception e) {
      return imejiPage.getLabel();
    }
  }

  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  public String getCompleteUrlWithHistory() {
    String delim = params.isEmpty() ? "?" : "&";
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


}
