package de.mpg.imeji.logic.search.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndex;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticQueryFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticSortFactory;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Search} implemtation for ElasticSearch
 * 
 * @author bastiens
 * 
 */
public class ElasticSearch implements Search {

  private ElasticTypes type = null;
  private ElasticIndexer indexer = null;

  /**
   * Construct an Elastic Search Query for on data type. If type is null, search for all types
   * 
   * @param type
   */
  public ElasticSearch(SearchObjectTypes type) {
    switch (type) {
      case ITEM:
        this.type = ElasticTypes.items;
        break;
      case COLLECTION:
        this.type = ElasticTypes.folders;
        break;
      case ALBUM:
        this.type = ElasticTypes.albums;
        break;
      case SPACE:
        this.type = ElasticTypes.spaces;
        break;
      default:
        this.type = null;
        break;
    }
    this.indexer = new ElasticIndexer(ElasticIndex.data, this.type);;
  }

  @Override
  public SearchIndexer getIndexer() {
    return indexer;
  }

  @Override
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user, String folderUri,
      String spaceId, int from, int size) {
    FilterBuilder f = ElasticQueryFactory.build(query, folderUri, spaceId, user);
    // System.out.println(f.buildAsBytes().toUtf8());
    if (size == -1) {
      size = Integer.MAX_VALUE;
    }
    SearchResponse resp =
        ElasticService.client.prepareSearch(ElasticIndex.data.name()).setNoFields()
            .setPostFilter(f).setTypes(getTypes()).setSize(size).setFrom(from)
            .addSort(ElasticSortFactory.build(sortCri)).execute().actionGet();
    return toSearchResult(resp);
  }

  @Override
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user,
      List<String> uris, String spaceId) {
    // Not needed for Elasticsearch. This method is used for sparql search
    return null;
  }

  @Override
  public SearchResult searchString(String query, SortCriterion sort, User user, int from, int size) {
    QueryBuilder q = QueryBuilders.queryStringQuery(query);
    SearchResponse resp =
        ElasticService.client.prepareSearch(ElasticIndex.data.name()).setNoFields()
            .setTypes(getTypes()).setQuery(q).setSize(size).setFrom(from)
            .addSort(ElasticSortFactory.build(sort)).execute().actionGet();
    return toSearchResult(resp);
  }

  /**
   * Get the datatype to search for
   * 
   * @return
   */
  private String[] getTypes() {
    if (type == null) {
      return (String[]) Arrays.asList(ElasticTypes.values()).toArray();
    }
    return (String[]) Arrays.asList(type.name()).toArray();
  }

  /**
   * Transform a {@link SearchResponse} to a {@link SearchResult}
   * 
   * @param resp
   * @return
   */
  private SearchResult toSearchResult(SearchResponse resp) {
    List<String> ids = new ArrayList<>();
    for (SearchHit hit : resp.getHits()) {
      ids.add(hit.getId());
    }
    return new SearchResult(ids, resp.getHits().getTotalHits());
  }


}
