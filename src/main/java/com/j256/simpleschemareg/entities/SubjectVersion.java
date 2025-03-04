package com.j256.simpleschemareg.entities;

import java.util.Objects;

/**
 * Bunch of fields returned when looking up a subject/version.
 */
public class SubjectVersion {

	private final String subject;
	private final long version;

	public SubjectVersion(String subject, long version) {
		this.subject = subject;
		this.version = version;
	}

	public String getSubject() {
		return subject;
	}

	public long getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + (int) (version ^ (version >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SubjectVersion other = (SubjectVersion) obj;
		if (!Objects.equals(subject, other.subject)) {
			return false;
		}
		return (version == other.version);
	}
}
