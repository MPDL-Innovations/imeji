/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.ImejiSPARQL;
import de.mpg.imeji.logic.auth.AuthenticationFactory;
import de.mpg.imeji.logic.auth.Authorization;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.notification.NotificationUtils;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchFactory;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.impl.ExternalStorage;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.PropertyReader;

/**
 * The Servlet to Read files from imeji {@link Storage}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class FileServlet extends HttpServlet {
  private static final long serialVersionUID = 5502546330318540997L;
  private static Logger logger = Logger.getLogger(FileServlet.class);
  private StorageController storageController;
  private Authorization authorization;
  private Navigation navivation;
  private String domain;
  private String digilibUrl;
  /**
   * If property imeji.storage.path= /data/imeji/files/ then, internalStorageRoot = files
   */
  private String internalStorageRoot;
  /**
   * The path for this servlet as defined in the web.xml
   */
  public static final String SERVLET_PATH = "file";

  @Override
  public void init() {
    try {
      storageController = new StorageController();
      logger.info("File Servlet initialized");
      authorization = new Authorization();
      navivation = new Navigation();
      domain = StringHelper.normalizeURI(navivation.getDomain());
      domain = domain.substring(0, domain.length() - 1);
      digilibUrl = PropertyReader.getProperty("digilib.imeji.instance.url");
      if (digilibUrl != null && !digilibUrl.isEmpty()) {
        digilibUrl = StringHelper.normalizeURI(digilibUrl);
      }
      InternalStorageManager ism = new InternalStorageManager();
      internalStorageRoot =
          FilenameUtils.getBaseName(FilenameUtils.normalizeNoEndSeparator(ism.getStoragePath()));
    } catch (Exception e) {
      throw new RuntimeException("Image servlet not initialized! " + e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String url = req.getParameter("id");
    boolean download = "1".equals(req.getParameter("download"));
    if (url == null) {
      // if the id parameter is null, interpret the whole url as a direct
      // to the file (can only work if the
      // internal storage is used)
      url = domain + req.getRequestURI();
    }
    resp.setContentType(StorageUtils.getMimeType(StringHelper.getFileExtension(url)));
    SessionBean session = getSession(req);
    User user; 
    try {
      user =  getUser(req, session);
      if ("NO_THUMBNAIL_URL".equals(url)) {
        ExternalStorage eStorage = new ExternalStorage();
        eStorage.read("http://localhost:8080/imeji/resources/icon/empty.png",
            resp.getOutputStream(), true);
      } else {
        if (download) {
          downloadFile(resp, url, session, user);
        } else {
          readFile(url, resp, false, user);
        }
      }
    } catch (Exception e) {
      if (e instanceof NotAllowedError) {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN,
            "imeji security: You are not allowed to view this file");
      } else if (e instanceof AuthenticationError) {
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            "imeji security: You need to be signed-in to view this file.");
      } else if (e instanceof NotFoundException) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND,
            "The resource you are trying to retrieve does not exist!");
      } else {
        logger.error(e.getMessage());
        if (!resp.isCommitted()) {
          resp.sendError(422, "Unprocessable entity!");
        }

        /*
         * ExternalStorage eStorage = new ExternalStorage(); eStorage.read(domain +
         * "/imeji/resources/icon/empty.png", resp.getOutputStream(), true);
         */}
    }
  }

  private void downloadFile(HttpServletResponse resp, String url, SessionBean session, User user)
      throws Exception {
    boolean iSpaceLogo =
        url.contains("/file/spaces") || url.matches(".*/file/\\w*[^/]/thumbnail/.*\\..+");
    resp.setHeader("Content-disposition", "attachment;");
    boolean isExternalStorage = false;
    if (!iSpaceLogo) {
      Item fileItem = getItem(url, user);
      NotificationUtils.notifyByItemDownload(user, fileItem, session);
      isExternalStorage = StringHelper.isNullOrEmptyTrim(fileItem.getStorageId());
    }
    readFile(url, resp, isExternalStorage, user);

  }

  /**
   * Read a File and write it back in the response
   * 
   * @param url
   * @param resp
   * @param isExternalStorage
   * @throws ImejiException
   * @throws IOException
   */
  private void readFile(String url, HttpServletResponse resp, boolean isExternalStorage, User user)
      throws ImejiException, IOException {
    if (isExternalStorage) {
      readExternalFile(url, resp);
    } else {
      readStorageFile(url, resp, user);
    }
  }

  /**
   * Read a File from the current storage
   * 
   * @param url
   * @param resp
   * @throws ImejiException
   * @throws IOException
   */
  private void readStorageFile(String url, HttpServletResponse resp, User user)
      throws ImejiException, IOException {
    checkSecurity(url, user);
    storageController.read(url, resp.getOutputStream(), true);
  }

  /**
   * Exeption if the user is not allowed to read the file
   * 
   * @param url
   * @param user
   * @return
   * @throws NotAllowedError
   */
  private void checkSecurity(String url, User user) throws NotAllowedError {
    URI uri = getCollectionURI(url);
    if (authorization.read(user, uri) || isPublicCollection(uri)) {
      // ok!
    } else {
      throw new NotAllowedError("You are not allowed to read this file");
    }
  }


  /**
   * True if the collection is public
   * 
   * @param collectionId
   * @return
   */
  private boolean isPublicCollection(URI collectionId) {
    List<String> r =
        ImejiSPARQL.exec(JenaCustomQueries.selectCollectionStatus(collectionId.toString()), null);
    return !r.isEmpty() && r.get(0).equals(Status.RELEASED.getUriString());
  }


  /**
   * Read an external (i.e not in the current storage) file
   * 
   * @param url
   * @param resp
   * @throws ImejiException
   * @throws IOException
   */
  private void readExternalFile(String url, HttpServletResponse resp)
      throws ImejiException, IOException {
    ExternalStorage eStorage = new ExternalStorage();
    eStorage.read(url, resp.getOutputStream(), true);
  }



  /**
   * Return the {@link User} of the request. Check first is a user is send with the request. If not,
   * check in the the session.
   * 
   * @param req
   * @return
   * @throws AuthenticationError 
   */
  private User getUser(HttpServletRequest req, SessionBean session) throws AuthenticationError {

    User user = AuthenticationFactory.factory(req).doLogin();
    if (user != null) {
      return user;
    }
    if (session != null) {
      return session.getUser();
    }
    return null;

  }

  /**
   * Return the uri of the {@link CollectionImeji} of the file with this url
   * 
   * @param url
   * @return
   */
  private URI getCollectionURI(String url) {
    String id = storageController.getCollectionId(url);
    if (id != null) {
      return ObjectHelper.getURI(CollectionImeji.class, id);
    } else {
      Search s = SearchFactory.create();
      List<String> r =
          s.searchString(JenaCustomQueries.selectCollectionIdOfFile(url), null, null, 0, -1)
              .getResults();
      if (!r.isEmpty()) {
        return URI.create(r.get(0));
      } else {
        return null;
      }
    }
  }

  /**
   * Find the {@link Item} which is owner of the file
   * 
   * @param url
   * @return
   * @throws Exception
   */
  private Item getItem(String url, User user) throws Exception {
    Search s = SearchFactory.create();
    List<String> r =
        s.searchString(JenaCustomQueries.selectItemIdOfFile(url), null, null, 0, -1).getResults();
    if (!r.isEmpty() && r.get(0) != null) {
      ItemController c = new ItemController();
      return c.retrieveLazy(URI.create(r.get(0)), user);
    } else {
      throw new NotFoundException("Can not find the resource requested");
    }
  }

  /**
   * Return the {@link SessionBean} form the {@link HttpSession}
   * 
   * @param req
   * @return
   */
  private SessionBean getSession(HttpServletRequest req) {
    return (SessionBean) req.getSession(true).getAttribute(SessionBean.class.getSimpleName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // No post action
    return;
  }
}
