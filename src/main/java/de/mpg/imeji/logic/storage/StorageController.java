/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License"). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE
 * or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at license/ESCIDOC.LICENSE.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft
 * für wissenschaftlich-technische Information mbH and Max-Planck-
 * Gesellschaft zur Förderung der Wissenschaft e.V.
 * All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.storage;

import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.presentation.util.PropertyReader;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;

/**
 * Controller for the {@link Storage} objects
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class StorageController implements Serializable {
	private static final long serialVersionUID = -2651970941029421673L;
	public static final String UPLOAD_BLACKLIST_PROPERTY = "imeji.upload.blacklist";
	public static final String IMEJI_STORAGE_NAME_PROPERTY = "imeji.storage.name";
	public static final String UPLOAD_WHITELIST_PROPERTY = "imeji.upload.whitelist";
	private Storage storage;



	private String formatWhiteList;
	private String formatBlackList;

	/**
	 * Create new {@link StorageController} for the {@link Storage} defined in
	 * imeji.properties
	 */
	public StorageController() {
		String name;
		try {
			name = PropertyReader.getProperty(IMEJI_STORAGE_NAME_PROPERTY);
			formatBlackList = PropertyReader.getProperty(UPLOAD_BLACKLIST_PROPERTY);
			formatWhiteList = PropertyReader.getProperty(UPLOAD_WHITELIST_PROPERTY);
		} catch (Exception e) {
			throw new RuntimeException("Error reading storage name property: ",
					e);
		}
		storage = StorageFactory.create(name);
	}

	/**
	 * Construct a {@link StorageController} for one {@link Storage}
	 * 
	 * @param name
	 *            - The name of the storage, as defined by getName() method
	 */
	public StorageController(String name) {
		storage = StorageFactory.create(name);
	}

	/**
	 * Call upload method of the controlled {@link Storage}
	 * 
	 * @param filename
	 * @param file
	 * @param collectionId
	 * @return
	 */
	public UploadResult upload(String filename, File file, String collectionId) {

		UploadResult result = storage.upload(filename, file, collectionId);
		try {
			result.setChecksum(calculateChecksum(file));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * Call read method of the controlled {@link Storage}
	 * 
	 * @param url
	 * @param out
	 */
	public void read(String url, OutputStream out, boolean close) {
		storage.read(url, out, close);
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
		storage.update(url, file);
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
	 * Calculate the Checksum of a byte array with MD5 algorithm displayed in
	 * Hexadecimal
	 * 
	 * @param bytes
	 * @return
	 * @throws IOException
	 */
	public String calculateChecksum(File file) throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			return DigestUtils.md5Hex(fis);
		} catch (Exception e) {
			throw new RuntimeException(
					"Error calculating the cheksum of the file: ", e);
		} finally {
			if (fis != null)
				fis.close();
		}
	}

	/**
	 * True if the file format related to the passed extension can be download
	 *
	 * @param extension
	 * @return
	 */
	public boolean isAllowedFormat(String extension) {
		// If no extension, not possible to recognized the format
		if ("".equals(extension.trim()))
			return false;
		// check in white list, if found then allowed
		for (String s : formatWhiteList.split(","))
			if (StorageUtils.compareExtension(extension, s.trim()))
				return true;
		// check black list, if found then forbidden
		for (String s : formatBlackList.split(","))
			if (StorageUtils.compareExtension(extension, s.trim()))
				return false;
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
}
