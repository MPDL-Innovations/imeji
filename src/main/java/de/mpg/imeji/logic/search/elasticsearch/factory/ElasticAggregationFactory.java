package de.mpg.imeji.logic.search.elasticsearch.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiFileTypes;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;

/**
 * Factory class to buid an {@link AbstractAggregationBuilder}
 * 
 * @author saquet
 *
 */
public class ElasticAggregationFactory {

  private final static int BUCKETS_MAX_SIZE = 100;

  public static List<AbstractAggregationBuilder> build() {
    List<AbstractAggregationBuilder> aggregations = new ArrayList<>();
    List<Facet> facets = new FacetService().retrieveAllFromCache();
    FiltersAggregationBuilder systemAggregations =
        AggregationBuilders.filters("all", new FiltersAggregator.KeyedFilter("all", QueryBuilders.matchAllQuery()));
    NestedAggregationBuilder metadataAggregations = AggregationBuilders.nested("metadata", "metadata");
    for (Facet facet : facets) {
      String metadataField = getMetadataField(facet);
      if (metadataField != null) {
        metadataAggregations.subAggregation(getMetadataAggregation(facet, metadataField));
      } else if (SearchFields.filetype.name().equals(facet.getIndex())) {
        systemAggregations.subAggregation(getFiletypeAggregation(facet));
      } else if (SearchFields.collection.name().equals(facet.getIndex())) {
        systemAggregations.subAggregation(getCollectionAggregation(facet));
      } else if (SearchFields.license.name().equals(facet.getIndex())) {
        systemAggregations.subAggregation(getLicenseAggregation(facet));
      } else if (SearchFields.valueOfIndex(facet.getIndex()) == SearchFields.collection_author_organisation) {
        systemAggregations.subAggregation(getOrganizationsOfCollectionAggregation(facet));
      } else if (SearchFields.valueOfIndex(facet.getIndex()) == SearchFields.collection_author) {
        systemAggregations.subAggregation(getAuthorsOfCollectionAggregation(facet));
      } else {
        System.out.println("NOT CREATED AGGREGATION FOR FACET " + facet.getIndex());
      }
    }
    aggregations.add(metadataAggregations);
    aggregations.add(systemAggregations);
    aggregations.add(AggregationBuilders.filters(Facet.ITEMS,
        new FiltersAggregator.KeyedFilter(Facet.ITEMS, QueryBuilders.termQuery(ElasticFields.JOIN_FIELD.field(), "item"))));
    // new FiltersAggregator.KeyedFilter(Facet.ITEMS, QueryBuilders.typeQuery(ElasticService.ElasticIndices.items.name()))));

    aggregations.add(AggregationBuilders.filters(Facet.SUBCOLLECTIONS,
        new FiltersAggregator.KeyedFilter(Facet.SUBCOLLECTIONS, QueryBuilders.termQuery(ElasticFields.JOIN_FIELD.field(), "folder"))));
    // new FiltersAggregator.KeyedFilter(Facet.SUBCOLLECTIONS, QueryBuilders.typeQuery(ElasticService.ElasticIndices.folders.name()))));

    return aggregations;
  }

  /**
   * Create the aggregation for the license
   * 
   * @param facet
   * @return
   */
  private static AbstractAggregationBuilder getLicenseAggregation(Facet facet) {
    List<String> licenses = Stream.of(ImejiLicenses.values()).map(l -> l.name()).collect(Collectors.toList());
    licenses.add(ImejiLicenses.NO_LICENSE);
    IncludeExclude inex = new IncludeExclude(licenses.toArray(new String[0]), null);
    return AggregationBuilders.terms(SearchFields.license.name()).field(ElasticFields.LICENSE.field()).includeExclude(inex);
  }

  /**
   * Create the aggregation for filetype
   * 
   * @param facet
   * @return
   */
  private static AbstractAggregationBuilder getFiletypeAggregation(Facet facet) {
    FiltersAggregationBuilder filetypeAggregation = null;
    for (ImejiFileTypes.Type type : Imeji.CONFIG.getFileTypes().getTypes()) {
      BoolQueryBuilder filetypeQuery = QueryBuilders.boolQuery();
      for (String ext : type.getExtensionArray()) {
        filetypeQuery.should(QueryBuilders.queryStringQuery(ElasticFields.NAME.field() + ".suggest:" + "*." + ext));
      }
      filetypeAggregation = AggregationBuilders.filters(type.getName(null), filetypeQuery);
    }
    return filetypeAggregation;
  }

  /**
   * Create the aggregation for the collection
   * 
   * @param facet
   * @return
   */
  private static AbstractAggregationBuilder getCollectionAggregation(Facet facet) {
    return AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.TITLE_WITH_ID_OF_COLLECTION.field());
  }

  private static AbstractAggregationBuilder getAuthorsOfCollectionAggregation(Facet facet) {
    return AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.AUTHORS_OF_COLLECTION.field());
  }

  private static AbstractAggregationBuilder getOrganizationsOfCollectionAggregation(Facet facet) {
    return AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.ORGANIZATION_OF_COLLECTION.field());
  }

  /**
   * Return the aggregation for metadata
   * 
   * @param facet
   * @param metadataField
   * @return
   */
  private static AbstractAggregationBuilder getMetadataAggregation(Facet facet, String metadataField) {
    switch (StatementType.valueOf(facet.getType())) {
      case TEXT:
        return getMetadataTextAggregation(facet);
      case DATE:
        return getMetadataNumberAggregation(facet, metadataField);
      // return getMetadataDateAggregation(facet, metadataField);
      case NUMBER:
        return getMetadataNumberAggregation(facet, metadataField);
      case URL:
        return getMetadataTextAggregation(facet);
      case PERSON:
        return getMetadataTextAggregation(facet);
      default:
        return null;
    }
  }

  /**
   * Create aggregation for Date
   * 
   * @param facet
   * @param metadataField
   * @return
   */
  private static FilterAggregationBuilder getMetadataNumberAggregation(Facet facet, String metadataField) {
    FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex(),
        QueryBuilders.termQuery("metadata.index", getMetadataStatementIndex(facet.getIndex())));
    fb.subAggregation(AggregationBuilders.stats(facet.getIndex()).field(getMetadataField(facet)));
    return fb;
  }

  /**
   * Create aggregation for Date
   * 
   * @param facet
   * @param metadataField
   * @return
   */
  private static FilterAggregationBuilder getMetadataDateAggregation(Facet facet, String metadataField) {
    FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex(),
        QueryBuilders.termQuery("metadata.index", getMetadataStatementIndex(facet.getIndex())));
    fb.subAggregation(AggregationBuilders.dateHistogram(facet.getIndex()).field(getMetadataField(facet))
        .dateHistogramInterval(DateHistogramInterval.YEAR).format("yyyy"));

    return fb;
  }

  /**
   * Create Aggregation for metadata of type TEXT
   * 
   * @param facet
   * @param metadataField
   * @return
   */
  private static FilterAggregationBuilder getMetadataTextAggregation(Facet facet) {
    FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex(),
        QueryBuilders.termQuery("metadata.index", getMetadataStatementIndex(facet.getIndex())));
    fb.subAggregation(AggregationBuilders.terms(facet.getName()).field(getMetadataField(facet)).size(BUCKETS_MAX_SIZE));
    return fb;
  }

  /**
   * Extract from the search index (for ex: md.title.text) the statement index and return it
   * 
   * @param searchIndex
   * @return
   */
  public static String getMetadataStatementIndex(String searchIndex) {
    return searchIndex.startsWith("md.") ? searchIndex.split("\\.")[1] : searchIndex;
  }

  /**
   * Return the field in elasticsearch
   * 
   * @param searchIndex
   * @return
   */
  public static String getMetadataField(Facet f) {
    if (!f.getIndex().startsWith("md.")) {
      return null;
    }
    switch (StatementType.valueOf(f.getType())) {
      case TEXT:
        return "metadata.text.exact";
      case DATE:
        return "metadata.date";
      case NUMBER:
        return "metadata.number";
      case URL:
        return "metadata.text.exact";
      case PERSON:
        return "metadata.text.exact";
      default:
        return null;
    }
  }
}
