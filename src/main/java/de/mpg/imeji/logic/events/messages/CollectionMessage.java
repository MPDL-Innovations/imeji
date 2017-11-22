package de.mpg.imeji.logic.events.messages;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * A Message for every events related to a collection
 * 
 * @author saquet
 *
 */
public class CollectionMessage extends Message {
  private static final long serialVersionUID = -4114678682804596495L;
  private final String parent;

  public CollectionMessage(MessageType type, CollectionImeji collection) {
    super(type, collection.getId());
    parent =
        collection.getCollection() != null ? ObjectHelper.getId(collection.getCollection()) : null;
  }

  public String getParent() {
    return parent;
  }
}
