package org.eclipse.gyrex.persistence.internal.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;

/**
 * A repository definition.
 */
public class RepositoryDefinition implements IRepositoryDefinition {

	private static final String KEY_TAGS = "tags";
	private static final String KEY_TYPE = "type";

	private final IEclipsePreferences storage;
	private final String repositoryId;
	private volatile RepositoryPreferences repositoryPreferences;

	RepositoryDefinition(final String repositoryId, final IEclipsePreferences storage) {
		this.repositoryId = repositoryId;
		this.storage = storage;
	}

	@Override
	public void addTag(final String tag) {
		if (StringUtils.isBlank(tag)) {
			return;
		}
		final HashSet<String> tags = new HashSet<String>(getTags());
		if (tags.contains(tag)) {
			return;
		}
		tags.add(tag);
		setTags(tags);
	}

	private void checkExist() {
		try {
			if (!storage.nodeExists("")) {
				throw new IllegalStateException(NLS.bind("Repository definition for repository ''{0}'' does not exist!", repositoryId));
			}
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(NLS.bind("Error accessing preferences store for repository ''{0}'': {1}", repositoryId, e.getMessage()), e);
		}
	}

	@Override
	public String getProviderId() {
		checkExist();
		return storage.get(KEY_TYPE, null);
	}

	@Override
	public String getRepositoryId() {
		return repositoryId;
	}

	@Override
	public IRepositoryPreferences getRepositoryPreferences() {
		if (null != repositoryPreferences) {
			return repositoryPreferences;
		}

		return repositoryPreferences = new RepositoryPreferences((IEclipsePreferences) storage.node("data"));
	}

	@Override
	public Collection<String> getTags() {
		checkExist();
		return Collections.unmodifiableCollection(Arrays.asList(StringUtils.split(storage.get(KEY_TAGS, StringUtils.EMPTY))));
	}

	@Override
	public void removeTag(final String tag) {
		if (StringUtils.isBlank(tag)) {
			return;
		}
		final HashSet<String> tags = new HashSet<String>(getTags());
		if (!tags.contains(tag)) {
			return;
		}
		tags.remove(tag);
		setTags(tags);
	}

	void setProviderId(final String providerId) {
		try {
			checkExist();
			if (storage.get(KEY_TYPE, null) != null) {
				throw new IllegalStateException(NLS.bind("Repository type for repository ''{0}'' already set!", repositoryId));
			}
			storage.put(KEY_TYPE, providerId);
			storage.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(NLS.bind("Error creating repository definition ''{0}'': {1}", repositoryId, e.getMessage()), e);
		}

	}

	private void setTags(final Collection<String> tags) {
		try {
			if (!tags.isEmpty()) {
				storage.put(KEY_TAGS, StringUtils.join(tags, ' '));
			} else {
				storage.remove(KEY_TAGS);
			}
			storage.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(NLS.bind("Error adding tags to repository definition ''{0}'': {1}", repositoryId, e.getMessage()), e);
		}
	}

}