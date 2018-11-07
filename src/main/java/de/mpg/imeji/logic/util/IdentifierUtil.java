package de.mpg.imeji.logic.util;

import java.net.URI;
import java.util.Random;
import java.util.UUID;

/**
 * Provides A utility Method to create identifers in imeji
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class IdentifierUtil {
	private static Random rand = new Random();
	/**
	 * Array of all possible {@link String} characters which are used to generate a
	 * random Id
	 */
	private static final String[] RANDOM_ID_CHARSET = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
			"n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H",
			"I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2",
			"3", "4", "5", "6", "7", "8", "9", "_", ""};
	/**
	 * The size of the random id. Since the RANDOM_ID_CHARSET has 64 elements, to
	 * calculate the number of possible id, do 64^RANDOM_ID_SIZE with: - 1: 6 bits -
	 * 10: 60 bits - 12: 72 bits - 15: 90bits
	 */
	private static final int RANDOM_ID_SIZE = 16;

	/**
	 * Private Constructor
	 */
	private IdentifierUtil() {
		// avoid constructor
	}

	/**
	 * Return a randon id
	 *
	 * @return
	 */
	public static String newId() {
		return newRandomId();
	}

	/**
	 * Return an identifier according to the method passed (universal or random)
	 *
	 * @return
	 */
	public static String newId(String method) {
		return "universal".equals(method) ? newUniversalUniqueId() : newRandomId();
	}

	/**
	 * Return an {@link URI} according to the identifier creation method
	 *
	 * @param c
	 * @return
	 */
	public static URI newURI(Class<?> c) {
		return ObjectHelper.getURI(c, newId());
	}

	/**
	 * Return an {@link URI} according to the identifier creation method. Method can
	 * be universal, random
	 *
	 * @param c
	 * @return
	 */
	public static URI newURI(Class<?> c, String method) {
		return ObjectHelper.getURI(c, newId(method));
	}

	/**
	 * Create a random id. No assurance of uniqueness, even if probability is small.
	 * Generated id are smaller...
	 *
	 * @return
	 */
	public static String newRandomId() {
		String id = "";
		for (int i = 0; i < RANDOM_ID_SIZE; i++) {
			id += RANDOM_ID_CHARSET[rand.nextInt(RANDOM_ID_CHARSET.length)];
		}
		return id;
	}

	/**
	 * Create a {@link UUID}
	 *
	 * @return
	 */
	public static String newUniversalUniqueId() {
		return UUID.randomUUID().toString();
	}
}
