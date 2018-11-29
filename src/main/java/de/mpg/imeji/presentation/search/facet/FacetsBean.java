package de.mpg.imeji.presentation.search.facet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "FacetsBean")
@ViewScoped
public class FacetsBean extends SuperBean {
  private static final long serialVersionUID = -2474393161093378625L;
  private static final Logger LOGGER = LogManager.getLogger(FacetsBean.class);
  private List<Facet> facets = new ArrayList<>();
  private FacetService facetService = new FacetService();

  public FacetsBean() {

  }

  @PostConstruct
  public void init() {
    try {
      facets = facetService.retrieveAll();
      setPosition();
    } catch (ImejiException e) {
      BeanHelper.error("Error retrieving facets: " + e.getMessage());
      LOGGER.error("Error retrieving facets ", e);
    }
  }

  /**
   * Set the position of the item of the list
   */
  private void setPosition() {
    int i = 0;
    for (Facet f : facets) {
      f.setPosition(i);
      i++;
    }
  }

  public void delete() {
    final String index = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("index");
    try {
      Facet f = facetService.retrieveByIndexFromCache(index);
      facetService.delete(f, getSessionUser());
      BeanHelper.info("Facet " + f.getName() + " successfully deleted");
      reload();
    } catch (ImejiException | IOException e) {
      BeanHelper.error("Error deleting facet: " + e.getMessage());
      LOGGER.error("Error deleting facet: ", e);
    }
  }

  /**
   * Move the facet in the position to the top. i.e., get a lower position to appear one position
   * higher in the list
   * 
   * @param facet
   * @throws ImejiException
   */
  public void moveUp(Facet facet) throws ImejiException {
    Collections.swap(facets, facet.getPosition(), facet.getPosition() - 1);
    setPosition();
    facetService.update(facets, getSessionUser());
  }

  /**
   * Move the facet to the bottom of the list, i.e. get a higher position to appear one position
   * lower in the list
   * 
   * @param facet
   * @throws ImejiException
   */
  public void moveDown(Facet facet) throws ImejiException {
    Collections.swap(facets, facet.getPosition(), facet.getPosition() + 1);
    setPosition();
    facetService.update(facets, getSessionUser());
  }

  /**
   * @return the facets
   */
  public List<Facet> getFacets() {
    return facets;
  }

  /**
   * @param facets the facets to set
   */
  public void setFacets(List<Facet> facets) {
    this.facets = facets;
  }
}
