/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.controller.business;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.ImejiController;
import de.mpg.imeji.logic.controller.resource.ContentController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.LicenseUtil;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ImejiFactory;

/**
 * Implements CRUD and Search methods for {@link Item}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ItemBusinessController extends ImejiController {
  private static final Logger LOGGER = Logger.getLogger(ItemBusinessController.class);
  public static final String NO_THUMBNAIL_URL = "NO_THUMBNAIL_URL";
  private final Search search =
      SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.ELASTIC);

  private ItemController itemController = new ItemController();

  /**
   * Controller constructor
   */
  public ItemBusinessController() {
    super();
  }

  /**
   * Create an {@link Item} in a {@link CollectionImeji}
   *
   * @param item
   * @param coll
   * @param user
   * @throws ImejiException
   * @return
   */
  public Item create(Item item, CollectionImeji coll, User user) throws ImejiException {
    create(Arrays.asList(item), coll, user);
    return item;
  }

  /**
   * Create an {@link Item} for a {@link File}.
   *
   * @param f
   * @param filename (optional)
   * @param c - the collection in which the file is uploaded
   * @param user
   * @return
   * @throws ImejiException
   */
  public Item createWithFile(Item item, File f, String filename, CollectionImeji c, User user)
      throws ImejiException {
    System.out.println("CREATE ITEM WITH FILE " + f.getAbsolutePath());
    if (StringHelper.isNullOrEmptyTrim(filename)) {
      throw new UnprocessableError("Filename must not be empty!");
    }
    validateChecksum(c.getId(), f, false);
    ContentController contentController = new ContentController();
    ContentVO content = contentController.create(item, f, c, user);
    System.out.println("CONTENT CREATED " + f.getAbsolutePath());
    if (item == null) {
      item = ImejiFactory.newItem(c);
    }
    item = copyContent(item, content);
    item.setFilename(filename);
    if (c.getStatus() == Status.RELEASED) {
      item.setStatus(Status.RELEASED);
    }
    item = create(item, c, user);
    System.out.println("Item CREATED " + item.getIdString());
    contentController.extractFileContentAndUpdateContentVOAsync(item.getId().toString(), content);
    System.out.println("File content extracted ");
    return item;
  }

  /**
   * Create an {@link Item} with an external {@link File} according to its URL
   *
   * @param item
   * @param c
   * @param externalFileUrl
   * @param download
   * @param user
   * @return
   * @throws IOException
   * @throws ImejiException
   */
  public Item createWithExternalFile(Item item, CollectionImeji c, String externalFileUrl,
      String filename, boolean download, User user) throws ImejiException {
    String origName = FilenameUtils.getName(externalFileUrl);
    if ("".equals(filename) || filename == null) {
      filename = origName;
    }
    // Filename extension will be added if not provided.
    // Will not be appended if it is the same value from original external reference again
    // Original external reference will be appended to the provided extension in addition
    else if (FilenameUtils.getExtension(filename).equals("")
        || !FilenameUtils.getExtension(filename).equals(FilenameUtils.getExtension(origName))) {
      filename = filename + "." + FilenameUtils.getExtension(origName);
    }

    if (filename == null || filename.equals("")) {
      throw new BadRequestException(
          "Could not derive the filename. Please provide the filename with the request!");
    }

    if (externalFileUrl == null || externalFileUrl.equals("")) {
      throw new BadRequestException("Please provide fetchUrl or referenceUrl with the request!");
    }
    if (item == null) {
      item = ImejiFactory.newItem(c);
    }
    if (download) {
      // download the file in storage
      File tmp = readFile(externalFileUrl);
      item = createWithFile(item, tmp, filename, c, user);
    } else {
      // Reference the file
      item.setFilename(filename);
      item.setFullImageUrl(URI.create(externalFileUrl));
      item.setThumbnailImageUrl(URI.create(NO_THUMBNAIL_URL));
      item.setWebImageUrl(URI.create(NO_THUMBNAIL_URL));
      item = create(item, c, user);
    }
    return item;
  }

  /**
   * Create a {@link List} of {@link Item} in a {@link CollectionImeji}. This method is faster than
   * using create(Item item, URI coll) when creating many items
   *
   * @param items
   * @param coll
   * @throws ImejiException
   */
  public void create(Collection<Item> items, CollectionImeji ic, User user) throws ImejiException {
    itemController.create(items, ic, user);
  }

  /**
   * Create an {@link Item}
   *
   * @param item
   * @param uploadedFile
   * @param filename
   * @param u
   * @param fetchUrl
   * @param referenceUrl
   * @return
   * @throws ImejiException
   */
  public Item create(Item item, CollectionImeji collection, File uploadedFile, String filename,
      User u, String fetchUrl, String referenceUrl) throws ImejiException {
    isLoggedInUser(u);
    if (uploadedFile != null && isNullOrEmpty(filename)) {
      throw new BadRequestException("Filename for the uploaded file must not be empty!");
    }

    if (uploadedFile == null && isNullOrEmpty(fetchUrl) && isNullOrEmpty(referenceUrl)) {
      throw new BadRequestException(
          "Please upload a file or provide one of the fetchUrl or referenceUrl as input.");
    }
    Item newItem = new Item(item);
    if (uploadedFile != null) {
      newItem = createWithFile(item, uploadedFile, filename, collection, u);
    } else if (getExternalFileUrl(fetchUrl, referenceUrl) != null) {
      // If no file, but either a fetchUrl or a referenceUrl
      newItem = createWithExternalFile(item, collection, getExternalFileUrl(fetchUrl, referenceUrl),
          filename, downloadFile(fetchUrl), u);
    } else {
      throw new BadRequestException("Filename or reference must not be empty!");
    }

    return newItem;
  }

  /**
   * User ObjectLoader to load image
   *
   * @param imgUri
   * @return
   * @throws ImejiException
   */
  public Item retrieve(URI imgUri, User user) throws ImejiException {
    return itemController.retrieve(imgUri, user);
  }

  public Item retrieveLazy(URI imgUri, User user) throws ImejiException {
    return itemController.retrieveLazy(imgUri, user);
  }

  /**
   * Lazy Retrieve the Item containing the file with the passed storageid
   *
   * @param storageId
   * @param user
   * @return
   * @throws ImejiException
   */
  public Item retrieveLazyForFile(String fileUrl, User user) throws ImejiException {
    Search s = SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.JENA);
    List<String> r =
        s.searchString(JenaCustomQueries.selectItemOfFile(fileUrl), null, null, 0, -1).getResults();
    if (!r.isEmpty() && r.get(0) != null) {
      return retrieveLazy(URI.create(r.get(0)), user);
    } else {
      throw new NotFoundException("Can not find the resource requested");
    }
  }

  /**
   * Retrieve the items lazy (without the metadata)
   *
   * @param uris
   * @param limit
   * @param offset
   * @return
   * @throws ImejiException
   */
  public Collection<Item> retrieveBatch(List<String> uris, int limit, int offset, User user)
      throws ImejiException {
    return itemController.retrieveBatch(uris, limit, offset, user);
  }

  /**
   * Retrieve the items fully (with all metadata)
   *
   * @param uris
   * @param limit
   * @param offset
   * @param user
   * @return
   * @throws ImejiException
   */
  public Collection<Item> retrieveBatchLazy(List<String> uris, int limit, int offset, User user)
      throws ImejiException {
    return itemController.retrieveBatchLazy(uris, limit, offset, user);
  }


  /**
   * Retrieve all {@link Item} (all status, all users) in imeji
   *
   * @return
   * @throws ImejiException
   */
  public Collection<Item> retrieveAll(User user) throws ImejiException {
    List<String> uris = ImejiSPARQL.exec(JenaCustomQueries.selectItemAll(), Imeji.imageModel);
    LOGGER.info(uris.size() + " items found, retrieving...");
    return retrieveBatch(uris, -1, 0, user);
  }

  /**
   * Update an {@link Item} in the database
   *
   * @param item
   * @param user
   * @throws ImejiException
   */
  public Item update(Item item, User user) throws ImejiException {
    updateBatch(Arrays.asList(item), user);
    return retrieve(item.getId(), user);
  }

  /**
   * Update a {@link Collection} of {@link Item}
   *
   * @param items
   * @param user
   * @throws ImejiException
   */
  public void updateBatch(Collection<Item> items, User user) throws ImejiException {
    itemController.updateBatch(items, user);
  }

  /**
   * Update the File of an {@link Item}
   *
   * @param item
   * @param f
   * @param user
   * @return
   * @throws ImejiException
   */
  public Item updateFile(Item item, CollectionImeji col, File f, String filename, User user)
      throws ImejiException {
    validateChecksum(item.getCollection(), f, true);
    ContentController contentController = new ContentController();
    ContentVO content;
    if (StringHelper.isNullOrEmptyTrim(item.getContentId())) {
      content = contentController.create(item, f, col, user);
    } else {
      content = contentController.update(item.getContentId(), f, col, user);
    }
    if (filename != null) {
      item.setFilename(filename);
    }
    item = copyContent(item, content);
    item = update(item, user);
    contentController.extractFileContentAndUpdateContentVOAsync(item.getId().toString(), content);
    return item;
  }

  /**
   * Update the {@link Item} with External link to File.
   *
   * @param item
   * @param externalFileUrl
   * @param filename
   * @param download
   * @param u
   * @return
   * @throws ImejiException
   */
  public Item updateWithExternalFile(Item item, CollectionImeji col, String externalFileUrl,
      String filename, boolean download, User u) throws ImejiException {
    String origName = FilenameUtils.getName(externalFileUrl);
    filename =
        isNullOrEmpty(filename) ? origName : filename + "." + FilenameUtils.getExtension(origName);
    item.setFilename(filename);
    if (download) {
      File tmp = readFile(externalFileUrl);
      item = updateFile(item, col, tmp, filename, u);
    } else {
      removeFileFromStorage(item);
      // Reference the file
      item.setFullImageUrl(URI.create(externalFileUrl));
      item.setThumbnailImageUrl(URI.create(NO_THUMBNAIL_URL));
      item.setWebImageUrl(URI.create(NO_THUMBNAIL_URL));
      item.setChecksum("");
      item.setFiletype("");
      item = update(item, u);
    }
    return item;

  }

  /**
   *
   * Update only the thumbnail and the Web Resolution (doesn't change the original file)
   *
   * @param item
   * @param f
   * @param user
   * @return
   * @throws ImejiException
   */
  public Item updateThumbnail(Item item, File f, User user) throws ImejiException {
    StorageController sc = new StorageController();
    sc.update(item.getWebImageUrl().toString(), f);
    sc.update(item.getThumbnailImageUrl().toString(), f);
    return update(item, user);
  }

  /**
   * Delete a {@link List} of {@link Item} inclusive all files stored in the {@link Storage}
   *
   * @param items
   * @param user
   * @return
   * @throws ImejiException
   */
  public void delete(List<Item> items, User user) throws ImejiException {
    itemController.delete(items, user);
    for (Item item : items) {
      removeFileFromStorage(item);
    }
  }

  /**
   * Delete a {@link List} of {@link Item} inclusive all files stored in the {@link Storage}
   *
   * @param itemId
   * @param u
   * @return
   * @throws ImejiException
   */
  public void delete(String itemId, User u) throws ImejiException {
    Item item = retrieve(ObjectHelper.getURI(Item.class, itemId), u);
    delete(Arrays.asList(item), u);
  }

  /**
   * Search {@link Item}
   *
   * @param containerUri - if the search is done within a {@link Container}
   * @param searchQuery - the {@link SearchQuery}
   * @param sortCri - the {@link SortCriterion}
   * @param user
   * @param size
   * @param offset
   * @return
   */
  public SearchResult search(URI containerUri, SearchQuery searchQuery, SortCriterion sortCri,
      User user, String spaceId, int size, int offset) {
    return search.search(searchQuery, sortCri, user,
        containerUri != null ? containerUri.toString() : null, spaceId, offset, size);
  }

  /**
   * load items of a container. Perform a search to load all items: is faster than to read the
   * complete container
   *
   * @param c
   * @param user
   */
  public Container searchAndSetContainerItems(Container c, User user, int limit, int offset) {
    List<String> newUris = search(c.getId(), null, null, user, null, limit, 0).getResults();
    c.getImages().clear();
    for (String s : newUris) {
      c.getImages().add(URI.create(s));
    }
    return c;
  }

  /**
   * Retrieve all items filtered by query
   *
   * @param user
   * @param q
   * @return
   * @throws ImejiException
   */
  public List<Item> searchAndRetrieve(URI containerUri, SearchQuery q, SortCriterion sort,
      User user, String spaceId, int offset, int size) throws ImejiException, IOException {
    List<Item> itemList = new ArrayList<Item>();
    try {
      List<String> results =
          search(containerUri, q, sort, user, spaceId, size, offset).getResults();
      itemList = (List<Item>) retrieveBatch(results, -1, 0, user);
    } catch (Exception e) {
      throw new UnprocessableError("Cannot retrieve items:", e);
    }
    return itemList;
  }

  /**
   * Set the status of a {@link List} of {@link Item} to released
   *
   * @param l
   * @param user
   * @param defaultLicens
   * @throws ImejiException
   */
  public void release(List<Item> l, User user, License defaultLicense) throws ImejiException {
    Collection<Item> items = filterItemsByStatus(l, Status.PENDING);
    for (Item item : items) {
      prepareRelease(item, user);
      if (defaultLicense != null && LicenseUtil.getActiveLicense(item) == null) {
        item.setLicenses(Arrays.asList(defaultLicense.clone()));
      }
    }
    updateBatch(items, user);
  }

  /**
   * Release the items with the current default license
   * 
   * @param l
   * @param user
   * @throws ImejiException
   */
  public void releaseWithDefaultLicense(List<Item> l, User user) throws ImejiException {
    this.release(l, user, itemController.getDefaultLicense());
  }

  /**
   * Set the status of a {@link List} of {@link Item} to withdraw and delete its files from the
   * {@link Storage}
   *
   * @param l
   * @param comment
   * @throws ImejiException
   */
  public void withdraw(List<Item> l, String comment, User user) throws ImejiException {
    Collection<Item> items = filterItemsByStatus(l, Status.RELEASED);
    for (Item item : items) {
      prepareWithdraw(item, comment);
    }
    updateBatch(items, user);
    for (Item item : items) {
      removeFileFromStorage(item);
    }
  }

  /**
   * Reindex all items
   * 
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws ImejiException {
    LOGGER.info("Indexing Items...");
    ElasticIndexer indexer = new ElasticIndexer(index, ElasticTypes.items, ElasticService.ANALYSER);
    LOGGER.info("Retrieving Items...");
    List<Item> items = (List<Item>) retrieveAll(Imeji.adminUser);
    LOGGER.info("+++ " + items.size() + " items to index +++");
    indexer.indexBatch(items);
    LOGGER.info("Items reindexed!");
  }

  /**
   * Copy the value of the contentVO to the item
   *
   * @param item
   * @param content
   * @return
   */
  private Item copyContent(Item item, ContentVO content) {
    item.setContentId(content.getId().toString());
    item.setChecksum(content.getChecksum());
    item.setFiletype(content.getMimetype());
    item.setFileSize(content.getFileSize());
    item.setFullImageUrl(URI.create(content.getOriginal()));
    item.setWebImageUrl(URI.create(content.getPreview()));
    item.setThumbnailImageUrl(URI.create(content.getThumbnail()));
    return item;
  }

  /**
   * Update the fulltext and the technical metadata of all items
   * 
   * @throws ImejiException
   */
  public void extractFulltextAndTechnicalMetadataForAllItems() throws ImejiException {
    List<Item> allItems = (List<Item>) retrieveAll(Imeji.adminUser);
    int count = 1;
    int countPart = 1;
    List<Item> itemToUpdate = new ArrayList<>();
    List<ContentVO> contents = new ArrayList<>();
    ContentController contentController = new ContentController();
    for (Item item : allItems) {
      try {
        ContentVO contentVO = toContentVO(item);
        if (contentVO.getId() == null) {
          contentVO = contentController.create(item, contentVO);
          item.setContentId(contentVO.getId().toString());
          itemToUpdate.add(item);
        }
        boolean extracted = contentController.extractContent(contentVO);
        count++;
        if (extracted) {
          countPart++;
          contents.add(contentVO);
          LOGGER.info(count + "/" + allItems.size() + " extracted (" + item.getIdString() + ")");
        } else {
          LOGGER
              .info(count + "/" + allItems.size() + " NOT extracted (" + item.getIdString() + ")");
        }
        if (countPart > 100) {
          // Update after 100 extraction to avoid to high memory consumption
          LOGGER.info("Updating " + contents.size() + " content with extracted infos...");
          contentController.updateBatch(contents);
          contents = new ArrayList<>();
          countPart = 1;
          LOGGER.info("... done!");
        }
      } catch (Exception e) {
        LOGGER.error("Error extracting fulltext/technical metadata for item " + item.getIdString(),
            e);
      }
    }
    LOGGER.info("Updating " + contents.size() + " content with extracted infos...");
    contentController.updateBatch(contents);
    LOGGER.info("... done!");
    LOGGER.info("Updating " + itemToUpdate.size() + " items with new Content");
    itemController.updateBatchForce(itemToUpdate, Imeji.adminUser);
    LOGGER.info("... done!");
  }

  /**
   * 
   * @param item
   * @return
   */
  private ContentVO toContentVO(Item item) {
    ContentVO contentVO = new ContentVO();
    if (StringHelper.isNullOrEmptyTrim(item.getContentId() != null)) {
      contentVO.setId(URI.create(item.getContentId()));
    }
    contentVO.setOriginal(item.getFullImageUrl().toString());
    contentVO.setPreview(item.getWebImageUrl().toString());
    contentVO.setThumbnail(item.getThumbnailImageUrl().toString());
    contentVO.setChecksum(item.getChecksum());
    contentVO.setFileSize(item.getFileSize());
    contentVO.setMimetype(item.getFiletype());
    return contentVO;
  }

  /**
   * Find a the creator of an item from a list of user. If not in list, return default admin user
   * 
   * @param item
   * @param allUsers
   * @return
   */
  private User getCreator(Item item, List<User> allUsers) {
    for (User user : allUsers) {
      if (user.getId().equals(item.getCreatedBy())) {
        return user;
      }
    }
    return Imeji.adminUser;
  }

  /**
   * Return a new filtered List of only item with the requested {@link Status}
   *
   * @param items
   * @param status
   * @return
   */
  private Collection<Item> filterItemsByStatus(List<Item> items, Status status) {
    return new ArrayList<>(Collections2.filter(items, new Predicate<Item>() {
      @Override
      public boolean apply(Item item) {
        return item.getStatus() == status;
      }
    }));
  }

  /**
   * Remove a file from the current {@link Storage}
   *
   * @param id
   */
  private void removeFileFromStorage(Item item) {
    ContentController contentController = new ContentController();
    try {
      contentController.delete(item.getContentId());
    } catch (Exception e) {
      LOGGER.error("error deleting file", e);
    }
  }


  /**
   * Return the external Url of the File
   *
   * @param fetchUrl
   * @param referenceUrl
   * @return
   */
  private String getExternalFileUrl(String fetchUrl, String referenceUrl) {
    return firstNonNullOrEmtpy(fetchUrl, referenceUrl);
  }

  private String firstNonNullOrEmtpy(String... strs) {
    if (strs == null) {
      return null;
    }
    for (String str : strs) {
      if (str != null && !"".equals(str.trim())) {
        return str;
      }
    }
    return null;
  }

  /**
   * True if the file must be download in imeji (i.e fetchurl is defined)
   *
   * @param fetchUrl
   * @return
   */
  private boolean downloadFile(String fetchUrl) {
    return !isNullOrEmpty(fetchUrl);
  }

  /**
   * Set the status of a {@link List} of {@link Item} to released
   *
   * @param l
   * @param user
   * @throws ImejiException
   */
  public void updateItemsProfile(List<Item> l, User user, String profileUri) throws ImejiException {
    for (Item item : l) {
      item.getMetadataSet().setProfile(URI.create(profileUri));
      item.getMetadataSet().getMetadata().clear();
    }
    updateBatch(l, user);
  }


  /**
   * Throws an {@link Exception} if the file cannot be uploaded. The validation will only occur when
   * the file has been stored locally)
   *
   * @throws ImejiException
   * @throws UnprocessableError
   */
  private void validateChecksum(URI collectionURI, File file, Boolean isUpdate)
      throws UnprocessableError, ImejiException {
    if (checksumExistsInCollection(collectionURI, StorageUtils.calculateChecksum(file))) {
      throw new UnprocessableError((!isUpdate)
          ? "Same file already exists in the collection (with same checksum). Please choose another file."
          : "Same file already exists in the collection or you are trying to upload same file for the item (with same checksum). Please choose another file.");
    }
  }

  /**
   * True if the checksum already exists within another {@link Item} in this {@link CollectionImeji}
   *
   * @param filename
   * @return
   */
  private boolean checksumExistsInCollection(URI collectionId, String checksum) {
    Search s = SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.JENA);
    return s.searchString(JenaCustomQueries.selectItemByChecksum(collectionId, checksum), null,
        null, 0, -1).getNumberOfRecords() > 0;
  }


  /**
   * Read a file from its url
   *
   * @param tmp
   * @param url
   * @return
   * @throws UnprocessableError
   */
  private File readFile(String url) throws UnprocessableError {
    try {
      StorageController sController = new StorageController("external");
      File tmp = TempFileUtil.createTempFile("createOrUploadWithExternalFile", null);
      sController.read(url, new FileOutputStream(tmp), true);
      return tmp;
    } catch (Exception e) {
      throw new UnprocessableError(e.getLocalizedMessage());
    }
  }
}
