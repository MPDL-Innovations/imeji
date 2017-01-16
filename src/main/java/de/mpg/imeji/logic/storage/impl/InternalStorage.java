/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.storage.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.UploadResult;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.internal.InternalStorageItem;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.storage.util.ImageMagickUtils;
import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * imeji internal {@link Storage}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class InternalStorage implements Storage {
  private static final long serialVersionUID = 7865121663793602621L;
  private static Logger LOGGER = Logger.getLogger(InternalStorage.class);
  private static final String name = "internal";
  protected InternalStorageManager manager;

  /**
   * Default Constructor
   */
  public InternalStorage() {
    try {
      manager = new InternalStorageManager();
    } catch (final Exception e) {
      throw new RuntimeException("Error initialising InternalStorageManager: ", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#upload(byte[])
   */
  @Override
  public UploadResult upload(String filename, File file, String collectionId) {
    final InternalStorageItem item = manager.createItem(file, filename, collectionId);
    return new UploadResult(item.getId(), item.getOriginalUrl(), item.getWebUrl(),
        item.getThumbnailUrl(), item.getFullUrl());
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#read(java.lang.String)
   */
  @Override
  public void read(String url, OutputStream out, boolean close) {
    final String path = manager.transformUrlToPath(url);
    try {
      final FileInputStream fis = new FileInputStream(path);
      StorageUtils.writeInOut(fis, out, close);
    } catch (final Exception e) {
      throw new RuntimeException("Error reading file " + path + " in internal storage: ", e);
    }
  }

  @Override
  public File read(String url) {
    final String path = manager.transformUrlToPath(url);
    File file;
    try {
      file = new File(path);
    } catch (final Exception e) {
      LOGGER.error("Error reading file " + url, e);
      throw new RuntimeException("Error reading file " + path + " in internal storage: ", e);
    }
    return file;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#delete(java.lang.String)
   */
  @Override
  public void delete(String id) {
    manager.removeItem(new InternalStorageItem(id));
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#update(java.lang.String, byte[])
   */
  @Override
  public void changeThumbnail(String url, File file) {
    try {
      manager.changeThumbnail(file, url);
    } catch (final IOException e) {
      throw new RuntimeException(
          "Error updating file " + manager.transformUrlToPath(url) + " in internal storage: ", e);
    }
  }

  @Override
  public void update(String url, File file) throws IOException {
    manager.replaceFile(url, file);

  }

  @Override
  public void rotate(String fullUrl, int degrees) throws IOException, Exception {
    final String thumbnailUrl = getThumbnailUrl(fullUrl);
    final String webUrl = getWebResolutionUrl(fullUrl);
    File thumbnail = read(thumbnailUrl);
    File web = read(webUrl);
    final File full = read(fullUrl);

    if (ImageMagickUtils.jpegtranEnabled) {
      ImageMagickUtils.rotateJPEG(thumbnail, degrees);
      ImageMagickUtils.rotateJPEG(web, degrees);
      ImageMagickUtils.rotateJPEG(full, degrees);
    } else {
      ImageUtils.rotate(full, degrees);
      web = ImageUtils.resizeJPEG(full, FileResolution.WEB);
      thumbnail = ImageUtils.resizeJPEG(full, FileResolution.THUMBNAIL);
      update(webUrl, web);
      update(thumbnailUrl, thumbnail);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getAdminstrator()
   */
  @Override
  public StorageAdministrator getAdministrator() {
    return manager.getAdministrator();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getCollectionId(java.lang.String)
   */
  @Override
  public String getCollectionId(String url) {
    return URI.create(url).getPath().replace(URI.create(manager.getStorageUrl()).getPath(), "")
        .split("/", 2)[0];
  }


  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#readFileStringContent(java.lang.String)
   */
  @Override
  public String readFileStringContent(String url) {
    final String pathString = manager.transformUrlToPath(url);
    final Path path = Paths.get(pathString);
    String stringFromFile = "";
    try {
      stringFromFile = new String(Files.readAllBytes(path));
    } catch (final Exception e) {
      stringFromFile = "";
    }
    return stringFromFile;
  }

  @Override
  public String getStorageId(String url) {
    return manager.getStorageId(url);
  }

  private String getThumbnailUrl(String originalUrl) {
    return originalUrl.replace("/full/", "/thumbnail/");
  }

  private String getWebResolutionUrl(String originalUrl) {
    return originalUrl.replace("/full/", "/web/");
  }

  @Override
  public int getImageWidth(String url) throws IOException {
    final File file = read(url);
    return ImageUtils.getImageWidth(file);
  }

  @Override
  public int getImageHeight(String url) throws IOException {
    final File file = read(url);
    return ImageUtils.getImageHeight(file);
  }
}
