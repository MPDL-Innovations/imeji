/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.faces.model.SelectItem;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;

/**
 * Helper to work with vocabularies
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class VocabularyHelper implements Serializable {
  private static final long serialVersionUID = -6066436759603556529L;
  private List<SelectItem> vocabularies;
  private static volatile Properties properties = null;

  /**
   * Load the properties and initialize the vocabularies
   */
  public VocabularyHelper(Locale locale) {
    try {
      loadProperties();
      initVocabularies(locale);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initialize the vocabularies
   */
  public void initVocabularies(Locale locale) {
    vocabularies = new ArrayList<SelectItem>();
    for (final Object o : properties.keySet()) {
      vocabularies.add(new SelectItem(properties.getProperty(o.toString()),
          Imeji.RESOURCE_BUNDLE.getLabel("vocabulary_" + o.toString(), locale)));
    }
  }

  /**
   * Load the properties form the file vocabularies.properties
   *
   * @throws IOException
   */
  public void loadProperties() throws IOException {
    if (properties == null) {
      synchronized (this) {
        if (properties == null) {
          InputStream instream = null;
          try {
            instream = PropertyReader.getInputStream("vocabulary.properties");
            final Properties p = new Properties();
            p.load(instream);
            properties = p;
          } catch (final Exception e) {
            throw new RuntimeException(e);
          } finally {
            if (instream != null) {
              instream.close();
            }
          }
        }
      }
    }
  }

  /**
   * Return the name of a vocabulary as defined in the properties
   *
   * @param uri
   * @return
   */
  public String getVocabularyName(URI uri) {
    if (uri == null) {
      return null;
    } else {
      for (final SelectItem voc : vocabularies) {
        if (voc.getValue().toString().equals(uri.toString())) {
          return voc.getLabel();
        }
      }
    }
    return "unknown";
  }

  public List<SelectItem> getVocabularies() {
    return vocabularies;
  }
}
