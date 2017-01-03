package de.mpg.imeji.logic.item;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.service.ImejiServiceAbstract;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.util.LicenseUtil;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.MetadataUtil;
import de.mpg.imeji.logic.writer.WriterFacade;

/**
 * Resource controller for item
 *
 * @author saquet
 *
 */
public class ItemController extends ImejiServiceAbstract {
  private static final Logger LOGGER = Logger.getLogger(ItemService.class);
  private final ReaderFacade READER;
  private final WriterFacade WRITER;

  public ItemController() {
    READER = new ReaderFacade(Imeji.imageModel);
    WRITER = new WriterFacade(Imeji.imageModel);
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
    for (final Item item : items) {
      prepareCreate(item, user);
      item.setFilename(FilenameUtils.getName(item.getFilename()));
      item.setStatus(ic.getStatus());
      item.setCollection(ic.getId());
      ic.getImages().add(item.getId());
    }
    cleanItem(items);
    WRITER.create(J2JHelper.cast2ObjectList((List<?>) items), user);
    // Update the collection
    // cc.update(cc.retrieve(coll, user), Imeji.adminUser);
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
   * User ObjectLoader to load image
   *
   * @param imgUri
   * @return
   * @throws ImejiException
   */
  public Item retrieve(URI imgUri, User user) throws ImejiException {
    return (Item) READER.read(imgUri.toString(), user, new Item());
  }

  public Item retrieveLazy(URI imgUri, User user) throws ImejiException {
    return (Item) READER.readLazy(imgUri.toString(), user, new Item());
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
    final List<Item> items = uris2Items(uris, limit, offset);
    READER.read(J2JHelper.cast2ObjectList(items), user);
    return items;
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
    final List<Item> items = uris2Items(uris, limit, offset);
    READER.readLazy(J2JHelper.cast2ObjectList(items), user);
    return items;
  }

  /**
   * Update a {@link Collection} of {@link Item}
   *
   * @param items
   * @param user
   * @throws ImejiException
   */
  public void updateBatch(Collection<Item> items, User user) throws ImejiException {
    if (items != null && !items.isEmpty()) {
      for (final Item item : items) {
        prepareUpdate(item, user);
        item.setFilename(FilenameUtils.getName(item.getFilename()));
      }
      cleanItem(items);
      WRITER.update(J2JHelper.cast2ObjectList((List<?>) items), user, true);
    }
  }

  /**
   * Update without any validation and any operations on the data. WARNING: use with care. Invalid
   * data would overwrite valid data
   *
   * @param items
   * @param user
   * @throws ImejiException
   */
  public void updateBatchForce(Collection<Item> items, User user) throws ImejiException {
    cleanItem(items);
    WRITER.updateWithoutValidation(new ArrayList<>(items), user);
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
    WRITER.delete(new ArrayList<Object>(items), user);
  }

  /**
   * Clean the values of all {@link Metadata} of an {@link Item}
   *
   * @param l
   * @throws ImejiException
   */
  private void cleanItem(Collection<Item> l) {
    for (final Item item : l) {
      final List<Metadata> cleanMetadata =
          item.getMetadata().stream().filter(md -> !MetadataUtil.isEmpty(md))
              .map(md -> MetadataUtil.cleanMetadata(md)).collect(Collectors.toList());
      item.setMetadata(cleanMetadata);
      cleanLicenses(item);
    }
  }

  /**
   * Clean the licenses of the item
   *
   * @param item
   * @throws ImejiException
   */
  private void cleanLicenses(Item item) {
    final long start = System.currentTimeMillis();
    item.setLicenses(LicenseUtil.removeDuplicates(item.getLicenses()));
    final License active = LicenseUtil.getActiveLicense(item);
    if (active != null && !active.isEmtpy() && active.getStart() < 0) {
      active.setStart(start);
      if (item.getStatus().equals(Status.PENDING)) {
        item.setLicenses(Arrays.asList(active));
      }
      setLicensesEnd(item, active, start);
    } else if ((active == null || active.isEmtpy()) && item.getStatus().equals(Status.PENDING)) {
      item.setLicenses(new ArrayList<>());
    }
  }

  /**
   * Set the end of the licenses (normally, only one license shouldn't have any end)
   *
   * @param item
   * @param current
   * @param end
   */
  private void setLicensesEnd(Item item, License current, long end) {
    for (final License lic : item.getLicenses()) {
      if (lic.getEnd() < 0 && !lic.getName().equals(current.getName())) {
        lic.setEnd(end);
      }
    }
  }

  /**
   * Transform a list of uris into a list of Item
   *
   * @param uris
   * @param limit
   * @param offset
   * @return
   */
  private List<Item> uris2Items(List<String> uris, int limit, int offset) {
    List<String> retrieveUris;
    if (limit < 0) {
      retrieveUris = uris;
    } else {
      retrieveUris = uris.size() > 0 && limit > 0
          ? uris.subList(offset, getMin(offset + limit, uris.size())) : new ArrayList<String>();
    }
    final List<Item> items = new ArrayList<Item>();
    for (final String s : retrieveUris) {
      items.add((Item) J2JHelper.setId(new Item(), URI.create(s)));
    }
    return items;
  }
}
