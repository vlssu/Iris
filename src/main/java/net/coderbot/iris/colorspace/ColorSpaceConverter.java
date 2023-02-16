package net.coderbot.iris.colorspace;

import com.google.common.collect.ImmutableSet;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.IrisRenderSystem;
import net.coderbot.iris.gl.program.ComputeProgram;
import net.coderbot.iris.gl.program.ProgramBuilder;
import net.coderbot.iris.gl.texture.InternalTextureFormat;
import net.coderbot.iris.shaderpack.preprocessor.JcppProcessor;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL43C;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

public class ColorSpaceConverter {
	private ColorSpace colorSpace;
	private boolean shouldSkipColorSpaceConversion;
	private ColorBlindness colorBlindness;
	private float colorBlindnessIntensity;
	private int target;
	private int width;
	private int height;
	private ComputeProgram colorSpaceProgram;
	private ComputeProgram colorBlindnessProgram;

	public ColorSpaceConverter(int mainRenderTarget, ColorSpace currentColorSpace, ColorBlindness currentColorBlindness, float colorBlindnessIntensity, int width, int height) {
		this.target = mainRenderTarget;
		this.colorSpace = currentColorSpace;
		this.colorBlindness = currentColorBlindness;
		this.colorBlindnessIntensity = colorBlindnessIntensity;

		this.width = width;
		this.height = height;
		recreateColorSpaceShader(colorSpace);
		recreateColorBlindnessShader(colorBlindness, colorBlindnessIntensity);
	}

	public void changeMainRenderTarget(int mainRenderTarget, int width, int height) {
		this.target = mainRenderTarget;
		this.width = width;
		this.height = height;
	}

	public void changeCurrentColorSpace(ColorSpace space) {
		colorSpace = space;
		recreateColorSpaceShader(colorSpace);
	}

	public void changeCurrentColorBlindness(ColorBlindness space, float colorBlindnessIntensity) {
		colorBlindness = space;
		this.colorBlindnessIntensity = colorBlindnessIntensity;
		recreateColorBlindnessShader(colorBlindness,colorBlindnessIntensity);
	}

	public void recreateColorSpaceShader(ColorSpace colorSpace) {
		try {
			if (colorSpaceProgram != null) {
				colorSpaceProgram.destroy();
			}
			String source = new String(IOUtils.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/Iris_ColourManagement.csh"))), StandardCharsets.UTF_8);

			source = source.replace("PLACEHOLDER", colorSpace.name().toUpperCase(Locale.US));
			source = JcppProcessor.glslPreprocessSource(source, Collections.EMPTY_LIST);

			ProgramBuilder builder = ProgramBuilder.beginCompute("colorSpace", source, ImmutableSet.of());
			builder.addTextureImage(() -> target, InternalTextureFormat.RGBA8, "mainImage");

			this.colorSpaceProgram = builder.buildCompute();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void recreateColorBlindnessShader(ColorBlindness colorBlindness, float colorBlindnessIntensity) {
		try {
			if (colorBlindnessProgram != null) {
				colorBlindnessProgram.destroy();
			}
			String source = new String(IOUtils.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/iris_ColorBlindness.csh"))), StandardCharsets.UTF_8);

			source = source.replace("PLACEHOLDER", colorBlindness.name().toUpperCase(Locale.US));
			source = source.replace("INTENSITY", String.valueOf(colorBlindnessIntensity));
			source = JcppProcessor.glslPreprocessSource(source, Collections.EMPTY_LIST);

			ProgramBuilder builder = ProgramBuilder.beginCompute("colorBlindness", source, ImmutableSet.of());
			builder.addTextureImage(() -> target, InternalTextureFormat.RGBA8, "mainImage");

			this.colorBlindnessProgram = builder.buildCompute();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void processColorSpace() {
		if (colorSpace == ColorSpace.SRGB || shouldSkipColorSpaceConversion) {
			// Packs output in SRGB by default.
			return;
		}

		colorSpaceProgram.use();
		IrisRenderSystem.dispatchCompute(width / 8, height / 8, 1);
		IrisRenderSystem.memoryBarrier(GL43C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL43C.GL_TEXTURE_FETCH_BARRIER_BIT);
		ComputeProgram.unbind();
	}

	public void processColorBlindness() {
		if (colorBlindness == ColorBlindness.NONE) {
			// Packs output in SRGB by default.
			return;
		}

		colorBlindnessProgram.use();
		IrisRenderSystem.dispatchCompute(width / 8, height / 8, 1);
		IrisRenderSystem.memoryBarrier(GL43C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL43C.GL_TEXTURE_FETCH_BARRIER_BIT);
		ComputeProgram.unbind();
	}
}
