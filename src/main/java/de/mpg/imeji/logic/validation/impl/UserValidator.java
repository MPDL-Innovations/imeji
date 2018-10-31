package de.mpg.imeji.logic.validation.impl;

import java.util.HashSet;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchResult;

/**
 * {@link Validator} for {@link CollectionImeji}
 *
 * @author saquet
 *
 */
public class UserValidator extends ObjectValidator implements Validator<User> {
	private UnprocessableError exception = new UnprocessableError(new HashSet<String>());

	@Override
	public void validate(User user, Method m) throws UnprocessableError {
		exception = new UnprocessableError();
		setValidateForMethod(m);
		if (isDelete()) {
			return;
		}

		if (user.getEmail() == null || "".equals(user.getEmail().trim())) {
			exception = new UnprocessableError("error_user_email_unfilled", exception);
		} else if (!isValidEmail(user.getEmail())) {
			exception = new UnprocessableError("error_user_email_not_valid", exception);
		}

		if (emailAlreadyUsed(user)) {
			exception = new UnprocessableError("error_user_already_exists", exception);
		}

		if (user.getPerson() == null || "".equals(user.getPerson().getFamilyName())
				|| user.getPerson().getFamilyName() == null) {
			exception = new UnprocessableError("error_user_name_unfilled", exception);
		}

		if (user.getPerson() != null && "".equals(user.getPerson().getOrganizationString())) {
			exception = new UnprocessableError("error_user_organization_unfilled", exception);
		}

		if (exception.hasMessages()) {
			throw exception;
		}
	}

	/**
	 * True if the {@link User} exists
	 *
	 * @return
	 * @throws Exception
	 */
	private boolean emailAlreadyUsed(User user) {
		return existsUserWitheMail(user.getEmail(), user.getId().toString(),
				(Method.CREATE.equals(getValidateForMethod()) ? true : false));
	}

	/**
	 * Is true if the Email is valid
	 *
	 * @return
	 */
	public static boolean isValidEmail(String email) {
		// final String regexEmailMatch = "([^.@]+)(\\.[^.@]+)*@([^.@]+\\.)+([^.@]+)";
		final String regexEmailMatch = "([^@\\s]+)*@([^.@]+\\.)+([^.@]+)";
		return email.matches(regexEmailMatch);
	}

	/**
	 * Retrieve a {@link User} according to its email
	 *
	 * @param email
	 * @return
	 * @throws ImejiException
	 */
	private boolean existsUserWitheMail(String email, String userUri, boolean newUser) {
		final Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.JENA);
		final SearchResult result = search.searchString(JenaCustomQueries.selectUserByEmail(email), null, null,
				Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS);
		if (result.getNumberOfRecords() == 0) {
			return false;
		} else {
			// New users always have assigned Id, thus we do not check if it is existing
			// user here
			if (newUser && result.getNumberOfRecords() > 0) {
				return true;
			}

			// Check if it is existing user here who has same email
			boolean thereIsOtherUser = false;
			for (final String userId : result.getResults()) {
				if (!userUri.equals(userId)) {
					thereIsOtherUser = true;
				}
			}
			return thereIsOtherUser;
		}
	}

}
