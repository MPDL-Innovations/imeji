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
package de.mpg.imeji.logic.writer;

import java.net.URI;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.ImejiTriple;
import de.mpg.imeji.logic.auth.Authorization;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.validation.Validator;
import de.mpg.imeji.logic.validation.ValidatorFactory;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Facade implementing Writer {@link Authorization}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class WriterFacade {
  private Writer writer;

  /**
   * Constructor for one model
   */
  public WriterFacade(String modelURI) {
    this.writer = WriterFactory.create(modelURI);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.writer.Writer#create(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void create(List<Object> objects, MetadataProfile profile, User user)
      throws ImejiException {
    if (objects.isEmpty())
      return;
    checkSecurity(objects, user, GrantType.CREATE);
    validate(objects, profile, Validator.Method.CREATE);
    writer.create(objects, user);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.writer.Writer#delete(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void delete(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty())
      return;
    checkSecurity(objects, user, GrantType.DELETE);
    validate(objects, null, Validator.Method.DELETE);
    writer.delete(objects, user);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.writer.Writer#update(java.util.List, de.mpg.imeji.logic.vo.User),
   * choose to check security
   */
  public void update(List<Object> objects, MetadataProfile profile, User user,
      boolean doCheckSecurity) throws ImejiException {
    if (objects.isEmpty())
      return;
    if (doCheckSecurity)
      checkSecurity(objects, user, GrantType.UPDATE);
    validate(objects, profile, Validator.Method.UPDATE);
    writer.update(objects, user);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.writer.Writer#updateLazy(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void updateLazy(List<Object> objects, MetadataProfile profile, User user)
      throws ImejiException {
    if (objects.isEmpty())
      return;
    checkSecurity(objects, user, GrantType.UPDATE);
    validate(objects, profile, Validator.Method.UPDATE);
    writer.updateLazy(objects, user);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.writer.Writer#updatePatch(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void patch(List<ImejiTriple> triples, User user, boolean doCheckSecurity)
      throws ImejiException {
    if (triples.isEmpty())
      return;
    List<Object> l = new ArrayList<Object>();
    for (ImejiTriple t : triples)
      l.add(t.getObject());
    if (doCheckSecurity)
      checkSecurity(l, user, GrantType.UPDATE);
    writer.patch(triples, user, doCheckSecurity);

  }

  private void validate(List<Object> list, MetadataProfile profile, Validator.Method method)
      throws UnprocessableError {
    if (list.isEmpty())
      return;
    Validator<Object> validator =
        (Validator<Object>) ValidatorFactory.newValidator(list.get(0), method);
    for (Object o : list) {
      validator.validate(o, profile);
    }
  }

  /**
   * Check {@link Security} for WRITE operations
   * 
   * @param list
   * @param user
   * @param opType
   * @throws NotAllowedError
   */
  private void checkSecurity(List<Object> list, User user, GrantType gt) throws NotAllowedError {
    String messageHelper = null;
    for (Object o : list) {
      messageHelper = " not allowed to " + Grant.getGrantTypeName(gt) + " " + extractID(o);
      if (gt == GrantType.CREATE) {
        throwAuthorizationException(AuthUtil.staticAuth().create(user, o), user.getEmail()
            + messageHelper);
      } else if (gt == GrantType.UPDATE) {
        throwAuthorizationException(AuthUtil.staticAuth().update(user, o), user.getEmail()
            + messageHelper);

      } else if (gt == GrantType.DELETE) {
        throwAuthorizationException(AuthUtil.staticAuth().delete(user, o), user.getEmail()
            + messageHelper);

      } else if (gt == GrantType.UPDATE_CONTENT) {
        throwAuthorizationException(AuthUtil.staticAuth().updateContent(user, o), user.getEmail()
            + messageHelper);

      } else if (gt == GrantType.ADMIN_CONTENT) {
        throwAuthorizationException(AuthUtil.staticAuth().adminContent(user, o), user.getEmail()
            + messageHelper);

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
   */
  public void checkSecurityParentObject(List<Object> list, User user, GrantType gt)
      throws NotAllowedError {
    if (list.isEmpty())
      return;
    checkSecurity(list, user, gt);
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
    } else if (o instanceof MetadataProfile) {
      return ((MetadataProfile) o).getId();
    } else if (o instanceof Space) {
      return ((Space) o).getId();
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
   */
  private void throwAuthorizationException(boolean allowed, String message) throws NotAllowedError {
    if (!allowed) {
      throw new NotAllowedError(message);
    }
  }

  /**
   * Transform a single {@link Object} into a {@link List} with one {@link Object}
   * 
   * @param o
   * @return
   */
  public static List<Object> toList(Object o) {
    List<Object> list = new ArrayList<Object>();
    list.add(o);
    return list;
  }

}
