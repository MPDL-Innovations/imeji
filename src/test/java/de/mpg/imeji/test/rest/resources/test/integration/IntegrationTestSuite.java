package de.mpg.imeji.test.rest.resources.test.integration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.rest.resources.AlbumIntegration;
import de.mpg.imeji.testimpl.rest.resources.CollectionIntegration;
import de.mpg.imeji.testimpl.rest.resources.ProfileIntegration;
import de.mpg.imeji.testimpl.rest.resources.StorageIntegration;
import de.mpg.imeji.testimpl.rest.resources.VersionManagerTest;
import de.mpg.imeji.testimpl.rest.resources.item.ItemCreate;
import de.mpg.imeji.testimpl.rest.resources.item.ItemCreateRaw;
import de.mpg.imeji.testimpl.rest.resources.item.ItemDefaultMdCreate;
import de.mpg.imeji.testimpl.rest.resources.item.ItemDefaultMdCreateHierarchical;
import de.mpg.imeji.testimpl.rest.resources.item.ItemDefaultMdUpdateHierarchical;
import de.mpg.imeji.testimpl.rest.resources.item.ItemDeleteRaw;
import de.mpg.imeji.testimpl.rest.resources.item.ItemReadRaw;
import de.mpg.imeji.testimpl.rest.resources.item.ItemUpdate;
import de.mpg.imeji.testimpl.rest.resources.item.ItemDelete;
import de.mpg.imeji.testimpl.rest.resources.item.ItemEasyPatchMetadata;
import de.mpg.imeji.testimpl.rest.resources.item.ItemRead;
import de.mpg.imeji.testimpl.rest.resources.item.ItemUpdateBasic;
import de.mpg.imeji.testimpl.rest.resources.item.ItemUpdateBasicRaw;
import de.mpg.imeji.testimpl.rest.resources.item.ItemUpdateFile;
import de.mpg.imeji.testimpl.rest.resources.item.ItemUpdateFileRaw;
import de.mpg.imeji.testimpl.rest.resources.item.ItemUpdateMetadata;
import de.mpg.imeji.testimpl.rest.resources.item.ItemUpdateMetadataRaw;
import de.mpg.imeji.testimpl.rest.resources.item.ItemUpdateRaw;
import util.SuperTestSuite;


@RunWith(Suite.class)
@Suite.SuiteClasses({ 
   //Default format Tests
    ItemCreate.class, ItemRead.class, ItemUpdateBasic.class, ItemUpdateFile.class, 
    ItemUpdateMetadata.class , ItemDelete.class,  ItemDefaultMdCreate.class,ItemDefaultMdCreateHierarchical.class, ItemUpdate.class,
    ItemDefaultMdUpdateHierarchical.class,
    ItemEasyPatchMetadata.class, 
    //Raw Format Tests
    ItemCreateRaw.class, ItemReadRaw.class, ItemUpdateBasicRaw.class, ItemUpdateFileRaw.class, 
    ItemUpdateMetadataRaw.class , ItemDeleteRaw.class,  ItemUpdateRaw.class,

    //Other Tests
    AlbumIntegration.class, CollectionIntegration.class,
    ProfileIntegration.class, StorageIntegration.class, VersionManagerTest.class})

public class IntegrationTestSuite extends SuperTestSuite {

}
