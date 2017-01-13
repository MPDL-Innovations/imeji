package de.mpg.imeji.logic.search.util;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiFileTypes.Type;
import de.mpg.imeji.logic.search.Search;

/**
 * Utility class for the {@link Search}
 *
 * @author bastiens
 *
 */
public class SearchUtils {

  /**
   * Parse the query for File types and return a {@link List} of extension
   *
   * @param fileTypes
   * @return
   */
  public static List<String> parseFileTypesAsExtensionList(String fileTypes) {
    final List<String> extensions = new ArrayList<>();
    for (final String typeName : fileTypes.split(" OR ")) {
      final Type type = Imeji.CONFIG.getFileTypes().getType(typeName);
      for (final String ext : type.getExtensionArray()) {
        extensions.add(ext);
      }
    }
    return extensions;
  }
}
