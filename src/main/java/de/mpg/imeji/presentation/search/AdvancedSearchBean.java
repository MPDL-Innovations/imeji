/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.beans.FileTypes.Type;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.filter.FiltersSession;
import de.mpg.imeji.presentation.lang.MetadataLabels;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Java bean for the advanced search page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AdvancedSearchBean {
  private SearchForm formular = null;
  // Menus
  private List<SelectItem> profilesMenu;
  private List<SelectItem> collectionsMenu;
  private List<SelectItem> operatorsMenu;
  private List<SelectItem> fileTypesMenu;
  private List<String> fileTypesSelected;
  /**
   * True if the query got an error (for instance wrong date format). Then the message is written in
   * red
   */
  private boolean errorQuery = false;
  private SessionBean session;
  private static Logger logger = Logger.getLogger(AdvancedSearchBean.class);

  /**
   * Constructor for the {@link AdvancedSearchBean}
   */
  public AdvancedSearchBean() {
    session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  }

  /**
   * Called when the page is called per get request. Read the query in the url and initialize the
   * form with it
   * 
   * @return
   */
  public String getNewSearch() {
    initMenus();
    try {
      String query =
          FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("q");
      if (!UrlHelper.getParameterBoolean("error")) {
        errorQuery = false;
        initForm(SearchQueryParser.parseStringQuery(query));
      }
    } catch (Exception e) {
      logger.error("Error initializing advanced search", e);
      BeanHelper.error("Error initializing advanced search");
    }
    return "";
  }

  /**
   * Init the menus of the page
   */
  private void initMenus() {
    operatorsMenu = new ArrayList<SelectItem>();
    operatorsMenu.add(new SelectItem(LOGICAL_RELATIONS.AND, session.getLabel("and_small")));
    operatorsMenu.add(new SelectItem(LOGICAL_RELATIONS.OR, session.getLabel("or_small")));
    fileTypesMenu = new ArrayList<>();
    for (Type type : ConfigurationBean.getFileTypesStatic().getTypes()) {
      fileTypesMenu.add(new SelectItem(type.getName(session.getLocale().getLanguage())));
    }
  }

  /**
   * Initialized the search form with the {@link SearchQuery}
   * 
   * @param searchQuery
   * @throws Exception
   */
  public void initForm(SearchQuery searchQuery) throws Exception {
    Map<String, MetadataProfile> profs = loadProfilesAndInitMenu();
    ((MetadataLabels) BeanHelper.getSessionBean(MetadataLabels.class))
        .init1((new ArrayList<MetadataProfile>(profs.values())));
    formular = new SearchForm(searchQuery, profs);
    if (formular.getGroups().size() == 0) {
      formular.addSearchGroup(0);
    }
    initFileTypesSelected();
  }

  /**
   * Init the selected file types according the query
   */
  private void initFileTypesSelected() {
    fileTypesSelected = new ArrayList<String>();
    for (String t : formular.getFileTypeSearch().getValue().split(Pattern.quote("|"))) {
      Type type = ConfigurationBean.getFileTypesStatic().getType(t);
      if (type != null) {
        fileTypesSelected.add(type.getName(session.getLocale().getLanguage()));
      }
      fileTypesSelected.add(t);
    }
  }

  /**
   * Reset the Search form with empty values
   * 
   * @throws Exception
   */
  public String reset() throws Exception {
    initForm(new SearchQuery());
    return "";
  }

  /**
   * Load all available profiles
   * 
   * @return
   * @throws ImejiException
   */
  private Map<String, MetadataProfile> loadProfilesAndInitMenu() throws ImejiException {
    profilesMenu = new ArrayList<SelectItem>();
    profilesMenu.add(new SelectItem(null, session.getLabel("select_profile")));
    ProfileController controller = new ProfileController();
    Map<String, MetadataProfile> map = new HashMap<String, MetadataProfile>();
    for (MetadataProfile p : controller.search(session.getUser(),
        session.getSelectedSpaceString())) {
      if (p != null && p.getStatements() != null && p.getStatements().size() > 0) {
        map.put(p.getId().toString(), p);
        profilesMenu.add(new SelectItem(p.getId().toString(), p.getTitle()));
      }
    }
    return map;
  }

  /**
   * Method called when form is submitted
   * 
   * @return
   * @throws IOException
   */
  public String search() throws IOException {
    FiltersSession filtersSession =
        (FiltersSession) BeanHelper.getSessionBean(FiltersSession.class);
    filtersSession.getFilters().clear();
    goToResultPage();
    return "";
  }

  /**
   * Redirect to the search result page
   * 
   * @throws IOException
   */
  public void goToResultPage() throws IOException {
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    try {
      errorQuery = false;
      String q = SearchQueryParser.transform2UTF8URL(formular.getFormularAsSearchQuery());
      if (!"".equals(q)) {
        FacesContext.getCurrentInstance().getExternalContext()
            .redirect(navigation.getBrowseUrl() + "?q=" + q);
      } else
        BeanHelper.error(session.getMessage("error_search_query_emtpy"));
    } catch (Exception e) {
      errorQuery = true;
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(navigation.getSearchUrl() + "?error=1&types=" + getFileTypesQuery());
    }
  }

  @SuppressWarnings("unchecked")
  public void fileTypeListener(ValueChangeEvent event) {
    fileTypesSelected = (List<String>) event.getNewValue();
    System.out.println(getFileTypesQuery());
    formular.getFileTypeSearch().setValue(getFileTypesQuery());
    Arrays.asList(formular.getFileTypeSearch().getValue().split("|"));
  }

  /**
   * REturn the file types Selected as a query
   * 
   * @return
   */
  private String getFileTypesQuery() {
    String qf = "";
    for (String type : fileTypesSelected) {
      if (!qf.equals(""))
        qf += "|";
      qf += type;
    }
    return qf;
  }


  /**
   * Change the {@link SearchGroup}
   * 
   * @throws ImejiException
   */
  public void changeGroup() throws ImejiException {
    int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    formular.changeSearchGroup(gPos);
  }

  /**
   * Add a new {@link SearchGroupForm}
   */
  public void addGroup() {
    int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    formular.addSearchGroup(gPos);
  }

  /**
   * Remove a {@link SearchGroupForm}
   */
  public void removeGroup() {
    int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    formular.removeSearchGroup(gPos);
    if (formular.getGroups().size() == 0) {
      formular.addSearchGroup(0);
    }
  }

  /**
   * Change a {@link SearchMetadataForm}. The search value is removed
   */
  public void changeElement() {
    int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    int elPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("elPos"));
    formular.changeElement(gPos, elPos, false);
  }

  /**
   * Update a {@link SearchMetadataForm}. The search value is keeped
   */
  public void updateElement() {
    int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    int elPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("elPos"));
    formular.changeElement(gPos, elPos, true);
  }

  /**
   * Add a new {@link SearchMetadataForm}
   */
  public void addElement() {
    int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    int elPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("elPos"));
    formular.addElement(gPos, elPos);
  }

  /**
   * Remove a new {@link SearchMetadataForm}
   */
  public void removeElement() {
    int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    int elPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("elPos"));
    formular.removeElement(gPos, elPos);
    if (formular.getGroups().get(gPos).getSearchElementForms().size() == 0) {
      formular.removeSearchGroup(gPos);
      formular.addSearchGroup(gPos);
    }
  }

  /**
   * Return the current {@link SearchQuery} in the form as a user friendly query
   * 
   * @return
   */
  public String getSimpleQuery() {
    try {
      errorQuery = false;
      return SearchQueryParser.searchQuery2PrettyQuery(formular.getFormularAsSearchQuery());
    } catch (Exception e) {
      errorQuery = true;
      if ("Wrong date format".equals(e.getMessage()))
        return session.getMessage("error_date_format");
      return e.getMessage();
    }
  }

  /**
   * Getter
   * 
   * @return
   */
  public List<SelectItem> getProfilesMenu() {
    return profilesMenu;
  }

  /**
   * Setter
   * 
   * @param collectionsMenu
   */
  public void setProfilesMenu(List<SelectItem> profilesMenu) {
    this.profilesMenu = profilesMenu;
  }

  /**
   * Getter
   * 
   * @return
   */
  public SearchForm getFormular() {
    return formular;
  }

  /**
   * stter
   * 
   * @param formular
   */
  public void setFormular(SearchForm formular) {
    this.formular = formular;
  }

  /**
   * Getter
   * 
   * @return
   */
  public List<SelectItem> getOperatorsMenu() {
    return operatorsMenu;
  }

  /**
   * Setter
   * 
   * @param operatorsMenu
   */
  public void setOperatorsMenu(List<SelectItem> operatorsMenu) {
    this.operatorsMenu = operatorsMenu;
  }

  /**
   * @return the errorQuery
   */
  public boolean getErrorQuery() {
    return errorQuery;
  }

  /**
   * @param errorQuery the errorQuery to set
   */
  public void setErrorQuery(boolean errorQuery) {
    this.errorQuery = errorQuery;
  }

  /**
   * @return the fileTypesMenu
   */
  public List<SelectItem> getFileTypesMenu() {
    return fileTypesMenu;
  }

  /**
   * @param fileTypesMenu the fileTypesMenu to set
   */
  public void setFileTypesMenu(List<SelectItem> fileTypesMenu) {
    this.fileTypesMenu = fileTypesMenu;
  }

  /**
   * @return the fileTypesSelected
   */
  public List<String> getFileTypesSelected() {
    return fileTypesSelected;
  }

  /**
   * @param fileTypesSelected the fileTypesSelected to set
   */
  public void setFileTypesSelected(List<String> fileTypesSelected) {
    this.fileTypesSelected = fileTypesSelected;
  }

  public List<SelectItem> getCollectionsMenu() {
    return collectionsMenu;
  }

  public void setCollectionsMenu(List<SelectItem> collectionsMenu) {
    this.collectionsMenu = collectionsMenu;
  }

}
