/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.image.ItemBean;
import de.mpg.imeji.presentation.image.ItemsBean;
import de.mpg.imeji.presentation.image.SingleItemBrowse;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Bean for the detail item page when viewed within a collection
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "CollectionItemBean")
@ViewScoped
public class CollectionItemBean extends ItemBean {
  private String collectionId;
  private CollectionItemsBean collectionImagesBean;
  private static Logger LOGGER = Logger.getLogger(CollectionItemBean.class);

  public CollectionItemBean() {
    super();
    this.collectionId = UrlHelper.getParameterValue("collectionId");
    this.prettyLink = SessionBean.getPrettySpacePage("pretty:EditImageOfCollection", getSpace());
  }

  @Override
  public void initBrowsing() {
    if (getImage() != null) {
      ItemsBean itemsBean =
          (CollectionItemsBean) BeanHelper.getSessionBean(CollectionItemsBean.class);
      String tempId = (String) FacesContext.getCurrentInstance().getExternalContext()
          .getSessionMap().get("CollectionItemsBean.id");
      if (UrlHelper.getParameterBoolean("reload")) {
        itemsBean.browseInit(); // search the items
        itemsBean.update(); // Load the items
      }
      setBrowse(new SingleItemBrowse(itemsBean, getImage(), "collection", tempId));
    }
  }

  @Override
  public void redirectToBrowsePage() {
    try {
      redirect(getNavigation().getCollectionUrl() + collectionId + "/"
          + getNavigation().getBrowsePath());
    } catch (IOException e) {
      LOGGER.error("Error redirect to browse page", e);
    }
  }


  @Override
  public String getPageUrl() {
    return getNavigation().getCollectionUrl() + collectionId + "/" + Navigation.ITEM.getPath() + "/"
        + getId();
  }

  @Override
  public String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:CollectionItem", getSpace());
  }

  public CollectionItemsBean getCollectionImagesBean() {
    return collectionImagesBean;
  }

  public void setCollectionImagesBean(CollectionItemsBean collectionImagesBean) {
    this.collectionImagesBean = collectionImagesBean;
  }

  public String getCollectionId() {
    return collectionId;
  }
}
