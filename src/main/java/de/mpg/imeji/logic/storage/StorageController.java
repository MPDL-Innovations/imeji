package de.mpg.imeji.logic.storage;

import static de.mpg.imeji.logic.storage.util.StorageUtils.calculateChecksum;
import static de.mpg.imeji.logic.storage.util.StorageUtils.compareExtension;
import static de.mpg.imeji.logic.storage.util.StorageUtils.guessExtension;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * Controller for the {@link Storage} objects
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public final class StorageController implements Serializable {
  private static final long serialVersionUID = -2651970941029421673L;;
  public static final String IMEJI_STORAGE_NAME_PROPERTY = "imeji.storage.name";
  private final Storage storage;
  private final String formatWhiteList;
  private final String formatBlackList;
  private static final Logger LOGGER = Logger.getLogger(StorageController.class);

  /**
   * Create new {@link StorageController} for the {@link Storage} defined in imeji.properties
   *
   * @throws URISyntaxException
   * @throws IOException
   */
  public StorageController() {
    this(null);
  }

  /**
   * Construct a {@link StorageController} for one {@link Storage}
   *
   * @param name - The name of the storage, as defined by getName() method
   */
  public StorageController(String name) {
    try {
      if (name == null) {
        name = PropertyReader.getProperty(IMEJI_STORAGE_NAME_PROPERTY);
      }
    } catch (final Exception e) {
      LOGGER.error("Error initializing StorageController", e);
    }
    storage = StorageFactory.create(name);
    formatBlackList = Imeji.CONFIG.getUploadBlackList();
    formatWhiteList = Imeji.CONFIG.getUploadWhiteList();
  }

  /**
   * Call upload method of the controlled {@link Storage}
   *
   * @param filename
   * @param file
   * @param collectionId
   * @return
   * @throws ImejiException
   */
  public UploadResult upload(String filename, File file, String collectionId)
      throws ImejiException {
    filename = FilenameUtils.getName(filename);
    final UploadResult result = storage.upload(filename, file, collectionId);
    final File storageFile = storage.read(result.getOrginal());
    result.setChecksum(calculateChecksum(storageFile));
    result.setFileSize(storageFile.length());
    // If the file is an image, read the dimension of the image
    if (StorageUtils.getMimeType(storageFile).contains("image")) {
      final Dimension d = ImageUtils.getImageDimension(storageFile);
      if (d != null) {
        result.setHeight(d.height);
        result.setWidth(d.width);
      }
    }
    return result;
  }

  /**
   * Call read method of the controlled {@link Storage}
   *
   * @param url
   * @param out
   * @throws ImejiException
   */
  public void read(String url, OutputStream out, boolean close) throws ImejiException {
    storage.read(url, out, close);
  }

  /**
   * Return the file for the url
   *
   * @param url
   * @param out
   * @throws ImejiException
   */
  public File read(String url) throws ImejiException {
    return storage.read(url);
  }

  /**
   * Call delete method of the controlled {@link Storage}
   *
   * @param url
   */
  public void delete(String url) {
    storage.delete(url);
  }

  /**
   * Call update method of the controlled {@link Storage}
   *
   * @param url
   * @param bytes
   */
  public void update(String url, File file) {
    storage.changeThumbnail(url, file);
  }

  /**
   * Copya file and all resolution of this file to another collection
   * 
   * @param url
   * @param filename
   * @param collectionId
   * @return
   * @throws ImejiException
   */
  public UploadResult copy(String url, String collectionId) throws ImejiException {
    try {
      return storage.copy(url, collectionId);
    } catch (IOException e) {
      throw new ImejiException("Error copying storage file", e);
    }
  }

  /**
   * Return the {@link StorageAdministrator} of the current {@link Storage}
   *
   * @return
   */
  public StorageAdministrator getAdministrator() {
    return storage.getAdministrator();
  }

  /**
   * Return the id of the {@link CollectionImeji} of this file
   *
   * @return
   */
  public String getCollectionId(String url) {
    return storage.getCollectionId(url);
  }

  /**
   * Null if the file format related to the passed extension can be uploaded, not allowed file type
   * exception otherwise
   *
   * @param file
   * @return not allowed file format extension
   */
  public String guessNotAllowedFormat(File file) {
    boolean canBeUploaded = false;
    String guessedExtension = FilenameUtils.getExtension(file.getName());
    if (!"".equals(guessedExtension)) {
      canBeUploaded = isAllowedFormat(guessedExtension);
    }
    // In Any case check the extension by Tika results
    guessedExtension = guessExtension(file);

    // file can be uploaded only if both results are true
    canBeUploaded = canBeUploaded && isAllowedFormat(guessedExtension);

    return canBeUploaded ? guessedExtension : StorageUtils.BAD_FORMAT;
  }

  /**
   * Rotate the thumbnail, Web and full resolution
   *
   * @param degree @throws ImejiException @throws
   * @throws Exception
   * @throws IOException
   */
  public void rotate(String fullUrl, int degrees) throws IOException, Exception {
    storage.rotate(fullUrl, degrees);

  }

  /**
   * True if the file format related to the passed extension can be download
   *
   * @param extension
   * @return
   */
  private boolean isAllowedFormat(String extension) {
    // If no extension, not possible to recognized the format
    // Imeji will uprfont guess the extension for the uploaded file if it is
    // not provided
    // Thus this method is not public and cannot be used as public method
    if ("".equals(extension.trim())) {
      return false;
    }
    // check in white list, if found then allowed
    for (final String s : formatWhiteList.split(",")) {
      if (compareExtension(extension, s.trim())) {
        return true;
      }
    }
    // check black list, if found then forbidden
    for (final String s : formatBlackList.split(",")) {
      if (compareExtension(extension, s.trim())) {
        return false;
      }
    }
    // Not found in both list: if white list is empty, allowed
    return "".equals(formatWhiteList.trim());
  }

  /**
   * Get the {@link Storage} used by the {@link StorageController}
   *
   * @return
   */
  public Storage getStorage() {
    return storage;
  }

  /**
   * Call read method of the controlled {@link Storage}
   *
   * @param url
   * @param out
   * @throws ImejiException
   */
  public String readFileStringContent(String url) {
    return storage.readFileStringContent(url);
  }

  public String getFormatBlackList() {
    return formatBlackList;
  }

  public String getFormatWhiteList() {
    return formatWhiteList;
  }

}
