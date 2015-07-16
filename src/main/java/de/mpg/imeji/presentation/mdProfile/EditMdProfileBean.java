package de.mpg.imeji.presentation.mdProfile;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.VocabularyHelper;

/**
 * Java Bean for the edit metadata Profile page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class EditMdProfileBean extends MdProfileBean {
  private SessionBean session;
  private boolean init = false;
  private String colId = null;
  private static Logger logger = Logger.getLogger(EditMdProfileBean.class);
  private VocabularyHelper vocabularyHelper;
  private CollectionImeji collection;

  /**
   * Constructor
   */
  public EditMdProfileBean() {
    super();
    session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    readUrl();
  }

  @Override
  public String getInit() {

    readUrl();
    vocabularyHelper = new VocabularyHelper();
    if (init) {
      // set object to null (since this is a session bean)
      setProfile(null);
      setCollection(null);
      if (getId() != null) {
        try {
          if (colId != null) {
            // load the collection if provided in the url
            CollectionController cc = new CollectionController();
            setCollection(cc.retrieve(colId, session.getUser()));
          }
          // load the profile
          ProfileController pc = new ProfileController();
          setProfile(pc.retrieve(getId(), session.getUser()));
        } catch (ImejiException e) {
          BeanHelper.error(e.getMessage());
        }
      }
      init = false;
    }
    super.getInit();
    return "";
  }

  /**
   * Parse the url parameters
   */
  public void readUrl() {
    colId = UrlHelper.getParameterValue("col");
    init = UrlHelper.getParameterBoolean("init");
  }

  /**
   * Method when cancel button is clicked
   * 
   * @return
   * @throws IOException
   */
  public String cancel() throws IOException {
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    if (colId != null)
      FacesContext
          .getCurrentInstance()
          .getExternalContext()
          .redirect(
              navigation.getCollectionUrl() + colId + "/" + navigation.getInfosPath() + "?init=1");
    else {
      HistorySession history = (HistorySession) BeanHelper.getSessionBean(HistorySession.class);
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(history.getPreviousPage().getCompleteUrlWithHistory());
    }
    return "";
  }

  /**
   * Method when save button is clicked
   * 
   * @return
   * @throws IOException
   */
  public String save() throws IOException {
    getProfile().setStatements(getUnwrappedStatements());
    int pos = 0;
    // Set the position of the statement (used for the sorting later)
    for (Statement st : getProfile().getStatements()) {
      st.setPos(pos);
      pos++;
    }

    try {
      ProfileController profileController = new ProfileController();
      profileController.update(getProfile(), session.getUser());
      session.getProfileCached().clear();
      BeanHelper.info(session.getMessage("success_profile_save"));
      cancel();
    } catch (UnprocessableError e) {
      BeanHelper.error(session.getMessage(e.getMessage()));
    } catch (Exception e) {
      BeanHelper.error(session.getMessage("error_profile_save"), e.getMessage());
      logger.error(session.getMessage("error_profile_save"), e);
    }
    return "";
  }

  /**
   * Listener for the title input
   * 
   * @param event
   */
  public void titleListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue() != event.getOldValue()) {
      this.getProfile().setTitle(event.getNewValue().toString());
    }
  }

  /**
   * Listener for the description input
   * 
   * @param event
   */
  public void descriptionListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue() != event.getOldValue()) {
      this.getProfile().setTitle(event.getNewValue().toString());
    }
  }

  @Override
  protected String getNavigationString() {
    return session.getPrettySpacePage("pretty:editProfile");
  }

  /**
   * getter
   * 
   * @return
   */
  public String getColId() {
    return colId;
  }

  /**
   * setter
   * 
   * @param colId
   */
  public void setColId(String colId) {
    this.colId = colId;
  }

  /**
   * @return the vocabularyHelper
   */
  public VocabularyHelper getVocabularyHelper() {
    return vocabularyHelper;
  }

  /**
   * @param vocabularyHelper the vocabularyHelper to set
   */
  public void setVocabularyHelper(VocabularyHelper vocabularyHelper) {
    this.vocabularyHelper = vocabularyHelper;
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }
}
