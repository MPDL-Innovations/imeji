package de.mpg.imeji.presentation.share;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.collaboration.invitation.Invitation;
import de.mpg.imeji.logic.collaboration.invitation.InvitationBusinessController;
import de.mpg.imeji.logic.controller.resource.UserController;
import de.mpg.imeji.logic.controller.resource.UserGroupController;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.presentation.share.ShareBean.SharedObjectType;

/**
 * List of all entities with grant for one resource
 *
 * @author bastiens
 *
 */
public final class ShareList {
  private final List<ShareListItem> items = new ArrayList<ShareListItem>();
  private final List<ShareListItem> invitations = new ArrayList<ShareListItem>();


  /**
   * Create a list of all entities with grant for one resource
   *
   * @param ownerUri
   * @param sharedObjectUri
   * @param profileUri
   * @param type
   * @param currentUser
   * @throws ImejiException
   */
  public ShareList(URI ownerUri, String sharedObjectUri, String profileUri, SharedObjectType type,
      User currentUser, Locale locale) throws ImejiException {
    retrieveUsers(ownerUri, sharedObjectUri, profileUri, type, currentUser, locale);
    retrieveGroups(ownerUri, sharedObjectUri, profileUri, type, currentUser, locale);
    retrieveInvitations(sharedObjectUri, profileUri, type, currentUser, locale);
  }

  /**
   * Retrieve the user groups having grant for the this resource
   *
   * @param ownerUri
   * @param sharedObjectUri
   * @param profileUri
   * @param type
   * @param currentUser
   */
  private void retrieveGroups(URI ownerUri, String sharedObjectUri, String profileUri,
      SharedObjectType type, User currentUser, Locale locale) {
    UserGroupController ugc = new UserGroupController();
    Collection<UserGroup> groups = ugc.searchByGrantFor(sharedObjectUri, Imeji.adminUser);
    for (UserGroup group : groups) {
      items.add(
          new ShareListItem(group, type, sharedObjectUri, profileUri, null, currentUser, locale));
    }
  }

  /**
   * Retrieve all Users having grants for this resource
   *
   * @param ownerUri
   * @param sharedObjectUri
   * @param profileUri
   * @param type
   * @param currentUser
   */
  private void retrieveUsers(URI ownerUri, String sharedObjectUri, String profileUri,
      SharedObjectType type, User currentUser, Locale locale) {
    UserController uc = new UserController(Imeji.adminUser);
    Collection<User> allUser = uc.retrieveBatch(uc.searchByGrantFor(sharedObjectUri), -1);
    for (User u : allUser) {
      // Do not display the creator of this collection here
      if (!u.getId().toString().equals(ownerUri.toString())) {
        items.add(
            new ShareListItem(u, type, sharedObjectUri, profileUri, null, currentUser, locale));
      }
    }
  }

  /**
   * Retrieve all Pending invitations for this resource
   *
   * @param sharedObjectUri
   * @param profileUri
   * @param type
   * @param currentUser
   * @throws ImejiException
   */
  private void retrieveInvitations(String sharedObjectUri, String profileUri, SharedObjectType type,
      User currentUser, Locale locale) throws ImejiException {
    InvitationBusinessController invitationBC = new InvitationBusinessController();
    for (Invitation invitation : invitationBC.retrieveInvitationsOfObject(sharedObjectUri)) {
      invitations.add(
          new ShareListItem(invitation, type, sharedObjectUri, profileUri, currentUser, locale));
    }
  }

  public boolean isSizeEmtpy() {
    return items.isEmpty() && invitations.isEmpty();
  }

  public List<ShareListItem> getItems() {
    return items;
  }

  public List<ShareListItem> getInvitations() {
    return invitations;
  }

}
