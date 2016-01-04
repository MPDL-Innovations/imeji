package de.mpg.imeji.logic.jobs;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.AlbumController;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.SpaceController;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Space;

/**
 * REindex data from the database into elastic search
 * 
 * @author bastiens
 * 
 */
public class ElasticReIndexJob implements Callable<Integer> {

  private static final Logger logger = Logger.getLogger(ElasticReIndexJob.class);

  @Override
  public Integer call() throws Exception {
    logger.info("Reindex started!");
    // Check if the alias is used by only 1 index. If not, reset completely the indexes
    ElasticService.getIndexNameFromAliasName(ElasticService.DATA_ALIAS);
    String index = ElasticService.createIndex();
    reindexAlbums(index);
    reindexItems(index);
    reindexFolders(index);
    reindexSpaces(index);
    ElasticService.setNewIndexAndRemoveOldIndex(index);
    // IMPORTANT: Albums must be reindex after Items
    logger.info("Reindex done!");
    return null;
  }

  /**
   * Reindex all the {@link CollectionImeji} stored in the database
   * 
   * @throws ImejiException
   */
  private void reindexFolders(String index) throws ImejiException {
    logger.info("Indexing Folders...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.folders, ElasticService.ANALYSER);
    indexer.addMapping();
    CollectionController c = new CollectionController();
    List<CollectionImeji> collections = (List<CollectionImeji>) c.retrieveAll(Imeji.adminUser);
    indexer.indexBatch(collections);
    indexer.commit();
    logger.info("Folders reindexed!");
  }

  /**
   * Reindex all the {@link Album} stored in the database
   * 
   * @throws ImejiException
   */
  private void reindexAlbums(String index) throws ImejiException {
    logger.info("Indexing Albums...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.albums, ElasticService.ANALYSER);
    indexer.addMapping();
    AlbumController controller = new AlbumController();
    List<Album> albums = controller.retrieveAll(Imeji.adminUser);
    indexer.indexBatch(albums);
    indexer.commit();
    logger.info("Albums reindexed!");
  }

  /**
   * Reindex all {@link Item} stored in the database
   * 
   * @throws ImejiException
   * 
   */
  private void reindexItems(String index) throws ImejiException {
    logger.info("Indexing Items...");
    ElasticIndexer indexer = new ElasticIndexer(index, ElasticTypes.items, ElasticService.ANALYSER);
    indexer.addMapping();
    ItemController controller = new ItemController();
    List<Item> items = (List<Item>) controller.retrieveAll(Imeji.adminUser);
    logger.info("+++ " + items.size() + " items to index +++");
    indexer.indexBatch(items);
    indexer.commit();
    logger.info("Items reindexed!");
  }

  /**
   * Reindex all {@link Item} stored in the database
   * 
   * @throws ImejiException
   * 
   */
  private void reindexSpaces(String index) throws ImejiException {
    logger.info("Indexing Spaces...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.spaces, ElasticService.ANALYSER);
    indexer.addMapping();
    SpaceController controller = new SpaceController();
    List<Space> items = (List<Space>) controller.retrieveAll();
    logger.info("+++ " + items.size() + " items to index +++");
    indexer.indexBatch(items);
    indexer.commit();
    logger.info("Items reindexed!");
  }

}
