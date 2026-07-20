package dev.shoheiyamagiwa.enumerator.gpu;

import dev.shoheiyamagiwa.enumerator.benchmark.SplitMix64;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class Mix64GpuProbe {
    private static final String SHADER_RESOURCE_PATH = "shaders/mix64.comp";
    private static final String SHADER_NAME = "mix64.comp";

    private static final int ELEMENT_COUNT = 1_000_000;
    private static final long SEED = 12345L;

    private Mix64GpuProbe() {
        // No instantiation
    }

    static void main() {
        String shaderSource = loadShaderResource(SHADER_RESOURCE_PATH);
        byte[] spirvBytes = ShaderCompiler.compileComputeShader(shaderSource, SHADER_NAME);

        System.out.println("Generated SPIR-V (" + spirvBytes.length + " Bytes)。");

        long[] gpuResults = computeOnGpu(spirvBytes, ELEMENT_COUNT, SEED);
        long[] cpuReference = computeCpuReference(ELEMENT_COUNT, SEED);

        int firstMismatchIndex = findFirstMismatch(gpuResults, cpuReference);

        if (firstMismatchIndex < 0) {
            System.out.printf("PROBE OK: All results (%d elements) are matched between GPU and CPU.%n", ELEMENT_COUNT);
        } else {
            long gpuMismatchResult = gpuResults[firstMismatchIndex];
            long cpuRefResult = cpuReference[firstMismatchIndex];

            System.out.printf("PROBE FAILED: index=%d (gpu=%d, cpu=%d)。%n", firstMismatchIndex, gpuMismatchResult, cpuRefResult);
            System.exit(1);
        }
    }

    private static long[] computeOnGpu(byte[] spirvBytes, int elementCount, long seed) {
        long bufferSizeInBytes = (long) elementCount * Long.BYTES;

        try (VulkanContext context = new VulkanContext()) {
            System.out.println("Selected device: " + context.getPhysicalDeviceName());

            try (StorageBuffer outputBuffer = new StorageBuffer(context.getPhysicalDevice(), context.getDevice(), bufferSizeInBytes);
                 Mix64ComputePipeline pipeline = new Mix64ComputePipeline(context.getDevice(), spirvBytes, outputBuffer.getBufferHandle())) {

                return Mix64GpuRunner.run(context, pipeline, outputBuffer, elementCount, seed);
            }
        }
    }

    private static long[] computeCpuReference(int elementCount, long seed) {
        long[] reference = new long[elementCount];

        for (int index = 0; index < elementCount; index++) {
            long mixedIndex = SplitMix64.mix64(index);

            reference[index] = SplitMix64.mix64(seed ^ mixedIndex);
        }

        return reference;
    }

    private static int findFirstMismatch(long[] gpuResults, long[] cpuReference) {
        for (int index = 0; index < cpuReference.length; index++) {
            if (gpuResults[index] != cpuReference[index]) {
                return index;
            }
        }

        return -1;
    }

    private static String loadShaderResource(String resourcePath) {
        ClassLoader classLoader = Mix64GpuProbe.class.getClassLoader();

        try (InputStream resourceStream = classLoader.getResourceAsStream(resourcePath)) {
            if (resourceStream == null) {
                throw new IllegalStateException("Couldn't find the resource: " + resourcePath);
            }

            return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException readFailure) {
            throw new UncheckedIOException("Failed to load the shader: " + resourcePath, readFailure);
        }
    }
}
