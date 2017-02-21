package de.mpg.imeji.presentation.filter;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;

@ManagedBean(name = "StatusFilterMenuBean")
@ViewScoped
public class StatusFilterMenuBean extends SuperFilterMenuBean {
  private static final long serialVersionUID = -820658514106886929L;
  private static final Logger LOGGER = Logger.getLogger(StatusFilterMenuBean.class);

  @PostConstruct
  public void init() {
    try {
      init(initMenu());
    } catch (final UnprocessableError e) {
      LOGGER.error("Error initializing StatusFilterMenuBean", e);
    }
  }

  private List<SelectItem> initMenu() throws UnprocessableError {
    final List<SelectItem> menu = new ArrayList<SelectItem>();
    menu.add(new SelectItem(
        SearchQueryParser.transform2URL(SearchQuery.toSearchQuery(
            new SearchPair(SearchFields.status, SearchOperators.EQUALS, "private", false))),
        Imeji.RESOURCE_BUNDLE.getLabel("only_private", getLocale())));
    menu.add(new SelectItem(
        SearchQueryParser.transform2URL(SearchQuery.toSearchQuery(
            new SearchPair(SearchFields.status, SearchOperators.EQUALS, "public", false))),
        Imeji.RESOURCE_BUNDLE.getLabel("only_public", getLocale())));
    menu.add(new SelectItem(
        SearchQueryParser.transform2URL(SearchQuery.toSearchQuery(
            new SearchPair(SearchFields.status, SearchOperators.EQUALS, "discarded", false))),
        Imeji.RESOURCE_BUNDLE.getLabel("only_withdrawn", getLocale())));
    return menu;
  }

}
