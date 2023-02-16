package net.coderbot.iris.colorspace;

import com.google.common.collect.ImmutableSet;
import net.coderbot.iris.gl.IrisRenderSystem;
import net.coderbot.iris.gl.program.ComputeProgram;
import net.coderbot.iris.gl.program.Program;
import net.coderbot.iris.gl.program.ProgramBuilder;
import net.coderbot.iris.gl.texture.InternalTextureFormat;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL43C;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

public class ColorSpaceConverter {
	private ColorSpace colorSpace;
	private int target;
	private int width;
	private int height;
	private ComputeProgram program;

	public ColorSpaceConverter(int mainRenderTarget, ColorSpace currentColorSpace, int width, int height) {
		this.target = mainRenderTarget;
		this.colorSpace = currentColorSpace;

		this.width = width;
		this.height = height;
		recreateShader(colorSpace);
	}

	public void changeMainRenderTarget(int mainRenderTarget, int width, int height) {
		this.target = mainRenderTarget;
		this.width = width;
		this.height = height;
	}

	public void changeCurrentColorSpace(ColorSpace space) {
		colorSpace = space;
		recreateShader(colorSpace);
	}

	public void recreateShader(ColorSpace colorSpace) {
		try {
			String source = new String(IOUtils.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/colorSpace.csh"))), StandardCharsets.UTF_8);

			source = source.replace("PLACEHOLDER", colorSpace.name().toUpperCase(Locale.US));

			ProgramBuilder builder = ProgramBuilder.beginCompute("colorSpace", source, ImmutableSet.of());
			builder.addTextureImage(() -> target, InternalTextureFormat.RGBA8, "framebuffer");

			this.program = builder.buildCompute();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void processColorSpace() {
		if (colorSpace == ColorSpace.SRGB) {
			// Packs output in SRGB by default.
			return;
		}

		program.use();
		IrisRenderSystem.dispatchCompute(width / 8, height / 8, 1);
		IrisRenderSystem.memoryBarrier(GL43C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL43C.GL_TEXTURE_FETCH_BARRIER_BIT);
		ComputeProgram.unbind();
	}
}
