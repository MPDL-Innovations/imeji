package de.mpg.imeji.presentation.item.details;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;

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
  private static final long serialVersionUID = -6273094031705225499L;
  private final String collectionId;
  private static final Logger LOGGER = LogManager.getLogger(CollectionItemBean.class);

  public CollectionItemBean() {
    super();
    this.collectionId = UrlHelper.getParameterValue("collectionId");
  }

  @Override
  protected void initBrowsing() {
    if (getImage() != null) {
      setBrowse(new ItemDetailsBrowse(getImage(), "collection", ObjectHelper.getURI(CollectionImeji.class, collectionId).toString(),
          getSessionUser()));
    }
  }

  @Override
  public void redirectToBrowsePage() {
    try {
      redirect(getNavigation().getCollectionUrl() + collectionId);
    } catch (final IOException e) {
      LOGGER.error("Error redirect to browse page", e);
    }
  }

  public String getCollectionId() {
    return collectionId;
  }
}
