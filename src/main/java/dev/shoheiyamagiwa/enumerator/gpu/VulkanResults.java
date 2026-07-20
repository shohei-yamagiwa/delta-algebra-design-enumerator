package dev.shoheiyamagiwa.enumerator.gpu;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public final class VulkanResults {
    private VulkanResults() {
        // No instantiation
    }

    public static void requireSuccess(int resultCode, String operationName) {
        if (resultCode != VK_SUCCESS) {
            throw new IllegalStateException("Failed to " + operationName + " (VkResult=" + resultCode + ").");
        }
    }
}
