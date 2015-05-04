/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.upload;

import com.ocpsoft.pretty.PrettyContext;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchType;
import de.mpg.imeji.logic.search.SearchFactory;
import de.mpg.imeji.logic.search.query.SPARQLQueries;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.presentation.collection.CollectionBean;
import de.mpg.imeji.presentation.history.HistoryUtil;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ObjectLoader;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import static de.mpg.imeji.logic.notification.CommonMessages.getSuccessCollectionDeleteMessage;

/**
 * Bean for the upload page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "UploadBean")
@ViewScoped
public class UploadBean implements Serializable {
	private static final long serialVersionUID = -2731118794797476328L;
	private static Logger logger = Logger.getLogger(UploadBean.class);
	private CollectionImeji collection = new CollectionImeji();
	private int collectionSize = 0;
	private String id;
	private String localDirectory = null;
	private String externalUrl;
	@ManagedProperty(value = "#{SessionBean.user}")
	private User user;
	private boolean recursive;

	/**
	 * Construct the Bean and initalize the pages
	 * 
	 * @throws Exception
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public UploadBean() {
	}

	/**
	 * Method checking the url parameters and triggering then the
	 * {@link UploadBean} methods
	 * 
	 * @throws UnprocessableError
	 * 
	 * @throws Exception
	 */
	@PostConstruct
	public void status() {
		readId();
		try {
			loadCollection();
			if (UrlHelper.getParameterBoolean("init")) {
				((UploadSession) BeanHelper.getSessionBean(UploadSession.class))
						.reset();
				externalUrl = null;
				localDirectory = null;
			} else if (UrlHelper.getParameterBoolean("start")) {
				upload();
			} else if (UrlHelper.getParameterBoolean("done")) {
				((UploadSession) BeanHelper.getSessionBean(UploadSession.class))
						.resetProperties();
			} else {
				BeanHelper.error("I can not get to the collection id ");
			}

		} catch (Exception e) {
			BeanHelper.error(e.getLocalizedMessage());
		}

	}

	/**
	 * Read the id of the collection from the url
	 */
	private void readId() {
		URI uri = HistoryUtil.extractURI(PrettyContext.getCurrentInstance()
				.getRequestURL().toString());
		if (uri != null)
			this.id = ObjectHelper.getId(uri);
	}

	/**
	 * Start the Upload of the items
	 * 
	 * @throws Exception
	 */
	public void upload() {
		HttpServletRequest req = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if (isMultipart) {
			// Parse the request
			try {
				ServletFileUpload upload = new ServletFileUpload();
				FileItemIterator iter = upload.getItemIterator(req);
				while (iter.hasNext()) {
					FileItemStream fis = iter.next();

					InputStream stream = fis.openStream();
					if (!fis.isFormField()) {
						File tmp = createTmpFile(fis.getName());
						try {
							writeInTmpFile(tmp, stream);
							uploadFile(tmp, fis.getName());
						} finally {
							stream.close();
							FileUtils.deleteQuietly(tmp);
						}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Upload all Files from a directory
	 * 
	 * @param path
	 * @throws Exception
	 */
	public String uploadFromLocalDirectory() throws Exception {
		try {
			File dir = new File(localDirectory);
			int i = 0;
			if (dir.isDirectory()) {
				for (File f : FileUtils.listFiles(dir, null, recursive)) {
					uploadFile(f, f.getName());
					i++;
				}
			}
			BeanHelper.info(i + " files uploaded from " + localDirectory);
		} catch (Exception e) {
			BeanHelper.error(e.getMessage());
		}
		return "pretty:";
	}

	/**
	 * Upload a file from the web
	 * 
	 * @return
	 * @throws Exception
	 */
	public String uploadFromLink() throws Exception {
		try {
			URL url = new URL(URLDecoder.decode(externalUrl, "UTF-8"));
			File tmp = createTmpFile(findFileName(url));
			try {
				StorageController externalController = new StorageController(
						"external");
				FileOutputStream fos = new FileOutputStream(tmp);
				externalController.read(url.toString(), fos, true);
				uploadFile(tmp, findFileName(url));
				externalUrl = null;
			} catch (Exception e) {
				getfFiles().add(e.getMessage() + ": " + findFileName(url));
			} finally {
				FileUtils.deleteQuietly(tmp);
			}
		} catch (Exception e) {
			logger.error("Error uploading file from link: " + externalUrl, e);
			BeanHelper.error(e.getMessage());
		}
		return "pretty:";
	}

	/**
	 * Find in the url the filename
	 * 
	 * @param url
	 * @return
	 */
	private String findFileName(URL url) {
		String name = FilenameUtils.getName(url.getPath());
		if (isWellFormedFileName(name))
			return name;
		name = FilenameUtils.getName(url.toString());
		if (isWellFormedFileName(name))
			return name;
		return FilenameUtils.getName(url.getPath());
	}

	/**
	 * true if the filename is well formed, i.e. has an extension
	 * 
	 * @param filename
	 * @return
	 */
	private boolean isWellFormedFileName(String filename) {
		return FilenameUtils.wildcardMatch(filename, "*.???")
				|| FilenameUtils.wildcardMatch(filename, "*.??")
				|| FilenameUtils.wildcardMatch(filename, "*.?");
	}

	/**
	 * Create a tmp file with the uploaded file
	 * 
	 * @param fio
	 * @return
	 */
	private File createTmpFile(String title) {
		try {
			return File.createTempFile("upload",
					"." + FilenameUtils.getExtension(title));
		} catch (Exception e) {
			throw new RuntimeException("Error creating a temp file", e);
		}
	}

	/**
	 * Write an {@link InputStream} in a {@link File}
	 * 
	 * @param tmp
	 * @param fis
	 * @return
	 * @throws IOException
	 */
	private File writeInTmpFile(File tmp, InputStream fis) throws IOException {
		FileOutputStream fos = new FileOutputStream(tmp);
		try {
			StorageUtils.writeInOut(fis, fos, true);
			return tmp;
		} catch (Exception e) {
			throw new RuntimeException(
					"Error writing uploaded File in temp file", e);
		} finally {
			fos.close();
			fis.close();
		}
	}

	/**
	 * Throws an {@link Exception} if the file ca be upload. Works only if the
	 * file has an extension (therefore, for file without extension, the
	 * validation will only occur when the file has been stored locally)
	 */
	private void validateName(File file, String title) {
		if (StorageUtils.hasExtension(title)) {
			if (isCheckNameUnique()) {
				// if the checkNameUnique is checked, check that two files with
				// the same name is not possible
				if (!((isImportImageToFile() || isUploadFileToItem()))
						&& filenameExistsInCollection(title))
					throw new RuntimeException(
							"There is already at least one item with the filename "
									+ FilenameUtils.getBaseName(title));
			}
			StorageController sc = new StorageController();
			String guessedNotAllowedFormat = sc.guessNotAllowedFormat(file);
			if (StorageUtils.BAD_FORMAT.equals(guessedNotAllowedFormat)) {
				SessionBean sessionBean = (SessionBean) BeanHelper
						.getSessionBean(SessionBean.class);
				throw new RuntimeException(
						sessionBean.getMessage("upload_format_not_allowed")
								+ " (" + guessedNotAllowedFormat + ")");
			}
		}
	}

	/**
	 * Upload one File and create the {@link de.mpg.imeji.logic.vo.Item}
	 * 
	 * @param bytes
	 * @throws Exception
	 */
	private Item uploadFile(File fileUploaded, String title) {
		try {

			String calculatedExtension = StorageUtils
					.guessExtension(fileUploaded);
			File file = fileUploaded;

			// TODO: not certain we should change the extensions even if these
			// are not provided! Thus commented at the moment
			// if (!StorageUtils.hasExtension(title)) {
			// title += "."+calculatedExtension;
			// }

			if (!fileUploaded.getName().endsWith(calculatedExtension)) {
				file = new File(file.getName() + calculatedExtension);
				FileUtils.moveFile(fileUploaded, file);
			}

			validateName(file, title);

			Item item = null;
			ItemController controller = new ItemController();
			if (isImportImageToFile()) {
				item = controller.updateFile(findItemByFileName(title), file,
						user);
			} else if (isUploadFileToItem()) {
				item = controller.updateThumbnail(findItemByFileName(title),
						file, user);
			} else {
				item = controller.createWithFile(null, file, title, collection,
						user);
			}
			getsFiles().add(item);
			return item;
		} catch (Exception e) {
			getfFiles().add(
					" File " + title + " not uploaded: "
							+ e.getLocalizedMessage());
			logger.error("Error uploading item: ", e);
			return null;
		}
	}

	/**
	 * Search for an item in the current collection with the same filename. The
	 * filename must be unique!
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	private Item findItemByFileName(String filename) throws Exception {
		Search s = SearchFactory.create(SearchType.ITEM);
		List<String> sr = s.searchSimpleForQuery(
				SPARQLQueries.selectContainerItemByFilename(collection.getId(),
						FilenameUtils.getBaseName(filename))).getResults();
		if (sr.size() == 0)
			throw new RuntimeException("No item found with the filename "
					+ FilenameUtils.getBaseName(filename));
		if (sr.size() > 1)
			throw new RuntimeException("Filename "
					+ FilenameUtils.getBaseName(filename) + " not unique ("
					+ sr.size() + " found).");
		return ObjectLoader.loadItem(URI.create(sr.get(0)), user);
	}

	/**
	 * True if the filename is already used by an {@link Item} in this
	 * {@link CollectionImeji}
	 * 
	 * @param filename
	 * @return
	 */
	private boolean filenameExistsInCollection(String filename) {
		Search s = SearchFactory.create(SearchType.ITEM);
		// filename =
		// org.apache.xerces.impl.xpath.regex.REUtil.quoteMeta(filename);
		// filename = Pattern.quote(FilenameUtils.removeExtension(filename));
		// filename = filename.replace(")", "\\u+0029").replace("(",
		// "\\u+0028");
		return s.searchSimpleForQuery(
				SPARQLQueries.selectContainerItemByFilename(collection.getId(),
						FilenameUtils.getBaseName(filename)))
				.getNumberOfRecords() > 0;
	}

	/**
	 * Load the collection
	 * 
	 * @throws Exception
	 * 
	 * @throws Exception
	 */
	public void loadCollection() throws Exception {
		SessionBean sessionBean = (SessionBean) BeanHelper
				.getSessionBean(SessionBean.class);
		if (id != null) {
			collection = ObjectLoader.loadCollectionLazy(
					ObjectHelper.getURI(CollectionImeji.class, id), user);
			isDiscaded();
			if (collection != null && getCollection().getId() != null) {
				ItemController ic = new ItemController();
				collectionSize = ic.countContainerSize(collection);
			}
		} else {
			BeanHelper.error(sessionBean.getLabel("error") + "No ID in URL");
			throw new RuntimeException();
		}
	}

	/**
	 * True if the {@link CollectionImeji} is discaded
	 * 
	 * @throws UnprocessableError
	 */
	private void isDiscaded() throws UnprocessableError {
		if (collection.getStatus().equals(Status.WITHDRAWN)) {
			SessionBean sessionBean = (SessionBean) BeanHelper
					.getSessionBean(SessionBean.class);
			throw new UnprocessableError(
					sessionBean.getMessage("error_collection_discarded_upload"));
		}
	}

	/**
	 * release the {@link CollectionImeji}
	 * 
	 * @return
	 */
	public String release() {
		CollectionController cc = new CollectionController();
		SessionBean sessionBean = (SessionBean) BeanHelper
				.getSessionBean(SessionBean.class);
		try {
			cc.release(collection, user);
			BeanHelper.info(sessionBean
					.getMessage("success_collection_release"));
		} catch (Exception e) {
			BeanHelper
					.error(sessionBean.getMessage("error_collection_release"));
			BeanHelper.error(e.getMessage());
		}
		return "pretty:";
	}

	/**
	 * Delete the {@link CollectionImeji}
	 * 
	 * @return
	 */
	public String delete() {
		CollectionController cc = new CollectionController();
		SessionBean sessionBean = (SessionBean) BeanHelper
				.getSessionBean(SessionBean.class);
		try {
			cc.delete(collection, sessionBean.getUser());
			BeanHelper.info(getSuccessCollectionDeleteMessage(collection
					.getMetadata().getTitle(), sessionBean));
		} catch (Exception e) {
			BeanHelper.error(sessionBean.getMessage("error_collection_delete"));
			logger.error("Error delete collection", e);
		}
		return "pretty:collections";
	}

	/**
	 * Discard the {@link CollectionImeji} of this {@link CollectionBean}
	 * 
	 * @return
	 * @throws Exception
	 */
	public String withdraw() throws Exception {
		CollectionController cc = new CollectionController();
		SessionBean sessionBean = (SessionBean) BeanHelper
				.getSessionBean(SessionBean.class);
		try {
			cc.withdraw(collection, sessionBean.getUser());
			BeanHelper.info(sessionBean
					.getMessage("success_collection_withdraw"));
		} catch (Exception e) {
			BeanHelper.error(sessionBean
					.getMessage("error_collection_withdraw"));
			BeanHelper.error(e.getMessage());
			logger.error("Error discarding collection:", e);
		}
		return "pretty:";
	}

	public CollectionImeji getCollection() {
		return collection;
	}

	public void setCollection(CollectionImeji collection) {
		this.collection = collection;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public int getCollectionSize() {
		return collectionSize;
	}

	public void setCollectionSize(int collectionSize) {
		this.collectionSize = collectionSize;
	}

	public String getExternalUrl() {
		return externalUrl;
	}

	public void setExternalUrl(String externalUrl) {
		this.externalUrl = externalUrl;
	}

	public List<String> getfFiles() {
		return ((UploadSession) BeanHelper.getSessionBean(UploadSession.class))
				.getfFiles();
	}

	public List<Item> getsFiles() {
		return ((UploadSession) BeanHelper.getSessionBean(UploadSession.class))
				.getsFiles();
	}

	private boolean isCheckNameUnique() {
		return ((UploadSession) BeanHelper.getSessionBean(UploadSession.class))
				.isCheckNameUnique();
	}

	private boolean isImportImageToFile() {
		return ((UploadSession) BeanHelper.getSessionBean(UploadSession.class))
				.isImportImageToFile();
	}

	private boolean isUploadFileToItem() {
		return ((UploadSession) BeanHelper.getSessionBean(UploadSession.class))
				.isUploadFileToItem();
	}

	public String getDiscardComment() {
		return collection.getDiscardComment();
	}

	public void setDiscardComment(String comment) {
		collection.setDiscardComment(comment);
	}

	/**
	 * @return the localDirectory
	 */
	public String getLocalDirectory() {
		return localDirectory;
	}

	/**
	 * @param localDirectory
	 *            the localDirectory to set
	 */
	public void setLocalDirectory(String localDirectory) {
		this.localDirectory = localDirectory;
	}

	/**
	 * @return the recursive
	 */
	public boolean isRecursive() {
		return recursive;
	}

	/**
	 * @param recursive
	 *            the recursive to set
	 */
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

}
