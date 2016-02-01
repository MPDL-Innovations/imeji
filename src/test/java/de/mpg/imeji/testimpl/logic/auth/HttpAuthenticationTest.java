package de.mpg.imeji.testimpl.logic.auth;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.apache.commons.codec.binary.Base64;
import org.jose4j.lang.JoseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.auth.AuthenticationFactory;
import de.mpg.imeji.logic.auth.ImejiRsaKeys;
import de.mpg.imeji.logic.auth.authentication.APIKeyAuthentication;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.process.AdminProcess;
import util.JenaUtil;

/**
 * Unit Test for {@link HttpAuthenticationTest}
 * 
 * @author bastiens
 *
 */
public class HttpAuthenticationTest {
  @BeforeClass
  public static void setup()
      throws ImejiException, JoseException, NoSuchAlgorithmException, InvalidKeySpecException {
    JenaUtil.initJena();
    ImejiRsaKeys.init(null, null);
    UserController controller = new UserController(JenaUtil.testUser);
    User usertest = controller.retrieve(JenaUtil.TEST_USER_EMAIL);
    usertest.setApiKey(APIKeyAuthentication.generateKey(usertest.getId(), Integer.MAX_VALUE));
    controller.update(usertest, usertest);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    JenaUtil.closeJena();
  }

  /**
   * Test successful login with email and password
   */
  @Test
  public void loginWithEmailAndPassword() {
    try {
      User user = AuthenticationFactory
          .factory(
              generateBasicAuthenticationHeader(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD))
          .doLogin();
      Assert.assertNotNull(user);
    } catch (AuthenticationError e) {
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Test successful login with email and password
   */
  @Test
  public void loginWithWrongPassword() {
    try {
      AuthenticationFactory.factory(generateBasicAuthenticationHeader(JenaUtil.TEST_USER_EMAIL,
          JenaUtil.TEST_USER_PWD + "abc")).doLogin();
      Assert.fail();
    } catch (AuthenticationError e) {
      // Correct
    }
  }

  /**
   * Login a user with its API Key
   * 
   * @throws ImejiException
   * @throws JoseException
   */
  @Test
  public void loginWithAPIKey() throws ImejiException, JoseException {
    User usertest = new UserController(JenaUtil.testUser).retrieve(JenaUtil.TEST_USER_EMAIL);
    try {
      User user = AuthenticationFactory
          .factory(generateAPIKEYAuthenticationHeader(usertest.getApiKey())).doLogin();
      Assert.assertNotNull(user);
    } catch (AuthenticationError e) {
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Login A user with a wrong API Key
   * 
   * @throws ImejiException
   * @throws JoseException
   */
  @Test
  public void loginWithWrongAPIKey() throws ImejiException, JoseException {
    User usertest = new UserController(JenaUtil.testUser).retrieve(JenaUtil.TEST_USER_EMAIL);
    try {
      AuthenticationFactory
          .factory(generateAPIKEYAuthenticationHeader(usertest.getApiKey() + "abc")).doLogin();
      Assert.fail();
    } catch (AuthenticationError e) {
      // ok
    }
  }

  /**
   * Login a user which doesn't have an API Key (login is done with email/password)
   * 
   * @throws ImejiException
   */
  @Test
  public void loginUserWithoutKey() throws ImejiException {
    // Force key == null
    UserController controller = new UserController(JenaUtil.testUser);
    User usertest = controller.retrieve(JenaUtil.TEST_USER_EMAIL);
    usertest.setApiKey(null);
    controller.update(usertest, usertest);
    AdminProcess
        .login(generateBasicAuthenticationHeader(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD));
    String key = controller.retrieve(JenaUtil.TEST_USER_EMAIL).getApiKey();
    Assert.assertTrue(key != null && !"".equals(key));
  }

  /**
   * Login a user which doesn't have an API Key (login is done with email/password)
   * 
   * @throws ImejiException
   */
  @Test
  public void loginUserWithEmtpytKey() throws ImejiException {
    // Force key == ""
    UserController controller = new UserController(JenaUtil.testUser);
    User usertest = controller.retrieve(JenaUtil.TEST_USER_EMAIL);
    usertest.setApiKey("");
    controller.update(usertest, usertest);
    AdminProcess
        .login(generateBasicAuthenticationHeader(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD));
    String key = controller.retrieve(JenaUtil.TEST_USER_EMAIL).getApiKey();
    Assert.assertTrue(key != null && !"".equals(key));
  }

  /**
   * Logout a user
   * 
   * @throws ImejiException
   */
  @Test
  public void logout() throws ImejiException {
    // Login to be sure to have a valid key
    AdminProcess
        .login(generateBasicAuthenticationHeader(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD));
    // read the key
    UserController controller = new UserController(JenaUtil.testUser);
    User usertest = controller.retrieve(JenaUtil.TEST_USER_EMAIL);
    String key = usertest.getApiKey();
    AdminProcess.logout(generateAPIKEYAuthenticationHeader(key));
    Assert.assertFalse(key.equals(controller.retrieve(JenaUtil.TEST_USER_EMAIL).getApiKey()));
    try {
      // Try to login again with the old key
      AuthenticationFactory.factory(generateAPIKEYAuthenticationHeader(key)).doLogin();
      Assert.fail("Key shouldn't be valid after logout");
    } catch (AuthenticationError e) {
      // OK: Login not possible with old key
    }
  }

  /**
   * Return a correct Authorization header for basic authentication
   * 
   * @param email
   * @param password
   * @return
   */
  private String generateBasicAuthenticationHeader(String email, String password) {
    return "Basic "
        + new String(Base64.encodeBase64(new String(email + ":" + password).getBytes()));
  }

  /**
   * Return a correct Authorization header for API Key authentication
   * 
   * @param key
   * @return
   */
  private String generateAPIKEYAuthenticationHeader(String key) {
    return "Bearer " + key;
  }
}
