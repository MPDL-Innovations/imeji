/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.concurrency.locks.Lock;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.service.item.ItemService;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.metadata.editors.SingleEditor;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Metadata Editor for the detail item page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SingleEditorWrapper implements Serializable {
  private static final long serialVersionUID = 914870669221300081L;
  private Item item = null;
  private SingleEditor editor = null;
  private String toggleState = "displayMd";
  private int mdPosition = 0;
  private List<MetadataWrapper> metadataList;
  private User sessionUser;
  private Locale locale;
  private static final Logger LOGGER = Logger.getLogger(SingleEditorWrapper.class);

  /**
   * Constructor
   *
   * @param im
   * @param profile
   * @param pageUrl
   */
  public SingleEditorWrapper(Item im, User sessionUser, Locale locale) {
    this.item = im;
    this.sessionUser = sessionUser;
    this.locale = locale;
    init();
  }

  /**
   * Check in the url if the editor should be automatically shown
   *
   * @return
   */
  public String getCheckToggleState() {
    if (UrlHelper.getParameterBoolean("edit")) {
      showEditor();
    }
    return "";
  }

  /**
   * Initialize the page
   */
  private void init() {
    editor = new SingleEditor(item, null, sessionUser, locale);
    metadataList = new ArrayList<MetadataWrapper>();
    // metadataList.addAll(editor.getItems().get(0).asItem());
  }


  /**
   * Cancel the editing, and reset original values
   *
   * @return
   * @throws Exception
   */
  public String cancel() throws Exception {
    this.toggleState = "displayMd";
    Locks.unLock(new Lock(item.getId().toString(), sessionUser.getEmail()));
    reloadImage();
    editor = new SingleEditor(item, null, sessionUser, locale);
    return "";
  }

  /**
   * Reload the current image
   */
  private void reloadImage() {
    ItemService itemController = new ItemService();
    try {
      item = itemController.retrieve(item.getId(), sessionUser);
    } catch (Exception e) {
      BeanHelper.error("Error reload item" + e.getMessage());
      LOGGER.error("Error loading item", e);
    }
  }

  /**
   * Show the metadata editor
   *
   * @return
   */
  public String showEditor() {
    if (SecurityUtil.staticAuth().updateContent(sessionUser, item)) {
      this.toggleState = "editMd";
      try {
        Locks.lock(new Lock(item.getId().toString(), sessionUser.getEmail()));
      } catch (Exception e) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_editor_image_locked", locale));
        LOGGER.error("Error locking item", e);
      }
    } else {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_editor_not_allowed", locale));
    }
    return "";
  }

  public SingleEditor getEditor() {
    return editor;
  }

  public void setEditor(SingleEditor editor) {
    this.editor = editor;
  }

  public Item getImage() {
    return item;
  }

  public void setImage(Item item) {
    this.item = item;
  }

  public int getMdPosition() {
    return mdPosition;
  }

  public void setMdPosition(int mdPosition) {
    this.mdPosition = mdPosition;
  }

  public String getToggleState() {
    return toggleState;
  }

  public void setToggleState(String toggleState) {
    this.toggleState = toggleState;
  }

  public List<MetadataWrapper> getMetadataList() {
    return metadataList;
  }

  public void setMetadataList(List<MetadataWrapper> metadataList) {
    this.metadataList = metadataList;
  }

}
