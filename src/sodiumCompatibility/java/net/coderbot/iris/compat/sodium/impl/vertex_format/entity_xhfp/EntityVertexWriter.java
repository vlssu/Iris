package net.coderbot.iris.compat.sodium.impl.vertex_format.entity_xhfp;

import net.coderbot.iris.vertices.EntityVelocity;

public interface EntityVertexWriter {
	void setVelocity(EntityVelocity velocity);

	void resetVelocity();
}
