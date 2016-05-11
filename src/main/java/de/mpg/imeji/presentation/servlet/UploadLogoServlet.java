package de.mpg.imeji.presentation.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.TypeNotAllowedException;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.presentation.beans.ContainerEditorSession;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Servlet to upload logo to the ContainerEditorSession
 * 
 * @author bastiens
 *
 */
public class UploadLogoServlet extends HttpServlet {
  private static final long serialVersionUID = 8271914066699208201L;
  private static final Logger LOGGER = Logger.getLogger(UploadLogoServlet.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Only post supported");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (!isLoggedIn(req)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Please login");
    } else {
      try {
        File f = uploadLogo(req, resp);
        getContainerEditorSession(req)
            .setUploadedLogoPath(f != null && f.exists() ? f.getAbsolutePath() : null);
      } catch (FileUploadException | TypeNotAllowedException e) {
        LOGGER.error("Error uploading logo", e);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error uploading logo");
      }
    }
  }

  private File uploadLogo(HttpServletRequest request, HttpServletResponse response)
      throws FileUploadException, TypeNotAllowedException, IOException {
    File tmp = null;
    boolean isMultipart = ServletFileUpload.isMultipartContent(request);
    if (isMultipart) {
      ServletFileUpload upload = new ServletFileUpload();
      FileItemIterator iter = upload.getItemIterator(request);
      while (iter.hasNext()) {
        FileItemStream fis = iter.next();
        tmp = StorageUtils.toFile(fis.openStream());
        if (StorageUtils.getMimeType(tmp).contains("image")) {
          return tmp;
        }
      }
    }
    return null;
  }

  /**
   * Return the {@link SessionBean} form the {@link HttpSession}
   *
   * @param req
   * @return
   */
  private ContainerEditorSession getContainerEditorSession(HttpServletRequest req) {
    return (ContainerEditorSession) req.getSession(true)
        .getAttribute(ContainerEditorSession.class.getSimpleName());
  }

  private boolean isLoggedIn(HttpServletRequest req) {
    return ((SessionBean) req.getSession(true).getAttribute(SessionBean.class.getSimpleName()))
        .getUser() != null;
  }

}
