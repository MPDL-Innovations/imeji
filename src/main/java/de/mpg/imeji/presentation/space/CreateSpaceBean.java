package de.mpg.imeji.presentation.space;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "CreateSpaceBean")
@ViewScoped
public class CreateSpaceBean extends SpaceBean {
  private static final long serialVersionUID = -5469506610392004531L;

  public CreateSpaceBean() {
    setSpaceCreateMode(true);
    init();
  }

  public String save() throws Exception {
    if (createdSpace()) {
      getSessionBean().setSpaceId(getSpace().getSlug());
      // Go to the home URL of the Space
      redirect(getNavigation().getHomeUrl());
    }

    return "";
  }

  public boolean createdSpace() throws ImejiException, IOException {
    try {
      final SpaceController spaceController = new SpaceController();
      final File spaceLogoFile = (getSessionBean().getSpaceLogoIngestImage() != null)
          ? getSessionBean().getSpaceLogoIngestImage().getFile() : null;
      setSpace(spaceController.create(getSpace(), getSelectedCollections(), spaceLogoFile,
          getSessionBean().getUser()));
      // reset the Session bean and this local, as anyway it will navigate
      // back to the home page
      // Note: check how it will work with eDit! Edit bean should be
      // implemented
      setIngestImage(null);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_space_create", getLocale()));
      return true;
    } catch (final UnprocessableError e) {
      BeanHelper.cleanMessages();
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_space_create", getLocale()));
      final List<String> listOfErrors = Arrays.asList(e.getMessage().split(";"));
      for (final String errorM : listOfErrors) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(errorM, getLocale()));
      }
      return false;
    }
  }
}
