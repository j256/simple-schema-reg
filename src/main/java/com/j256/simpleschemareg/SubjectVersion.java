package com.j256.simpleschemareg;

import java.util.Objects;

/**
 * Class which encompasses subject and version.
 */
public class SubjectVersion {

	private final String subject;
	private final long version;

	public SubjectVersion(String subject, long version) {
		this.subject = subject;
		this.version = version;
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SubjectVersion other = (SubjectVersion) obj;
		return (Objects.equals(subject, other.subject) && version == other.version);
	}
}
