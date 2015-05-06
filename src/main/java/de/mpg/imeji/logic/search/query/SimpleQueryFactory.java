/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.search.query;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.search.SPARQLSearch;
import de.mpg.imeji.logic.search.vo.SearchIndex;
import de.mpg.imeji.logic.search.vo.SearchIndex.IndexNames;
import de.mpg.imeji.logic.search.vo.SearchMetadata;
import de.mpg.imeji.logic.search.vo.SearchPair;
import de.mpg.imeji.logic.search.vo.SortCriterion;
import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.*;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.beans.FileTypes.Type;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.j2j.helper.J2JHelper;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Factory to created Sparql query from a {@link SearchPair}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SimpleQueryFactory {
	private static String PATTERN_SELECT = "";

	/**
	 * Create a SPARQL query
	 * 
	 * @param rdfType
	 * @param pair
	 * @param sortCriterion
	 * @param user
	 * @param isCollection
	 * @param specificQuery
	 * @return
	 */
	public static String getQuery(String modelName, String rdfType,
			SearchPair pair, SortCriterion sortCriterion, User user,
			boolean isCollection, String specificQuery, String spaceId) {
		PATTERN_SELECT = "PREFIX fn: <http://www.w3.org/2005/xpath-functions#> SELECT DISTINCT ?s ?sort0 XXX_MODEL_NAMES_XXX WHERE {XXX_SPACE_FILTER_XXX XXX_SECURITY_FILTER_XXX XXX_SEARCH_ELEMENT_XXX XXX_SPECIFIC_QUERY_XXX XXX_SEARCH_TYPE_ELEMENT_XXX  ?s <"
				+ ImejiNamespaces.STATUS + "> ?status XXX_SORT_ELEMENT_XXX}";
		return PATTERN_SELECT
				.replace(
						"XXX_MODEL_NAMES_XXX",
						getModelNames(modelName, pair, specificQuery,
								!"".equals(getSpaceRestriction(spaceId,
										modelName))))
				.replace("XXX_SPACE_FILTER_XXX",
						getSpaceRestriction(spaceId, modelName))
				.replace(
						"XXX_SECURITY_FILTER_XXX",
						SimpleSecurityQuery.queryFactory(user, rdfType,
								getFilterStatus(pair), isUserSearchPair(pair)))
				.replace("XXX_SEARCH_ELEMENT_XXX",
						getSearchElement(pair, rdfType, user))
				.replace("XXX_SEARCH_TYPE_ELEMENT_XXX", getRdfType(rdfType))
				.replace(
						"XXX_SORT_ELEMENT_XXX",
						getSortElement(sortCriterion,
								"http://imeji.org/terms/item".equals(rdfType)))
				.replace("XXX_SPECIFIC_QUERY_XXX", specificQuery);
	}

	/**
	 * Return the RDF Type of the search objects
	 * 
	 * @param rdfType
	 * @return
	 */
	private static String getRdfType(String rdfType) {
		if (rdfType == null || rdfType.equals(""))
			return "";
		return "?s a <" + rdfType + "> .";
	}

	/**
	 * Return the space query of the search objects. It checks the query for
	 * model and adds respectively the spaceUri Space restriction will only work
	 * for search and for explicitly provided model in this case
	 * 
	 * @param spaceURI
	 * @return
	 */
	private static String getSpaceRestriction(String spaceUri, String modelName) {
		if (spaceUri == null || spaceUri.equals(""))
			return "";

		boolean searchInSpace = false;
		if (modelName == null || modelName.equals("")) {
			return "";
		}

		boolean isCollection = modelName.equals(Imeji.collectionModel);
		boolean isImage = modelName.equals(Imeji.imageModel);
		// boolean isAlbum = modelName.equals(Imeji.albumModel);

		if (!isCollection && !isImage)
			return "";

		return isCollection ? "?s <http://imeji.org/terms/space> <" + spaceUri
				+ "> ."
				: "?s <http://imeji.org/terms/collection> ?coll . ?coll  <http://imeji.org/terms/space>  <"
						+ spaceUri + "> .";
	}

	/**
	 * Return the names of the dataset (model) of the query
	 * 
	 * @param modelName
	 * @return
	 */
	private static String getModelNames(String modelName, SearchPair pair,
			String specificQuery, boolean isInSpace) {
		if (specificQuery != null && !specificQuery.equals(""))
			return "";
		String names = "";
		if (modelName != null && !modelName.equals("")) {
			names = "FROM <" + modelName + "> FROM <" + Imeji.userModel
					+ "> FROM <" + Imeji.collectionModel + ">";
			if (Imeji.profileModel.equals(modelName) || isInSpace) {
				names += " FROM <" + Imeji.collectionModel + ">";
			}
			if (pair != null
					&& SearchIndex.IndexNames.item.name().equals(
							pair.getIndex().getName())
					&& !Imeji.imageModel.equals(modelName)) {
				names += " FROM <" + Imeji.imageModel + ">";
			}
		}
		return names;
	}

	/**
	 * Return all sparql elements needed for the query
	 * 
	 * @param pair
	 * @return
	 */
	private static String getSearchElement(SearchPair pair, String rdfType,
			User user) {
		String searchQuery = "";
		String variable = "el";

		if (pair == null) {
			return "";
		}

		IndexNames index = IndexNames.valueOf(pair.getIndex().getName());
		switch (index) {
		case all:// Simple Search
		case created:// search for date created
		case filename:// Search for filename
		case modified: // search for date modified
		case checksum: // Search for checksum
			searchQuery = "?s <" + pair.getIndex().getNamespace() + "> ?el";
			break;
		case citation:
			break;
		case citation_style:
			break;
		case alb: // search for an album
		case col: // search for a collection
			// If not logged in, add the the path to collection/album
			if (user == null
					&& J2JHelper.getResourceNamespace(new Item()).equals(
							rdfType))
				searchQuery = " ?s <" + pair.getIndex().getNamespace()
						+ "> ?c .";
			// Search for collection/album by id (uri)
			return "FILTER("
					+ getSimpleFilter(pair,
							SimpleSecurityQuery.getVariableName(rdfType),
							pair.isNot()) + ") ." + searchQuery;
		case cone:
			break;
		case cont_description:// Search for container description
			searchQuery = "?s <http://imeji.org/terms/container/metadata> ?cmd . OPTIONAL_FOR_NOT {?cmd <http://purl.org/dc/elements/1.1/description> ?el";
			break;
		case cont_md:
			break;
		case cont_person:
			break;
		case cont_person_family:// Search for container creator family name
			searchQuery = "?s <http://imeji.org/terms/container/metadata> ?cmd . OPTIONAL_FOR_NOT { ?cmd <http://xmlns.com/foaf/0.1/person> ?p . ?p <http://purl.org/escidoc/metadata/terms/0.1/family-name> ?el";
			break;
		case cont_person_given:// Search for container creator given name
			searchQuery = "?s <http://imeji.org/terms/container/metadata> ?cmd . OPTIONAL_FOR_NOT {?cmd <http://xmlns.com/foaf/0.1/person> ?p . ?p <http://purl.org/escidoc/metadata/terms/0.1/given-name> ?el";
			break;
		case cont_person_name:// Search for container creator complete name
			searchQuery = "?s <http://imeji.org/terms/container/metadata> ?cmd . OPTIONAL_FOR_NOT {?cmd <http://xmlns.com/foaf/0.1/person> ?p . ?p <http://purl.org/escidoc/metadata/terms/0.1/complete-name> ?el";
			break;
		case cont_person_org:
			break;
		case cont_person_org_name:// Search for container creator orga name
			searchQuery = "?s <http://imeji.org/terms/container/metadata> ?cmd . OPTIONAL_FOR_NOT { ?cmd <http://xmlns.com/foaf/0.1/person> ?p . ?p <http://purl.org/escidoc/metadata/profiles/0.1/organizationalunit> ?org . ?org <http://purl.org/dc/terms/title> ?el";
			break;
		case cont_title:// Search for container title
			searchQuery = "?s <http://imeji.org/terms/container/metadata> ?cmd . OPTIONAL_FOR_NOT {?cmd <http://purl.org/dc/elements/1.1/title> ?el";
			break;
		case creator:
			break;
		case date:
			break;
		case editor:
			break;
		case filetype: // Search for filetype ( image, video, audio...)
			// TODO Make access to config via static method and not beans
			ConfigurationBean config = (ConfigurationBean) BeanHelper
					.getApplicationBean(ConfigurationBean.class);
			String regex = "";
			String types = pair.getValue();
			for (String typeName : types.split(Pattern.quote("|"))) {
				Type type = config.getFileTypes().getType(typeName);
				if (type != null) {
					if (!regex.equals(""))
						regex += "|";
					regex += type.getAsRegexQuery();
				}
			}
			pair = new SearchPair(pair.getIndex(), pair.getOperator(), regex);
			searchQuery = "?s <http://imeji.org/terms/filename> ?el";
			break;
		case grant:
			break;
		case grant_for:
			break;
		case grant_type:
			break;
		case item: // search for an item
			if (J2JHelper.getResourceNamespace(new CollectionImeji()).equals(
					rdfType)
					|| J2JHelper.getResourceNamespace(new Album()).equals(
							rdfType)) {
				searchQuery = "?s <" + pair.getIndex().getNamespace() + "> ?el";
			} else {
				// Search for and item by id (uri)
				pair.setValue(normalizeURI(Item.class, pair.getValue()));
				return "FILTER(" + getSimpleFilter(pair, "s", pair.isNot())
						+ ") .";
			}
			break;
		case label:
			break;
		case latitude:
			break;
		case license:
			break;
		case longitude:
			break;
		case md:
			break;
		case mds:
			break;
		case number:
			break;
		case person:
			break;
		case person_family:
			break;
		case person_given:
			break;
		case person_id:
			break;
		case person_name:
			break;
		case person_org:
			break;
		case person_org_city:
			break;
		case person_org_country:
			break;
		case person_org_description:
			break;
		case person_org_id:
			break;
		case person_org_title:
			break;
		case person_role:
			break;
		case prof: // Search for a profile
			if (J2JHelper.getResourceNamespace(new MetadataProfile()).equals(
					rdfType)) {
				pair.setValue(normalizeURI(MetadataProfile.class,
						pair.getValue()));
				return "FILTER("
						+ getSimpleFilter(pair, "s", pair.isNot())
						+ ") . ?c <"
						+ SPARQLSearch.getIndex(SearchIndex.IndexNames.prof)
								.getNamespace() + "> ?s .";
			} else if (J2JHelper.getResourceNamespace(new CollectionImeji())
					.equals(rdfType)) {
				searchQuery = "?s <http://imeji.org/terms/mdprofile> ?el";
			} else if (J2JHelper.getResourceNamespace(new Item()).equals(
					rdfType)) {
				searchQuery = "?c <http://imeji.org/terms/mdprofile> ?el";
			}
			break;
		case prop:
			break;
		case statement: // search for a statement
			if (pair.isNot())
				return "?s <http://imeji.org/terms/metadataSet> ?mds . OPTIONAL { ?mds <http://imeji.org/terms/metadata/> ?md  . ?md  <http://imeji.org/terms/statement> ?el  . FILTER(?el=<"
						+ pair.getValue() + ">)} . FILTER (!bound(?el) ) .";
			else
				return "?s <http://imeji.org/terms/metadataSet> ?mds .  ?mds <http://imeji.org/terms/metadata/> ?md  . ?md  <http://imeji.org/terms/statement> ?el  . FILTER(?el=<"
						+ pair.getValue() + ">) .";
		case status: // Search for the status (release, pending)
			return "";// this is filtered in the security query
		case text:
			break;
		case time:
			break;
		case title:
			break;
		case type:// Search for metadata type (Text, Date, Person...)
			return "?s <http://imeji.org/terms/metadataSet> ?mds . ?mds <"
					+ ImejiNamespaces.METADATA + "> ?md  . ?md a <"
					+ pair.getValue() + "> .";
		case url:
			break;
		case user:
			// Search for all objects of rdfType for which the user
			// (pair.getValue) has a Grant
			// If user is same as searchpair value, then the security query will
			// be enough else Search for all objects
			// of rdfType for which the user (pair.getValue) has a Grant
			if (user != null && pair.getValue().equals(user.getId().toString()))
				return "";
			else
				return "<"
						+ pair.getValue()
						+ "> <http://imeji.org/terms/grant> ?g . ?g <http://imeji.org/terms/grantFor> ?"
						+ SimpleSecurityQuery.getVariableName(rdfType) + " .";
		case visibility:
			break;
		}

		/*
		 * Default case
		 */
		if ("".equals(searchQuery)) {
			// Search for a metadata value
			searchQuery = "?s <http://imeji.org/terms/metadataSet> ?mds . OPTIONAL_FOR_NOT { ?mds <"
					+ ImejiNamespaces.METADATA
					+ "> ?md  . ?md "
					+ getSearchElementsParent(pair.getIndex(), 0)
					+ " <"
					+ pair.getIndex().getNamespace() + "> ?el ";
		}

		if (pair instanceof SearchMetadata) {
			searchQuery = searchQuery
					+ " . ?md <http://imeji.org/terms/statement> ?el1 ";
		}
		// if (pair.isNot()) {
		// // If the pair is a negation
		// // return searchQuery.replace("OPTIONAL_FOR_NOT {", "OPTIONAL {")
		// // + " . FILTER(" + getSimpleFilter(pair, variable)
		// // + ")} . FILTER (!bound(?el) ) .";
		// return searchQuery.replace("OPTIONAL_FOR_NOT {", "")
		// + " . FILTER(!" + getSimpleFilter(pair, variable) + ") .";
		// } else {
		//
		// }
		// If the pair is not a negation
		return searchQuery.replace("OPTIONAL_FOR_NOT {", "") + " . FILTER("
				+ getSimpleFilter(pair, variable, pair.isNot()) + ") .";
	}

	/**
	 * Return all parent search element (according to {@link SearchIndex}) of a
	 * search element, as a sparql query
	 * 
	 * @param index
	 * @param parentNumber
	 * @return
	 */
	private static String getSearchElementsParent(SearchIndex index,
			int parentNumber) {
		String q = "";
		if (index.getParent() != null) {
			q += getSearchElementsParent(index.getParent(), parentNumber + 1)
					+ " <" + index.getParent().getNamespace() + "> ?p"
					+ parentNumber + " . ?p" + parentNumber;
		}
		return q;
	}

	/**
	 * If the uri has been corrupted (for instance /profile/ instead
	 * /metadataProfile/), return the correct uri
	 * 
	 * @param c
	 * @param uri
	 * @return
	 */
	private static String normalizeURI(Class<?> c, String uri) {
		if (isURL(uri)) {
			return ObjectHelper.getURI(c, ObjectHelper.getId(URI.create(uri)))
					.toString();
		}
		return ObjectHelper.getURI(c, uri).toString();
	}

	/**
	 * If the curretn {@link SearchPair} search for a {@link Status}, then
	 * return the search value
	 * 
	 * @param pair
	 * @return
	 */
	private static Status getFilterStatus(SearchPair pair) {
		if (pair != null
				&& SearchIndex.IndexNames.status.name().equals(
						pair.getIndex().getName())) {
			if (Status.PENDING.getUriString().equals(pair.getValue())
					|| Status.PENDING.name().equalsIgnoreCase(pair.getValue())) {
				return Status.PENDING;
			} else if (Status.RELEASED.getUriString().equals(pair.getValue())
					|| Status.RELEASED.name().equalsIgnoreCase(pair.getValue())) {
				return Status.RELEASED;
			} else if (Status.WITHDRAWN.getUriString().equals(pair.getValue())
					|| Status.WITHDRAWN.name()
							.equalsIgnoreCase(pair.getValue())) {
				return Status.WITHDRAWN;
			}
		}
		return null;
	}

	/**
	 * True if th {@link SearchPair} is searching for a {@link User}
	 * 
	 * @param pair
	 * @return
	 */
	private static boolean isUserSearchPair(SearchPair pair) {
		return pair != null
				&& SearchIndex.IndexNames.user.name().equals(
						pair.getIndex().getName());
	}

	/**
	 * Return the sparql elements needed for the search
	 * 
	 * @param sortCriterion
	 * @return
	 */
	private static String getSortElement(SortCriterion sortCriterion,
			boolean item) {
		if (sortCriterion != null && sortCriterion.getIndex() != null) {
			if (SearchIndex.IndexNames.created.name().equals(
					sortCriterion.getIndex().getName())) {
				return ". ?s <" + sortCriterion.getIndex().getNamespace()
						+ "> ?sort0";
			} else if (SearchIndex.IndexNames.modified.name().equals(
					sortCriterion.getIndex().getName())) {
				return ". ?s <" + sortCriterion.getIndex().getNamespace()
						+ "> ?sort0";
			} else if (SearchIndex.IndexNames.status.name().equals(
					sortCriterion.getIndex().getName())) {
				return ". ?s <" + sortCriterion.getIndex().getNamespace()
						+ "> ?sort0";
			} else if (SearchIndex.IndexNames.creator.name().equals(
					sortCriterion.getIndex().getName())) {
				return ". ?s <http://imeji.org/terms/container/metadata> ?contmd . ?contmd <http://xmlns.com/foaf/0.1/person> ?person . ?person <http://purl.org/escidoc/metadata/terms/0.1/complete-name> ?sort0";
			} else if (SearchIndex.IndexNames.cont_title.name().equals(
					sortCriterion.getIndex().getName())) {
				return (item ? " . ?c" : " . ?s")
						+ " <http://imeji.org/terms/container/metadata> ?title . ?title <"
						+ sortCriterion.getIndex().getNamespace() + "> ?sort0";
			} else if (SearchIndex.IndexNames.filename.name().equals(
					sortCriterion.getIndex().getName())) {
				return ". ?s <" + sortCriterion.getIndex().getNamespace()
						+ "> ?sort0";
			} else if (SearchIndex.IndexNames.prof.name().equals(
					sortCriterion.getIndex().getName())) {
				return (item ? " . ?c" : " . ?s")
						+ " <http://purl.org/dc/elements/1.1/title> ?sort0 ";
			}
		}
		return "";
	}

	/**
	 * Return a sparql filter for a {@link SearchPair}
	 * 
	 * @param pair
	 * @param variable
	 * @return
	 */
	private static String getSimpleFilter(SearchPair pair, String variable,
			boolean not) {
		if (pair.getIndex().equals(
				SPARQLSearch.getIndex(SearchIndex.IndexNames.all))) {
			return getTextSearchFilter(pair, variable);
		}
		String filter = "";
		if (pair.getValue() != null) {
			switch (pair.getOperator()) {
			case REGEX:
				filter += (pair.isNot() ? "!" : "")
						+ getTextSearchFilter(pair, variable);
				break;
			case EQUALS:
				filter += "?"
						+ variable
						+ (pair.isNot() ? "!" : "")
						+ "="
						+ getSearchValueInSPARQL(pair.getValue(),
								isRDFDate(pair));
				break;
			case GREATER:
				filter += "?"
						+ variable
						+ (pair.isNot() ? "<" : ">=")
						+ getSearchValueInSPARQL(pair.getValue(),
								isRDFDate(pair));
				break;
			case LESSER:
				filter += "?"
						+ variable
						+ (pair.isNot() ? ">" : "<=")
						+ getSearchValueInSPARQL(pair.getValue(),
								isRDFDate(pair));
				break;
			default:
				if (pair.getValue().startsWith("\"")
						&& pair.getValue().endsWith("\"")) {
					filter += variable + "='"
							+ pair.getValue().replaceAll("\"", "") + "'";
				}
				break;
			}
		}
		if ("".equals(filter.trim())) {
			return "true";
		}
		if (pair instanceof SearchMetadata) {
			filter += " && ?el1=<"
					+ ((SearchMetadata) pair).getStatement().toString() + ">";
		}
		return filter;
	}

	/**
	 * Return the search value in SPARQL
	 * 
	 * @param str
	 * @return
	 */
	private static String getSearchValueInSPARQL(String str, boolean dateAsTime) {
		if (isURL(str)) {
			return "<" + URI.create(str) + ">";
		} else if (isDate(str)) {
			if (dateAsTime)
				return "'" + DateFormatter.getTime(str)
						+ "'^^<http://www.w3.org/2001/XMLSchema#double>";
			else
				return "'" + DateFormatter.formatToSparqlDateTime(str)
						+ "'^^<http://www.w3.org/2001/XMLSchema#dateTime>";
		} else if (isNumber(str)) {
			return "'" + Double.valueOf(str)
					+ "'^^<http://www.w3.org/2001/XMLSchema#double>";
		}
		return "'" + escapeApostroph(str) + "'";
	}

	/**
	 * True if the {@link String} is an URL
	 * 
	 * @param str
	 * @return
	 */
	private static boolean isURL(String str) {
		return str
				.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
	}

	/**
	 * True if the String is a {@link Date}
	 * 
	 * @param str
	 * @return
	 */
	private static boolean isDate(String str) {
		return DateFormatter.parseDate(str, "yyyy-MM-dd") != null;
	}

	/**
	 * True if it is a Number
	 * 
	 * @param str
	 * @return
	 */
	private static boolean isNumber(String str) {
		return str.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
	}

	/**
	 * Return the {@link String} search value of the filter
	 * 
	 * @param pair
	 * @param variable
	 * @return
	 */
	private static String getTextSearchFilter(SearchPair pair, String variable) {
		String filter = "";
		String text = pair.getValue();
		StringReader simpleReader = new StringReader(text);
		int i = 0;
		boolean isPhraseQuery = false;
		List<String> words = new ArrayList<String>();
		String word = "";
		try {
			while ((i = simpleReader.read()) != -1) {
				if (i == '"') {
					if (isPhraseQuery) {
						isPhraseQuery = false;
						words.add(word);
						word = "";
					} else
						isPhraseQuery = true;
				} else if (i == ' ' && !isPhraseQuery) {
					words.add(word);
					word = "";
				} else {
					word += (char) i;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (!"".equals(word.trim())) {
			words.add(word);
		}
		for (String str : words) {
			if (!"".equals(filter)) {
				filter += " || ";
			}
			filter += "regex(str(?" + variable + "), '" + escapeApostroph(str)
					+ "', 'i')";
		}
		return filter.trim();
	}

	/**
	 * Escape the Apostrophe in String
	 * 
	 * @param s
	 * @return
	 */
	private static String escapeApostroph(String s) {
		return s.replace("'", "\\'");
	}

	/**
	 * True if the pair used a rdf date format
	 * 
	 * @param pair
	 * @return
	 */
	private static boolean isRDFDate(SearchPair pair) {
		return !(SearchIndex.IndexNames.created.name().equals(
				pair.getIndex().getName()) || SearchIndex.IndexNames.modified
				.name().equals(pair.getIndex().getName()));
	}
}
