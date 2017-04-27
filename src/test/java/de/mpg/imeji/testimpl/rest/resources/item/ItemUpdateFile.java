package de.mpg.imeji.testimpl.rest.resources.item;

import static de.mpg.imeji.logic.storage.util.StorageUtils.calculateChecksum;
import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_PATH;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_REST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.test.rest.resources.test.integration.ImejiTestBase;
import de.mpg.imeji.util.ImejiTestResources;

/**
 * Created by vlad on 09.12.14.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ItemUpdateFile extends ImejiTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemUpdateFile.class);
  private static final String PATH_PREFIX = "/rest/items";
  private static final String UPDATED_FILE_NAME = "updated_filename.png";
  private static String storedFileURL;
  private final String UPDATE_ITEM_FILE_JSON = STATIC_CONTEXT_REST + "/item.json";

  @BeforeClass
  public static void specificSetup() throws Exception {
    initCollection();
    initItem();
  }

  @Test
  public void test_1_UpdateItem_1_WithFile_Attached() throws IOException, BadRequestException {
    File testFile = ImejiTestResources.getTest2Jpg();
    String filename = testFile.getName();
    FileDataBodyPart filePart = new FileDataBodyPart("file", testFile);
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    itemTO.setFilename(null);
    multiPart.field("json", RestProcessUtils.buildJSONFromObject(itemTO));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Wrong file name", itemWithFileTO.getFilename(), equalTo(filename));
    storedFileURL = target().getUri() + itemWithFileTO.getFileUrl().getPath().substring(1);
    assertEquals(ImejiTestResources.getTest2Jpg().length(), itemWithFileTO.getFileSize());
    // LOGGER.info(RestProcessUtils.buildJSONFromObject(itemWithFileTO));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_2_WithFile_Fetched() throws ImejiException, IOException {

    initCollection();
    initItem();
    final String fileURL = target().getUri() + STATIC_CONTEXT_PATH.substring(1) + "/test2.jpg";

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        getStringFromPath(UPDATE_ITEM_FILE_JSON).replace("___FILE_NAME___", UPDATED_FILE_NAME)
            .replace("___FETCH_URL___", fileURL).replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "")
            .replace("___REFERENCE_URL___", ""));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file deos not match the source file",
        itemWithFileTO.getChecksumMd5(),
        equalTo(calculateChecksum(ImejiTestResources.getTest2Jpg())));
    // LOGGER.info(RestProcessUtils.buildJSONFromObject(itemWithFileTO));
  }


  @Ignore
  @Test
  public void test_1_UpdateItem_3_WithFile_Referenced() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        getStringFromPath(UPDATE_ITEM_FILE_JSON).replace("___FILE_NAME___", UPDATED_FILE_NAME)
            .replace("___FETCH_URL___", "").replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "")
            .replace("___REFERENCE_URL___", storedFileURL));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Reference URL does not match", storedFileURL,
        equalTo(itemWithFileTO.getFileUrl().toString()));
    assertThat("Should be link to NO_THUMBNAIL image:",
        itemWithFileTO.getWebResolutionUrlUrl().toString(), endsWith(ItemService.NO_THUMBNAIL_URL));
    assertThat("Should be link to NO_THUMBNAIL image:", itemWithFileTO.getThumbnailUrl().toString(),
        endsWith(ItemService.NO_THUMBNAIL_URL));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_4_WithFile_Attached_Fetched() throws IOException, ImejiException {

    File newFile = ImejiTestResources.getTestPng();

    final String fileURL = target().getUri() + STATIC_CONTEXT_PATH.substring(1) + "/test.jpg";

    FileDataBodyPart filePart = new FileDataBodyPart("file", newFile);

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json",
        getStringFromPath(UPDATE_ITEM_FILE_JSON).replace("___FILE_NAME___", newFile.getName())
            .replace("___FETCH_URL___", fileURL).replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "")
            .replace("___REFERENCE_URL___", ""));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file does not match the source file",
        itemWithFileTO.getChecksumMd5(), equalTo(calculateChecksum(newFile)));
    assertThat(itemWithFileTO.getThumbnailUrl().toString(),
        not(endsWith(ItemService.NO_THUMBNAIL_URL)));
    assertThat(itemWithFileTO.getWebResolutionUrlUrl().toString(),
        not(endsWith(ItemService.NO_THUMBNAIL_URL)));

  }

  @Ignore
  @Test
  public void test_1_UpdateItem_5_WithFile_Attached_Referenced()
      throws IOException, ImejiException {
    initCollection();
    initItem();
    File newFile = ImejiTestResources.getTest2Jpg();

    FileDataBodyPart filePart = new FileDataBodyPart("file", newFile);

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json",
        getStringFromPath(UPDATE_ITEM_FILE_JSON).replace("___FILE_NAME___", newFile.getName())
            .replace("___FETCH_URL___", "").replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "")
            .replace("___REFERENCE_URL___", storedFileURL));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file does not match the source file",
        itemWithFileTO.getChecksumMd5(), equalTo(calculateChecksum(newFile)));
    assertThat(itemWithFileTO.getThumbnailUrl().toString(),
        not(endsWith(ItemService.NO_THUMBNAIL_URL)));
    assertThat(itemWithFileTO.getWebResolutionUrlUrl().toString(),
        not(endsWith(ItemService.NO_THUMBNAIL_URL)));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_6_WithFile_Fetched_Referenced() throws IOException, ImejiException {

    final String fileURL = target().getUri() + STATIC_CONTEXT_PATH.substring(1) + "/test.jpg";

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        getStringFromPath(UPDATE_ITEM_FILE_JSON).replace("___FILE_NAME___", UPDATED_FILE_NAME)
            .replace("___FETCH_URL___", fileURL).replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "")
            .replace("___REFERENCE_URL___", storedFileURL));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file does not match the source file",
        itemWithFileTO.getChecksumMd5(),
        equalTo(calculateChecksum(ImejiTestResources.getTestJpg())));

    assertThat(itemWithFileTO.getFileUrl().toString(), not(isEmptyOrNullString()));
    assertThat(itemWithFileTO.getFileUrl().toString(), not(equalTo(storedFileURL)));
    assertThat(itemWithFileTO.getThumbnailUrl().toString(),
        not(endsWith(ItemService.NO_THUMBNAIL_URL)));
    assertThat(itemWithFileTO.getWebResolutionUrlUrl().toString(),
        not(endsWith(ItemService.NO_THUMBNAIL_URL)));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_7_WithFile_Attached_Fetched_Referenced()
      throws IOException, ImejiException {
    initCollection();
    initItem();

    File newFile = ImejiTestResources.getTest1Jpg();
    FileDataBodyPart filePart = new FileDataBodyPart("file", newFile);

    final String fileURL = target().getUri() + STATIC_CONTEXT_PATH.substring(1) + "/test1.jpg";

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json",
        getStringFromPath(UPDATE_ITEM_FILE_JSON).replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "")
            .replace("___FILE_NAME___", newFile.getName()).replace("___FETCH_URL___", fileURL)
            .replace("___REFERENCE_URL___", storedFileURL));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file does not match the source file",
        itemWithFileTO.getChecksumMd5(), equalTo(calculateChecksum(newFile)));
    assertThat(itemWithFileTO.getFileUrl().toString(), not(equalTo(fileURL)));
    assertThat(itemWithFileTO.getFileUrl().toString(), not(equalTo(storedFileURL)));
    assertThat(itemWithFileTO.getThumbnailUrl().toString(),
        not(containsString(ItemService.NO_THUMBNAIL_URL)));
    assertThat(itemWithFileTO.getWebResolutionUrlUrl().toString(),
        not(containsString(ItemService.NO_THUMBNAIL_URL)));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_8_InvalidFetchURL() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json", getStringFromPath(UPDATE_ITEM_FILE_JSON)
        .replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "").replace("___FETCH_URL___", "invalid url")

    );

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  @Ignore
  @Test
  public void test_1_UpdateItem_9_FetchURL_NoFile() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json",
        getStringFromPath(UPDATE_ITEM_FILE_JSON).replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "")
            .replace("___FETCH_URL___", "www.google.de")

    );

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  @Test
  public void test_2_UpdateItem_1_TypeDetection_JPG() throws IOException, BadRequestException {
    initCollection();
    initItem();
    itemTO.setFilename(null);
    File testFile = ImejiTestResources.getTest2Jpg();
    String filename = testFile.getName();
    FileDataBodyPart filePart = new FileDataBodyPart("file", testFile);
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    itemTO.setFilename(filename);
    multiPart.field("json", RestProcessUtils.buildJSONFromObject(itemTO));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Wrong file name", itemWithFileTO.getFilename(), equalTo(filename));
  }


  @Test
  public void test_3_UpdateItem_1_WithFile_AndCheckSumTest()
      throws IOException, BadRequestException {
    initItem("test2");
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTest2Jpg());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    itemTO.setFilename(ImejiTestResources.getTest2Jpg().getName());
    multiPart.field("json", RestProcessUtils.buildJSONFromObject(itemTO));

    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }
}
