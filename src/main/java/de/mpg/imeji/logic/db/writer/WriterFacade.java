/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.db.writer;

import java.net.URI;
import java.security.Security;
import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.authorization.Authorization;
import de.mpg.imeji.logic.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.validation.ValidatorFactory;
import de.mpg.imeji.logic.validation.impl.Validator;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * Facade implementing Writer {@link Authorization}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class WriterFacade {
  private final Writer writer;
  private SearchIndexer indexer;
  private final WorkflowValidator workflowManager = new WorkflowValidator();

  /**
   * Constructor for one model
   */
  public WriterFacade(String modelURI) {
    this.writer = WriterFactory.create(modelURI);
    if (modelURI.equals(Imeji.imageModel)) {
      indexer =
          SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    } else if (modelURI.equals(Imeji.collectionModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC)
          .getIndexer();

    } else if (modelURI.equals(Imeji.albumModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.ALBUM, SEARCH_IMPLEMENTATIONS.ELASTIC)
          .getIndexer();

    } else if (modelURI.equals(Imeji.userModel)) {
      indexer =
          SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    } else if (modelURI.equals(Imeji.contentModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.CONTENT, SEARCH_IMPLEMENTATIONS.ELASTIC)
          .getIndexer();
    } else {
      indexer = SearchFactory.create(SEARCH_IMPLEMENTATIONS.JENA).getIndexer();
    }
  }

  /**
   * Constructor to decouple model and searchtype. Needed for usergroup which have same mode than
   * users
   *
   * @param modelURI
   * @param type
   */
  public WriterFacade(String modelURI, SearchObjectTypes type) {
    this.writer = WriterFactory.create(modelURI);
    indexer = SearchFactory.create(type, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#create(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void create(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    checkSecurity(objects, user, false);
    validate(objects, Validator.Method.CREATE);
    writer.create(objects, user);
    indexer.indexBatch(objects);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#delete(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void delete(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    checkWorkflowForDelete(objects);
    checkSecurity(objects, user, false);
    validate(objects, Validator.Method.DELETE);
    writer.delete(objects, user);
    indexer.deleteBatch(objects);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#update(java.util.List, de.mpg.imeji.logic.vo.User),
   * choose to check security
   */
  public void update(List<Object> objects, User user, boolean doCheckSecurity)
      throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    if (doCheckSecurity) {
      checkSecurity(objects, user, false);
    }
    validate(objects, Validator.Method.UPDATE);
    writer.update(objects, user);
    indexer.indexBatch(objects);
  }

  /**
   * Do a full update without any validation
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void updateWithoutValidation(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    throwAuthorizationException(user != null,
        SecurityUtil.authorization().administrate(user, Imeji.PROPERTIES.getBaseURI()),
        "Only admin ca use update wihout validation");
    writer.update(objects, user);
    indexer.indexBatch(objects);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#updateLazy(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void updateLazy(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    checkSecurity(objects, user, false);
    validate(objects, Validator.Method.UPDATE);
    writer.updateLazy(objects, user);
    indexer.indexBatch(objects);
  }

  @SuppressWarnings("unchecked")
  private void validate(List<Object> list, Validator.Method method) throws UnprocessableError {
    if (list.isEmpty()) {
      return;
    }
    final Validator<Object> validator =
        (Validator<Object>) ValidatorFactory.newValidator(list.get(0), method);
    for (final Object o : list) {
      validator.validate(o, method);
    }
  }

  private void checkWorkflowForDelete(List<Object> objects) throws WorkflowException {
    for (final Object o : objects) {
      if (o instanceof Properties) {
        workflowManager.isDeleteAllowed((Properties) o);
      }
    }
  }

  /**
   * Check {@link Security} for WRITE operations
   *
   * @param list
   * @param user
   * @param opType
   * @throws NotAllowedError
   * @throws AuthenticationError
   */
  private void checkSecurity(List<Object> list, User user, boolean create)
      throws NotAllowedError, AuthenticationError {
    String message = user != null ? user.getEmail() : "";
    for (final Object o : list) {
      message += " not allowed to " + (create ? "create " : "edit ") + extractID(o);
      if (create) {
        throwAuthorizationException(user != null, SecurityUtil.authorization().create(user, o),
            message);
      } else {
        throwAuthorizationException(user != null, SecurityUtil.authorization().update(user, o),
            message);
      }
    }
  }

  /**
   * Extract the id (as {@link URI}) of an imeji {@link Object},
   *
   * @param o
   * @return
   */
  public static URI extractID(Object o) {
    if (o instanceof Item) {
      return ((Item) o).getId();
    } else if (o instanceof Container) {
      return ((Container) o).getId();
    } else if (o instanceof User) {
      return ((User) o).getId();
    } else if (o instanceof UserGroup) {
      return ((UserGroup) o).getId();
    }
    return null;
  }

  /**
   * If false, throw a {@link NotAllowedError}
   *
   * @param b
   * @param message
   * @throws NotAllowedError
   * @throws AuthenticationError
   */
  private void throwAuthorizationException(boolean loggedIn, boolean allowed, String message)
      throws NotAllowedError, AuthenticationError {
    if (!allowed) {
      if (!loggedIn) {
        throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
      } else {
        throw new NotAllowedError(message);
      }

    }
  }

  /**
   * Transform a single {@link Object} into a {@link List} with one {@link Object}
   *
   * @param o
   * @return
   * @deprecated
   */
  public static List<Object> toList(Object o) {
    return Arrays.asList(o);
  }
}
