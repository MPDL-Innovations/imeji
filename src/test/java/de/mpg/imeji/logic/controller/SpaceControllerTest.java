package de.mpg.imeji.logic.controller;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.ImejiSPARQL;
import de.mpg.imeji.logic.search.query.SPARQLQueries;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.imeji.rest.api.CollectionService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.resources.test.integration.ImejiTestBase;
import de.mpg.imeji.rest.to.CollectionTO;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.JenaUtil;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import static de.mpg.imeji.logic.Imeji.adminUser;
import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_REST;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by vlad on 15.04.15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SpaceControllerTest extends ControllerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpaceControllerTest.class);
  public static final int COL_NUM = 100;

  private static SpaceController sc;
  private static CollectionController cc;
  private static URI spaceId;
  private static Space space;
  private static final File uploadFile = new File("src/test/resources/storage/test.jpg");

  @BeforeClass
  public static void specificSetup() {
    sc = new SpaceController();
    cc = new CollectionController();
  }

  @Test
  public void test_1_Create() throws Exception {

    Space sp1 = ImejiFactory.newSpace();
    sp1.setTitle("Space 1");
    sp1.setDescription("Space 1 Description");
    sp1.setSlug("space_1");
    space = sc.create(sp1, adminUser);
    spaceId = space.getId();
    space = sc.retrieve(sp1.getId(), adminUser);
    assertThat(sp1.getTitle(), equalTo(space.getTitle()));
    assertThat(sp1.getDescription(), equalTo(space.getDescription()));
  }

  @Test
  public void test_2_Update() throws Exception {
    String changed = "_CHANGED";
    space.setTitle(space.getTitle() + changed);

    createCollection();
    CollectionImeji ci1 = collection;

    createCollection();
    CollectionImeji ci2 = collection;
    space = sc.update(space, adminUser);
    sc.addCollection(space, ci1.getId().toString(), adminUser);
    sc.addCollection(space, ci2.getId().toString(), adminUser);
    // Duplicates are not added
    sc.addCollection(space, ci2.getId().toString(), adminUser);

    assertThat(space.getTitle(), endsWith(changed));
    assertThat(sc.retrieveCollections(space), hasSize(2));
  }

  @Test
  public void test_3_UpdateFile() throws Exception {

    space = sc.updateFile(space, uploadFile, adminUser);
    File updateFile = new File("src/test/resources/storage/test2.jpg");
    space = sc.updateFile(space, updateFile, adminUser);
    assertTrue(FileUtils.contentEquals(updateFile,
        new File(sc.transformUrlToPath(space.getLogoUrl().toURL().toString()))));
    assertThat(sc.retrieveCollections(space), hasSize(2));
  }

  @Test
  public void test_4_Retrieve() throws Exception {
    assertThat(sc.retrieve(space.getId(), adminUser).getId(), equalTo(spaceId));
    assertThat(sc.retrieve(space.getId(), null).getId(), equalTo(spaceId));
  }

  @Test
  public void test_5_RetrieveAll() throws Exception {
    Space sp2 = ImejiFactory.newSpace();
    sp2.setTitle("Space 2");
    sp2.setSlug("space_2");
    sc.create(sp2, adminUser);
    assertThat(sc.retrieveAll(), hasSize(2));
  }

  @Test
  public void test_6_RemoveCollection() throws Exception {

    assertThat(space.getSpaceCollections(), hasSize(2));

    assertThat(
        sc.removeCollection(space, Iterables.getLast(sc.retrieveCollections(space)), adminUser),
        hasSize(1));
    space.setSpaceCollections(sc.retrieveCollections(space));
    assertThat(
        cc.retrieve(URI.create(Iterables.getFirst(space.getSpaceCollections(), null)), adminUser)
            .getSpace(), equalTo(spaceId));
  }


  @Test
  public void test_7_CreateFull() throws Exception {

    Space sp1 = ImejiFactory.newSpace();
    sp1.setTitle("Space Full Create");
    sp1.setSlug("space_full_create");

    createCollection();
    CollectionImeji ci1 = collection;

    createCollection();
    CollectionImeji ci2 = collection;

    final Collection<String> colls =
        Lists.newArrayList(ci1.getId().toString(), ci2.getId().toString());
    sp1.setSpaceCollections(colls);

    space = sc.retrieve(sc.create(sp1, colls, uploadFile, adminUser), adminUser);
    assertThat(space.getTitle(), equalTo(sp1.getTitle()));

    Collection<String> spaceCollections = space.getSpaceCollections();
    Iterables.removeAll(spaceCollections, colls);
    assertThat(spaceCollections, empty());

    assertTrue(FileUtils.contentEquals(uploadFile,
        new File(sc.transformUrlToPath(space.getLogoUrl().toURL().toString()))));
  }

  @Test
  public void test_8_Delete() throws Exception {
    for (Space s : sc.retrieveAll()) {
      sc.delete(s, adminUser);
    }
    assertThat(sc.retrieveAll(), empty());
  }



  @Test
  public void test_9_RetrieveSpaceCollections() throws Exception {
    Space sp2 = ImejiFactory.newSpace();
    sp2.setTitle("Space Collections Test");
    sp2.setSlug("space_collections_test");
    sc.create(sp2, adminUser);
    createCollection();
    CollectionImeji ci3 = collection;
    int collectionsOutOfSpace = cc.retrieveCollectionsNotInSpace(adminUser).size();
    sc.addCollection(sp2, ci3.getId().toString(), adminUser);
    assertThat(sc.retrieveCollections(sp2), hasSize(1));
    assertThat(cc.retrieveCollectionsNotInSpace(adminUser), hasSize(collectionsOutOfSpace - 1));
    sc.removeCollection(sp2, ci3.getId().toString(), adminUser);
    assertThat(cc.retrieveCollectionsNotInSpace(adminUser), hasSize(collectionsOutOfSpace));
  }

  /*
   * @Ignore
   * 
   * @Test public void test_999_Performance() throws Exception {
   * 
   * 
   * //create space Space sp1 = ImejiFactory.newSpace(); sp1.setTitle("Space 1");
   * sp1.setSlug("space_1"); space = sc.create(sp1, adminUser); spaceId = space.getId();
   * assertThat(sp1.getTitle(), equalTo(space.getTitle()));
   * 
   * List<String> results = ImejiSPARQL.exec(SPARQLQueries.countTriplesAll(), null);
   * 
   * //create COL_NUM collections String[] colIds = new String[COL_NUM]; collectionTO=
   * (CollectionTO) RestProcessUtils.buildTOFromJSON( getStringFromPath(STATIC_CONTEXT_REST +
   * "/createCollection.json"), CollectionTO.class); CollectionService cs = new CollectionService();
   * long startTime = System.currentTimeMillis(); for (int i = 0; i < COL_NUM; i++) {
   * collectionTO.setTitle("Collection " + i); colIds[i] = cs.create(collectionTO,
   * JenaUtil.testUser).getId(); } LOGGER.info("creation time: " + (System.currentTimeMillis() -
   * startTime ) + "ms");
   * 
   * startTime = System.currentTimeMillis(); //add collections to space for (int i = 0; i < COL_NUM;
   * i++) { sc.addCollection(space, colIds[i], adminUser); } LOGGER.info("addition time:" +
   * (System.currentTimeMillis() - startTime ) + "ms");
   * 
   * }
   */
}
