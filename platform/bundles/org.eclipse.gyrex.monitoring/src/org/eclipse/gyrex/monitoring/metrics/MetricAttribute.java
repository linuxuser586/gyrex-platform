package org.eclipse.gyrex.monitoring.metrics;

/**
 * A metric attribute descriptor.
 * <p>
 * This class describes the attributes of a metric. It's used by the framework
 * for processing purposes.
 * </p>
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class MetricAttribute {

	private final String name;
	private final String description;
	private final Class type;

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 *            the attribute name
	 * @param description
	 *            the attribute description
	 * @param type
	 *            the attribute type
	 */
	MetricAttribute(final String name, final String description, final Class type) {
		this.name = name;
		this.description = description;
		this.type = type;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final MetricAttribute other = (MetricAttribute) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the attribute description.
	 * 
	 * @return the attribute description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the attribute name.
	 * 
	 * @return the attribute name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the attribute type.
	 * 
	 * @return the attribute type
	 */
	public Class getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Attribute [name=").append(name).append(", description=").append(description).append(", type=").append(type).append("]");
		return builder.toString();
	}
}