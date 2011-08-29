package de.mpg.imeji.beans;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import de.mpg.imeji.util.BeanHelper;
import de.mpg.jena.controller.SearchCriterion;
import de.mpg.jena.controller.SearchCriterion.Filtertype;
import de.mpg.jena.controller.SearchCriterion.ImejiNamespaces;
import de.mpg.jena.controller.SearchCriterion.Operator;
import de.mpg.jena.controller.SortCriterion.SortOrder;
import de.mpg.jena.util.ObjectHelper;
import de.mpg.jena.vo.User;

public abstract class SuperContainerBean<T> extends BasePaginatorListSessionBean<T>
{
    protected String selectedMenu;
    
    private String selectedSortCriterion;
    private String selectedSortOrder;
    
    private String selectedFilter;
    
    private SessionBean sb;

    
    private List<SelectItem> sortMenu = new ArrayList<SelectItem>();
    protected List<SelectItem> filterMenu = new ArrayList<SelectItem>();

    public SuperContainerBean()
    {
    	selectedMenu = "SORTING";
    	selectedFilter = "all";
        sb = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
        initMenus();
        selectedSortCriterion = ImejiNamespaces.PROPERTIES_LAST_MODIFICATION_DATE.name();
        selectedSortOrder = SortOrder.DESCENDING.name();
    }
    

    protected void initMenus()
    {
    	sortMenu = new ArrayList<SelectItem>();
        sortMenu.add(new SelectItem(ImejiNamespaces.PROPERTIES_STATUS, sb.getLabel(ImejiNamespaces.PROPERTIES_STATUS.name())));
        sortMenu.add(new SelectItem(ImejiNamespaces.CONTAINER_METADATA_TITLE, sb.getLabel(ImejiNamespaces.CONTAINER_METADATA_TITLE.name())));
        sortMenu.add(new SelectItem(ImejiNamespaces.PROPERTIES_LAST_MODIFICATION_DATE,sb.getLabel(ImejiNamespaces.PROPERTIES_LAST_MODIFICATION_DATE.name())));
       
        filterMenu = new ArrayList<SelectItem>();
        filterMenu.add(new SelectItem("all", sb.getLabel("all_except_withdrawn")));
        if (sb.getUser() != null)
        {
        	filterMenu.add(new SelectItem("my", sb.getLabel("my")));
        	filterMenu.add(new SelectItem("private", sb.getLabel("only_private")));
        }
        filterMenu.add(new SelectItem("public", sb.getLabel("only_public")));
        filterMenu.add(new SelectItem("withdrawn", sb.getLabel("only_withdrawn")));
    }
    
    public String getInitMenus()
    {
    	initMenus();
    	return "";
    }
    
    public void reset()
    {
        super.reset();
        initMenus();
    }

    public SearchCriterion getFilter()
    {
    	SearchCriterion sc = null;	
    	
    	if ("my".equals(selectedFilter))
    	{
    		sc = new SearchCriterion(Operator.AND, ImejiNamespaces.MY_IMAGES, ObjectHelper.getURI(User.class, sb.getUser().getEmail()).toString(), Filtertype.URI);
    	}
    	else if("private".equals(selectedFilter))
    	{
    		sc = new SearchCriterion(Operator.AND, ImejiNamespaces.PROPERTIES_STATUS, "http://imeji.mpdl.mpg.de/status/PENDING", Filtertype.URI);
    	}
    	else if("public".equals(selectedFilter))
    	{
    		sc = new SearchCriterion(Operator.AND, ImejiNamespaces.PROPERTIES_STATUS, "http://imeji.mpdl.mpg.de/status/RELEASED", Filtertype.URI);
    	}
    	else if("withdrawn".equals(selectedFilter))
    	{
    		sc = new SearchCriterion(Operator.AND, ImejiNamespaces.PROPERTIES_STATUS, "http://imeji.mpdl.mpg.de/status/WITHDRAWN", Filtertype.URI);
    	}
    	
    	return sc;
    }
    

    public void setSelectedMenu(String selectedMenu)
    {
        this.selectedMenu = selectedMenu;
    }

    public String getSelectedMenu()
    {
    	if (FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().containsKey("tab"))
		{
    		selectedMenu = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("tab");
		}
        return selectedMenu;
    }
    
    public String selectAll()
    {
        return getNavigationString();
    }
    
    public String selectNone()
    {
        return getNavigationString();
    }


    public void setSortMenu(List<SelectItem> sortMenu)
    {
        this.sortMenu = sortMenu;
    }


    public List<SelectItem> getSortMenu()
    {
        return sortMenu;
    }


    public void setSelectedSortOrder(String selectedSortOrder)
    {
        this.selectedSortOrder = selectedSortOrder;
    }


    public String getSelectedSortOrder()
    {
        return selectedSortOrder;
    }


    public void setSelectedSortCriterion(String selectedSortCriterion)
    {
        this.selectedSortCriterion = selectedSortCriterion;
    }


    public String getSelectedSortCriterion()
    {
        return selectedSortCriterion;
    }
    
    public String toggleSortOrder()
    {
        if(selectedSortOrder.equals("DESCENDING"))
        {
            selectedSortOrder="ASCENDING";
        }
        else
        {
            selectedSortOrder="DESCENDING";
        }
        return getNavigationString();
        
    }


	public String getSelectedFilter() {
		return selectedFilter;
	}


	public void setSelectedFilter(String selectedFilter) {
		this.selectedFilter = selectedFilter;
	}


	public List<SelectItem> getFilterMenu() 
	{
		return filterMenu;
	}


	public void setFilterMenu(List<SelectItem> filterMenu) {
		this.filterMenu = filterMenu;
	}
    
    
}
