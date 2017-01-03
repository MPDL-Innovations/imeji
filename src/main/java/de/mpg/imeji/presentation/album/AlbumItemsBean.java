/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.album;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.item.ItemsBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.session.SessionObjectsController;

/**
 * {@link ItemsBean} within an {@link Album}: Used to browse {@link Item} of an {@link Album}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "AlbumItemsBean")
@ViewScoped
public class AlbumItemsBean extends ItemsBean {
  private static final long serialVersionUID = 5828613581065071144L;
  private String id;
  private Album album;
  private URI uri;
  private CollectionImeji collection;
  private boolean active;

  /**
   * Constructor
   */
  public AlbumItemsBean() {
    super();
  }

  @Override
  @PostConstruct
  public void init() {
    super.init();
  }

  @Override
  public void initSpecific() {
    try {
      id = UrlHelper.getParameterValue("id");
      uri = ObjectHelper.getURI(Album.class, id);
      loadAlbum();
      browseContext = getNavigationString() + id;
      active = getSessionBean().getActiveAlbum() != null
          && album.getIdString().equals(getSessionBean().getActiveAlbum().getIdString());
      update();
    } catch (final Exception e) {
      LOGGER.error("Error initializing AlbumItemsBean", e);
    }
  }

  @Override
  public String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:albumBrowse", getSpaceId());
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset,
      int limit) {
    final ItemService controller = new ItemService();
    return controller.search(uri, searchQuery, sortCriterion, getSessionUser(), null, limit,
        offset);
  }

  /**
   * Load the current album
   *
   * @throws ImejiException
   *
   * @
   */
  public void loadAlbum() throws ImejiException {
    album = new AlbumController().retrieveLazy(uri, getSessionUser());
  }

  @Override
  public void initFacets() {
    // NO FACETs FOR ALBUMS
  }

  /**
   * Remove the selected {@link Item} from the current {@link Album}
   *
   * @return @
   */
  public String removeFromAlbum() {
    removeFromAlbum(getSessionBean().getSelected());
    selectNone();
    return "pretty:";
  }

  /**
   * Remove selected {@link Item} from active {@link Album}
   *
   * @return
   * @throws ImejiException @
   */
  public String removeFromActiveAlbum() throws ImejiException {
    removeFromActive(getSessionBean().getSelected());
    selectNone();
    return "pretty:";
  }

  /**
   * Remove all current {@link Item} from {@link Album}
   *
   * @return @
   */
  public String removeAllFromAlbum() {
    try {
      removeAllFromAlbum(album);
    } catch (final ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getMessage(), getLocale()));
    }
    return "pretty:";
  }

  /**
   * Remove all current {@link Item} from active {@link Album}
   *
   * @return
   * @throws ImejiException @
   */
  public String removeAllFromActiveAlbum() throws ImejiException {
    removeAllFromAlbum(getSessionBean().getActiveAlbum());
    return "pretty:";
  }

  /**
   * Remove all {@link Item} from an {@link Album}
   *
   * @param album @
   */
  private void removeAllFromAlbum(Album album) throws ImejiException {
    if (active) {
      final List<String> uris = new ArrayList<>();
      for (final URI uri : getSessionBean().getActiveAlbum().getImages()) {
        uris.add(uri.toString());
      }
      removeFromActive(uris);
    } else {
      final AlbumController ac = new AlbumController();
      final ItemService ic = new ItemService();
      ac.removeFromAlbum(album,
          ic.search(album.getId(), null, null, getSessionUser(), getSpaceId(), -1, 0).getResults(),
          getSessionUser());
    }
  }

  /**
   * Remove a list of {@link Item} from the current {@link Album}
   *
   * @param uris @
   */
  private void removeFromAlbum(List<String> uris) {
    try {
      if (active) {
        removeFromActive(uris);
      } else {
        final ItemService ic = new ItemService();
        album = (Album) ic.searchAndSetContainerItems(album, getSessionUser(), -1, 0);
        final AlbumController ac = new AlbumController();
        final int deletedCount = ac.removeFromAlbum(album, uris, getSessionUser());
        BeanHelper.info(deletedCount + " "
            + Imeji.RESOURCE_BUNDLE.getMessage("success_album_remove_images", getLocale()));
      }
    } catch (final Exception e) {
      BeanHelper.error(e.getMessage());
    }
  }

  /**
   * Remove a list of {@link Item} from the active {@link Album}
   *
   * @param uris @
   */
  private void removeFromActive(List<String> uris) throws ImejiException {
    final SessionObjectsController soc = new SessionObjectsController();
    final int deleted = soc.removeFromActiveAlbum(uris);
    selectNone();
    BeanHelper.info(deleted + " "
        + Imeji.RESOURCE_BUNDLE.getMessage("success_album_remove_images", getLocale()));
  }

  @Override
  public String getImageBaseUrl() {
    if (album == null || album.getId() == null) {
      return "";
    }
    return getNavigation().getApplicationSpaceUrl() + "album/" + this.id + "/";
  }

  @Override
  public String getBackUrl() {
    return getNavigation().getBrowseUrl() + "/album/" + this.id;
  }

  /**
   * Release current {@link Album}
   *
   * @return @
   * @throws Exception
   */
  public String release() {
    final AlbumController ac = new AlbumController();
    try {
      ac.release(album, getSessionUser());
      if (active) {
        getSessionBean().deactivateAlbum();
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_album_release", getLocale()));
    } catch (final Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_album_release", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Issue during release", e);
    }
    return "pretty:";
  }

  /**
   * Delete current {@link Album}
   *
   * @return @
   * @throws Exception
   */
  public String delete() {
    final AlbumController c = new AlbumController();
    try {
      c.delete(album, getSessionUser());
      if (active) {
        getSessionBean().deactivateAlbum();
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_album_delete", getLocale())
          .replace("XXX_albumName_XXX", this.album.getMetadata().getTitle()));
    } catch (final Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_album_delete", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error during delete album", e);
    }
    return SessionBean.getPrettySpacePage("pretty:albums", getSelectedSpaceString());
  }

  /**
   * Withdraw current {@link Album}
   *
   * @return @
   */
  public String withdraw() {
    final AlbumController c = new AlbumController();
    try {
      c.withdraw(album, getSessionUser());
      if (active) {
        getSessionBean().deactivateAlbum();
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_album_withdraw", getLocale()));
    } catch (final Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_album_withdraw", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error during withdraw album", e);
    }
    return "pretty:";
  }

  /**
   * Listener for the discard comment
   *
   * @param event
   */
  @Override
  public void discardCommentListener(ValueChangeEvent event) {
    album.setDiscardComment(event.getNewValue().toString());
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
    // @Ye set session value to share with AlbumItemsBean, another way is
    // via injection
    FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("AlbumItemsBean.id",
        id);
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setAlbum(Album album) {
    this.album = album;
  }

  public Album getAlbum() {
    return album;
  }

  @Override
  public String getType() {
    return PAGINATOR_TYPE.ALBUM_ITEMS.name();
  }

}
