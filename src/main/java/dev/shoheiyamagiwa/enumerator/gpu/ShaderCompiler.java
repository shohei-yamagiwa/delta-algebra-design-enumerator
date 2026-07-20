package dev.shoheiyamagiwa.enumerator.gpu;

import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;

public final class ShaderCompiler {
    private ShaderCompiler() {
        // No instantiation
    }

    public static byte[] compileComputeShader(String glslSource, String shaderName) {
        long compilerHandle = Shaderc.shaderc_compiler_initialize();
        if (compilerHandle == 0L) {
            throw new IllegalStateException("Failed to initialize the shaderc compiler.");
        }

        try {
            long compilationResult = Shaderc.shaderc_compile_into_spv(compilerHandle, glslSource, Shaderc.shaderc_glsl_compute_shader, shaderName, "main", 0L);
            if (compilationResult == 0L) {
                throw new IllegalStateException("Couldn't get the result of shaderc compiling: " + shaderName);
            }

            try {
                return extractSpirvBytes(compilationResult, shaderName);
            } finally {
                Shaderc.shaderc_result_release(compilationResult);
            }
        } finally {
            Shaderc.shaderc_compiler_release(compilerHandle);
        }
    }

    private static byte[] extractSpirvBytes(long compilationResult, String shaderName) {
        int compilationStatus = Shaderc.shaderc_result_get_compilation_status(compilationResult);

        if (compilationStatus != Shaderc.shaderc_compilation_status_success) {
            String errorMessage = Shaderc.shaderc_result_get_error_message(compilationResult);
            throw new IllegalStateException("Couldn't compile shader (" + shaderName + "): " + errorMessage);
        }

        ByteBuffer spirvBuffer = Shaderc.shaderc_result_get_bytes(compilationResult);

        if (spirvBuffer == null) {
            throw new IllegalStateException("SPIR-V Byte array is empty: " + shaderName);
        }

        byte[] spirvBytes = new byte[spirvBuffer.remaining()];
        spirvBuffer.get(spirvBytes);

        return spirvBytes;
    }
}
