package net.coderbot.iris.uniforms.values;

import java.util.List;

public class UniformValuesBuilder {
	List<String> names;


	private int internName(String name) {
		// TODO
		return 0;
	}

	private void addEdge(int src, int dst) {

	}

	public void invertMatrix(String dstName, String srcName) {
		int dst = internName(dstName);
		int mid = internName(dstName + " = invert(" + srcName + ")");
		int src = internName(srcName);

		addEdge(src, mid);
		addEdge(mid, dst);
	}

	public UniformValues build() {
		// run DFS to figure out what's actually active
	}

	enum NodeType {
		Upload,
		Value,
		Invert,
		Eval
	}
}
