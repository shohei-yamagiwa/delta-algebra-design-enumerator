package dev.shoheiyamagiwa.enumerator.gpu;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public final class StorageBuffer implements AutoCloseable {
    private final VkDevice device;
    private final long bufferHandle;
    private final long memoryHandle;
    private final long sizeInBytes;

    public StorageBuffer(VkPhysicalDevice physicalDevice, VkDevice device, long sizeInBytes) {
        this.device = device;
        this.sizeInBytes = sizeInBytes;
        this.bufferHandle = createBuffer(device, sizeInBytes);

        try {
            this.memoryHandle = allocateAndBindMemory(physicalDevice, device, this.bufferHandle);
        } catch (RuntimeException allocationFailure) {
            vkDestroyBuffer(device, this.bufferHandle, null);
            throw allocationFailure;
        }
    }

    private static long createBuffer(VkDevice device, long sizeInBytes) {
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(sizeInBytes)
                    .usage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer bufferPointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(vkCreateBuffer(device, bufferCreateInfo, null, bufferPointer), "vkCreateBuffer");

            return bufferPointer.get(0);
        }
    }

    private static long allocateAndBindMemory(VkPhysicalDevice physicalDevice, VkDevice device, long bufferHandle) {
        try (MemoryStack stack = stackPush()) {
            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, bufferHandle, memoryRequirements);

            int requiredProperties = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
            int memoryTypeIndex = selectMemoryTypeIndex(physicalDevice, memoryRequirements.memoryTypeBits(), requiredProperties);

            VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(memoryTypeIndex);

            LongBuffer memoryPointer = stack.mallocLong(1);
            VulkanResults.requireSuccess(vkAllocateMemory(device, memoryAllocateInfo, null, memoryPointer), "vkAllocateMemory");

            long memoryHandle = memoryPointer.get(0);
            VulkanResults.requireSuccess(vkBindBufferMemory(device, bufferHandle, memoryHandle, 0L), "vkBindBufferMemory");

            return memoryHandle;
        }
    }

    private static int selectMemoryTypeIndex(VkPhysicalDevice physicalDevice, int allowedMemoryTypeBits, int requiredProperties) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);

            for (int memoryTypeIndex = 0; memoryTypeIndex < memoryProperties.memoryTypeCount(); memoryTypeIndex++) {
                boolean isAllowedByBuffer = (allowedMemoryTypeBits & (1 << memoryTypeIndex)) != 0;
                boolean hasRequiredProperties = (memoryProperties.memoryTypes(memoryTypeIndex).propertyFlags() & requiredProperties) == requiredProperties;

                if (isAllowedByBuffer && hasRequiredProperties) {
                    return memoryTypeIndex;
                }
            }

            throw new IllegalStateException("No host-visible and coherent memory type was found.");
        }
    }

    public long[] readLongs(int elementCount) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer mappedPointer = stack.mallocPointer(1);
            VulkanResults.requireSuccess(vkMapMemory(device, memoryHandle, 0L, VK_WHOLE_SIZE, 0, mappedPointer), "vkMapMemory");

            LongBuffer mappedLongs = mappedPointer.getLongBuffer(0, elementCount);
            long[] values = new long[elementCount];
            mappedLongs.get(values);

            vkUnmapMemory(device, memoryHandle);

            return values;
        }
    }

    public long getBufferHandle() {
        return bufferHandle;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public void close() {
        vkFreeMemory(device, memoryHandle, null);
        vkDestroyBuffer(device, bufferHandle, null);
    }
}
