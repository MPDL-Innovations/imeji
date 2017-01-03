package de.mpg.imeji.logic.jobs;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.item.ItemService;

/**
 * Job to extract fulltext and technical metadata for all files
 *
 * @author saquet
 *
 */
public class FulltextAndTechnicalMetadataJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(FulltextAndTechnicalMetadataJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Extracting fulltext and technical metadata for all files...");
    final ItemService itemBusinessController = new ItemService();
    itemBusinessController.extractFulltextAndTechnicalMetadataForAllItems();
    LOGGER.info("... Extracting fulltext and technical metadata for all files done!");
    return 1;
  }

}
