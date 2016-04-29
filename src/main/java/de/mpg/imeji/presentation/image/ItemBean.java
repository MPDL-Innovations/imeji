/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.image;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchFactory;
import de.mpg.imeji.logic.search.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.presentation.beans.MetadataLabels;
import de.mpg.imeji.presentation.beans.SuperViewBean;
import de.mpg.imeji.presentation.metadata.MetadataSetWrapper;
import de.mpg.imeji.presentation.metadata.SingleEditorWrapper;
import de.mpg.imeji.presentation.metadata.extractors.TikaExtractor;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.session.SessionObjectsController;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Bean for a Single image
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "ItemBean")
@ViewScoped
public class ItemBean extends SuperViewBean {
  private String tab;
  private Item item;
  private String id;
  private boolean selected;
  private CollectionImeji collection;
  private List<String> techMd;
  private MetadataProfile profile;
  private SingleEditorWrapper edit;
  protected String prettyLink;
  private SingleItemBrowse browse = null;
  private MetadataSetWrapper mds;
  private List<Album> relatedAlbums;
  private String dateCreated;
  private String newFilename;
  private String stringContent = null;
  private String imageUploader;
  private String discardComment;
  private MetadataLabels metadataLabels;
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItems;
  @ManagedProperty(value = "#{SessionBean.activeAlbum}")
  private Album activeAlbum;
  private static final Logger LOGGER = Logger.getLogger(ItemBean.class);

  /**
   * Construct a default {@link ItemBean}
   * 
   * @throws Exception
   */
  public ItemBean() {
    prettyLink = SessionBean.getPrettySpacePage("pretty:editImage", getSpace());
  }

  /**
   * Initialize the {@link ItemBean}
   * 
   * @return
   * @throws IOException
   * @throws Exception
   */
  @PostConstruct
  public void init() {
    this.id = UrlHelper.getParameterValue("id");
    tab = UrlHelper.getParameterValue("tab");
    if ("".equals(tab)) {
      tab = null;
    }
    try {
      loadImage();
      if (item != null) {
        if ("techmd".equals(tab)) {
          initViewTechnicalMetadata();
        } else if ("util".equals(tab)) {
          initUtilTab();
        } else {
          initViewMetadataTab();
        }
        initBrowsing();
        selected = getSelectedItems().contains(item.getId().toString());
      } else {
        edit = null;
      }
    } catch (NotFoundException e) {
      LOGGER.error("Error loading item", e);
      try {
        FacesContext.getCurrentInstance().getExternalContext().responseSendError(404,
            "404_NOT_FOUND");
      } catch (IOException e1) {
        LOGGER.error("Error sending error", e1);;
      }
    } catch (Exception e) {
      LOGGER.error("Error initialitzing item page", e);
      BeanHelper.error("Error initializing page" + e.getMessage());
    }
  }

  /**
   * Initialize the util tab
   * 
   * @throws Exception
   */
  private void initUtilTab() throws Exception {
    relatedAlbums = new ArrayList<Album>();
    AlbumController ac = new AlbumController();
    SearchQuery q = new SearchQuery();
    q.addPair(new SearchPair(SearchIndex.SearchFields.member, SearchOperators.EQUALS,
        getImage().getId().toString(), false));
    // TODO NB: check if related albums should be space restricted?
    relatedAlbums = (List<Album>) ac.retrieveBatchLazy(
        ac.search(q, getSessionUser(), null, -1, 0, null).getResults(), getSessionUser(), -1, 0);
    initImageUploader();
  }

  /**
   * Find the user name of the user who upload the file
   */
  private void initImageUploader() {
    Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.JENA);
    List<String> users =
        search.searchString(JenaCustomQueries.selectUserCompleteName(item.getCreatedBy()), null,
            Imeji.adminUser, 0, 1).getResults();
    if (users != null && users.size() > 0) {
      imageUploader = users.get(0);
    } else {
      imageUploader = Imeji.RESOURCE_BUNDLE.getLabel("unknown_user", getLocale());
    }
  }

  /**
   * Initialize the metadata information when the "view metadata" tab is called.
   * 
   * @throws Exception
   */
  public void initViewMetadataTab() throws Exception {
    if (item != null) {
      this.discardComment = null;
      User user = getSessionUser();
      if (AuthUtil.canReadItemButNotCollection(user, item)) {
        // User has right to read the item, but not collection and the
        // profile
        user = Imeji.adminUser;
      }
      loadCollection(user);
      loadProfile(user);
      metadataLabels = new MetadataLabels(profile, getLocale());
      edit = new SingleEditorWrapper(item, profile, getSessionUser(), getLocale());
      mds = edit.getEditor().getItems().get(0).getMds();
    }
  }

  /**
   * Initialize the technical metadata when the "technical metadata" tab is called
   * 
   * @throws Exception
   */
  public void initViewTechnicalMetadata() throws Exception {
    try {
      techMd = new ArrayList<String>();
      techMd = TikaExtractor.extract(item);
    } catch (Exception e) {
      techMd = new ArrayList<String>();
      techMd.add(e.getMessage());
      LOGGER.error("Error reading technical metadata", e);
    }
  }

  /**
   * Initiliaue the {@link SingleItemBrowse} for this {@link ItemBean}
   * 
   * @throws Exception
   */
  public void initBrowsing() throws Exception {
    if (item != null) {
      ItemsBean itemsBean = (ItemsBean) BeanHelper.getSessionBean(ItemsBean.class);
      if (UrlHelper.getParameterBoolean("reload")) {
        itemsBean.browseInit(); // search the items
        itemsBean.update(); // Load the items
      }
      browse = new SingleItemBrowse(itemsBean, item, "item", "");
    }
  }

  /**
   * Load the item according to the idntifier defined in the URL
   * 
   * @throws Exception
   */
  public void loadImage() throws Exception {
    item = new ItemController().retrieve(ObjectHelper.getURI(Item.class, id), getSessionUser());
    if (item == null) {
      throw new NotFoundException("LoadImage: empty");
    }
  }

  /**
   * Load the collection according to the identifier defined in the URL
   */
  public void loadCollection(User user) {
    try {
      collection = new CollectionController().retrieveLazy(item.getCollection(), user);
    } catch (Exception e) {
      BeanHelper.error(e.getMessage());
      collection = null;
      LOGGER.error("Error loading collection", e);
    }
  }

  /**
   * Load the {@link MetadataProfile} of the {@link Item}
   * 
   * @throws ImejiException
   */
  public void loadProfile(User user) throws ImejiException {
    profile = new ProfileController().retrieve(item.getMetadataSet().getProfile(), user);
  }

  /**
   * Return and URL encoded version of the filename
   * 
   * @return
   * @throws UnsupportedEncodingException
   */
  public String getEncodedFileName() throws UnsupportedEncodingException {
    return URLEncoder.encode(item.getFilename(), "UTF-8");
  }

  public List<String> getTechMd() throws Exception {
    return techMd;
  }

  public void setTechMd(List<String> md) {
    this.techMd = md;
  }

  public String getPageUrl() {
    return getNavigation().getItemUrl() + id;
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  public void setImage(Item item) {
    this.item = item;
  }

  public Item getImage() {
    return item;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public boolean getSelected() {
    return selected;
  }

  public String getThumbnailImageUrlAsString() {
    if (item.getThumbnailImageUrl() == null)
      return "/no_thumb";
    return item.getThumbnailImageUrl().toString();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTab() {
    return tab;
  }

  public void setTab(String tab) {
    this.tab = tab.toUpperCase();
  }

  public String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:item", getSpace());
  }

  public void makePublic() throws ImejiException {
    ItemController c = new ItemController();
    c.release(Arrays.asList(item), getSessionUser());
    item = c.retrieve(item.getId(), getSessionUser());
  }

  public void makePrivate() throws ImejiException {
    ItemController c = new ItemController();
    c.unRelease(Arrays.asList(item), getSessionUser());
    item = c.retrieve(item.getId(), getSessionUser());
  }

  public void saveEditor() throws IOException {
    try {
      edit.getEditor().save();
      BeanHelper.addMessage(Imeji.RESOURCE_BUNDLE.getMessage("success_editor_image", getLocale()));
      redirect(getHistory().getCurrentPage().getUrl());
      return;
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error saving item metadata", e);
    } catch (ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_metadata_edit", getLocale()));
      LOGGER.error("Error saving item metadata", e);
    }
  }

  /**
   * Add the current {@link Item} to the active {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String addToActiveAlbum() throws Exception {
    SessionObjectsController soc = new SessionObjectsController();
    List<String> l = new ArrayList<String>();
    l.add(item.getId().toString());
    int sizeBeforeAdd = getActiveAlbum().getImages().size();
    soc.addToActiveAlbum(l);
    int sizeAfterAdd = getActiveAlbum().getImages().size();
    boolean added = sizeAfterAdd > sizeBeforeAdd;
    if (!added) {
      BeanHelper
          .error(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename()
              + " " + Imeji.RESOURCE_BUNDLE.getMessage("already_in_active_album", getLocale()));
    } else {
      BeanHelper
          .info(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename()
              + " " + Imeji.RESOURCE_BUNDLE.getMessage("added_to_active_album", getLocale()));
    }
    return "";
  }

  /**
   * Remove the {@link Item} from the database. If the item was in the current {@link Album}, remove
   * the {@link Item} from it
   * 
   * @throws Exception
   */
  public void delete() throws Exception {
    if (getIsInActiveAlbum()) {
      removeFromActiveAlbum();
    }
    new ItemController().delete(Arrays.asList(item), getSessionUser());
    new SessionObjectsController().unselectItem(item.getId().toString());
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename()
        + " " + Imeji.RESOURCE_BUNDLE.getMessage("success_collection_remove_from", getLocale()));
    redirectToBrowsePage();
  }

  /**
   * Discard the Item
   * 
   * @throws ImejiException
   * @throws IOException
   */
  public void withdraw() throws ImejiException, IOException {
    new ItemController().withdraw(Arrays.asList(item), getDiscardComment(), getSessionUser());
    new SessionObjectsController().unselectItem(item.getId().toString());
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename()
        + " " + Imeji.RESOURCE_BUNDLE.getMessage("success_item_withdraw", getLocale()));
    redirectToBrowsePage();
  }

  /**
   * Listener for the discard comment
   * 
   * @param event
   */
  public void discardCommentListener(ValueChangeEvent event) {
    this.discardComment = event.getNewValue().toString();
  }

  /**
   * Remove the {@link Item} from the active {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String removeFromActiveAlbum() throws Exception {
    new SessionObjectsController().removeFromActiveAlbum(Arrays.asList(item.getId().toString()));
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename()
        + " " + Imeji.RESOURCE_BUNDLE.getMessage("success_album_remove_from", getLocale()));
    return "pretty:";
  }

  /**
   * Return true if the {@link Item} is in the active {@link Album}
   * 
   * @return
   */
  public boolean getIsInActiveAlbum() {
    if (getActiveAlbum() != null && item != null) {
      return getActiveAlbum().getImages().contains(item.getId());
    }
    return false;
  }

  /**
   * True if the current item page is part of the current album
   * 
   * @return
   */
  public boolean getIsActiveAlbum() {
    return false;
  }

  /**
   * Redirect to the browse page
   * 
   * @throws IOException
   */
  public void redirectToBrowsePage() throws IOException {
    redirect(getNavigation().getBrowseUrl());
  }

  /**
   * Listener of the value of the select box
   * 
   * @param event
   */
  public void selectedChanged(ValueChangeEvent event) {
    SessionObjectsController soc = new SessionObjectsController();
    if (event.getNewValue().toString().equals("true")) {
      setSelected(true);
      soc.selectItem(item.getId().toString());
    } else if (event.getNewValue().toString().equals("false")) {
      setSelected(false);
      soc.unselectItem(item.getId().toString());
    }
  }

  public MetadataProfile getProfile() {
    return profile;
  }

  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }

  public List<SelectItem> getStatementMenu() throws ImejiException {
    List<SelectItem> statementMenu = new ArrayList<SelectItem>();
    if (profile == null) {
      loadProfile(getSessionUser());
    }
    for (Statement s : profile.getStatements()) {
      statementMenu.add(new SelectItem(s.getId(), s.getLabels().iterator().next().toString()));
    }
    return statementMenu;
  }

  public SingleEditorWrapper getEdit() {
    return edit;
  }

  public void setEdit(SingleEditorWrapper edit) {
    this.edit = edit;
  }

  public boolean isLocked() {
    return Locks.isLocked(this.item.getId().toString(), getSessionUser().getEmail());
  }

  public SingleItemBrowse getBrowse() {
    return browse;
  }

  public void setBrowse(SingleItemBrowse browse) {
    this.browse = browse;
  }

  public String getDescription() {
    for (Statement s : getProfile().getStatements()) {
      if (s.isDescription()) {
        for (Metadata md : this.getImage().getMetadataSet().getMetadata()) {
          if (md.getStatement().equals(s.getId())) {
            return md.asFulltext();
          }
        }
      }
    }
    return item.getFilename();
  }

  /**
   * Function to return the content of the item
   * 
   * @return String
   */
  public String getStringContent() throws ImejiException {
    StorageController sc = new StorageController();
    stringContent = sc.readFileStringContent(item.getFullImageUrl().toString());
    return stringContent;
  }

  /**
   * Returns a list of all albums this image is added to.
   * 
   * @return
   * @throws Exception
   */
  public List<Album> getRelatedAlbums() throws Exception {
    return relatedAlbums;
  }

  /**
   * Return the {@link User} having uploaded the file for this item
   * 
   * @return
   * @throws Exception
   */
  public String getImageUploader() throws Exception {
    return imageUploader;
  }

  /**
   * setter
   * 
   * @param mds the mds to set
   */
  public void setMds(MetadataSetWrapper mds) {
    this.mds = mds;
  }

  /**
   * getter
   * 
   * @return the mds
   */
  public MetadataSetWrapper getMds() {
    return mds;
  }

  /**
   * getter
   * 
   * @return
   */
  public String getItemStorageIdFilename() {
    return StringHelper.normalizeFilename(this.item.getFilename());
  }

  /**
   * True if the current file is an image
   * 
   * @return
   */
  public boolean isImageFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename()))
        .contains("image");
  }


  /**
   * True if the data can be viewed in the data viewer (defined in the configuration)
   * 
   * @return
   */
  public boolean isViewInDataViewer() {
    return Imeji.CONFIG
        .isDataViewerSupportedFormats(FilenameUtils.getExtension(item.getFilename()));
  }

  /**
   * View the File in Digilib (if Digilib is enabled)
   * 
   * @return
   */
  public boolean isViewInDigilib() {
    return Imeji.PROPERTIES.isDigilibEnabled() && isImageFile() && !isSVGFile();
  }

  /**
   * True if the file is an svg
   * 
   * @return
   */
  public boolean isSVGFile() {
    return "svg".equals(FilenameUtils.getExtension(item.getFullImageUrl().toString()));
  }

  /**
   * True if the current file is a video
   * 
   * @return
   */
  public boolean isVideoFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename()))
        .contains("video");
  }

  /**
   * True if the File is a RAW file (a file which can not be viewed in any online tool)
   * 
   * @return
   */
  public boolean isRawFile() {
    return !isAudioFile() && !isVideoFile() && !isImageFile() && !isPdfFile();
  }

  /**
   * True if the current file is a pdf
   * 
   * @return
   */
  public boolean isPdfFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename()))
        .contains("application/pdf");
  }

  /**
   * Function checks if the file ends with swc
   */
  public boolean isSwcFile() {
    return item.getFullImageUrl().toString().endsWith(".swc");
  }

  /**
   * True if the current file is an audio
   * 
   * @return
   */
  public boolean isAudioFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename()))
        .contains("audio");
  }

  /**
   * @return the dateCreated
   */
  public String getDateCreated() {
    return dateCreated;
  }

  /**
   * @param dateCreated the dateCreated to set
   */
  public void setDateCreated(String dateCreated) {
    this.dateCreated = dateCreated;
  }

  public String getNewFilename() {
    this.newFilename = getImage().getFilename();
    return newFilename;
  }

  public void setNewFilename(String newFilename) {
    if (!"".equals(newFilename))
      getImage().setFilename(newFilename);
  }

  /**
   * @return the discardComment
   */
  public String getDiscardComment() {
    return discardComment;
  }

  /**
   * @param discardComment the discardComment to set
   */
  public void setDiscardComment(String discardComment) {
    this.discardComment = discardComment;
  }

  public MetadataLabels getMetadataLabels() {
    return metadataLabels;
  }

  public List<String> getSelectedItems() {
    return selectedItems;
  }

  public void setSelectedItems(List<String> selectedItems) {
    this.selectedItems = selectedItems;
  }

  public Album getActiveAlbum() {
    return activeAlbum;
  }

  public void setActiveAlbum(Album activeAlbum) {
    this.activeAlbum = activeAlbum;
  }
}
